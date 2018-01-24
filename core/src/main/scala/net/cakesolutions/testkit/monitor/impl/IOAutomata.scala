package net.cakesolutions.testkit.monitor.impl

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal

import akka.actor.{Actor, OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.Decider
import akka.event.Logging
import akka.pattern.ask
import akka.receive.pattern.ReceivePipeline
import akka.util.Timeout
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.{Observable, Observer}
import monix.reactive.observers.Subscriber
import org.slf4j.Logger
import net.cakesolutions.testkit.monitor._
import net.cakesolutions.utils.ValueDiscard

private[monitor] class IOAutomata[IOState: ClassTag, Event](
  initial: IOState,
  timeout: Option[FiniteDuration],
  transition: Behaviour[IOState, Event],
  sensor: Observable[Event]
)(implicit scheduler: Scheduler,
  log: Logger
) extends Actor
  with ReceivePipeline
  with SubscriberHandling
  with EventLogger[IOState, Event] {

  import IOAutomata._

  override val supervisorStrategy: SupervisorStrategy = {
    def stoppingDecider: Decider = {
      case NonFatal(exn) =>
        stop(Some(exn))
        SupervisorStrategy.Stop
    }
    OneForOneStrategy()(stoppingDecider)
  }

  private[this] var state: State[IOState] = State(initial, timeout, callback(timeout))

  override protected def getState = state

  private val fsmObs: Observer[Event] = new Observer[Event] {
    override def onNext(elem: Event): Future[Ack] = {
      log.debug(s"FSM Input Observer: onNext($elem)")
      // FIXME: set via configuration
      self.ask(Observe(elem))(Timeout(10.seconds)).mapTo[Ack]
    }

    override def onError(exn: Throwable): Unit = {
      log.debug(s"FSM Input Observer: onError($exn)")
      self ! exn
    }

    override def onComplete(): Unit = {
      log.debug("FSM Input Observer: onComplete()")
      if (state.timeout.isDefined) {
        self ! StateTimeout
      }
      self ! Shutdown
    }
  }
  private val outputSubscription: Cancelable = sensor.subscribe(fsmObs)

  private val behaviour: IOState => ObservedEvent[Event] => Action[IOState] = { state => event =>
    if (transition.isDefinedAt(state) && transition(state).isDefinedAt(event)) {
      log.info(s"@ $state matched: $event")
      transition(state)(event)
    } else {
      event match {
        case StateTimeout =>
          Stop(Fail(s"FSM matching failed in state $state"))
        case _: ObservedEvent[Event] =>
          log.debug(s"ignoring $event in state $state")
          Stay()
      }
    }
  }

  /**
    * TODO:
    *
    * @return
    */
  def receive: Receive = {
    case event: ObservedEvent[Event] =>
      try {
        val action = behaviour(state.state)(event)
        val timeout = action match {
          case Goto(_, forMax, _) =>
            forMax
          case _ =>
            None
        }
        action.emit
          .foreach(msg => getSubscribers.foreach(_.onNext(msg)))
        state = nextState(action, timeout)
        action match {
          case Stop(exn: Fail) =>
            stop(Some(exn))
          case Stop(_: Accept) =>
            log.debug(s"FSM stopping in state $state")
            stop()
          case _ =>
          // No work to do
        }
      } catch {
        case NonFatal(exn) =>
          sender() ! Ack.Stop
          stop(Some(UnexpectedException(exn, event)))
      }
    case Shutdown =>
      stop(Some(Shutdown))
    case exn: Throwable =>
      stop(Some(exn))
  }

  private def callback(timeout: Option[FiniteDuration]): Option[Cancelable] = {
    timeout.map { delay =>
      scheduler.scheduleOnce(delay) {
        self ! StateTimeout
        state.callback.foreach(_.cancel())
      }
    }
  }

  private def nextState(action: Action[IOState], timeout: Option[FiniteDuration]): State[IOState] = {
    action match {
      case Goto(next: IOState, forMax, _) =>
        // FIXME: take into account Goto that is equal to a Stay???
        sender() ! Ack.Continue
        state.callback.foreach(_.cancel())
        State(next, forMax, callback(forMax))
      case Stop(_) =>
        sender() ! Ack.Stop
        state
      case Stay(_) =>
        sender() ! Ack.Continue
        state
    }
  }

  private def stop(reason: Option[Throwable] = None): Unit = {
    state.callback.foreach(_.cancel())
    outputSubscription.cancel()
    val errMsg = reason.map {
      case StateTimeout =>
        s"FSM timed out in state $state"
      case Shutdown =>
        s"FSM upstreams closed in state $state"
      case UnexpectedException(_, event) =>
        s"FSM matching failed in state $state whilst handling $event"
      case Fail(causes) =>
        s"FSM matching failed in state $state due to $causes"
      case _ =>
        s"FSM matching failed in state $state"
    }
    reason
      .map {
        case UnexpectedException(exn, _) =>
          exn
        case exn =>
          exn
      }
      .foreach { exn =>
        val exnMsg = if (getLoggedTrace.length == traceSize) {
          s"${errMsg.get} - last $traceSize events (oldest first): ${pprint.tokenize(getLoggedTrace).mkString("")}\n${Logging.stackTraceFor(exn)}"
        } else {
          s"${errMsg.get} - events (oldest first): ${pprint.tokenize(getLoggedTrace).mkString("")}\n${Logging.stackTraceFor(exn)}"
        }
        getSubscribers.foreach(_.onNext(Fail(exnMsg)))
        log.debug(exnMsg)
      }
    // FIXME: surely we base ending on the reason???
    getSubscribers.foreach(_.onComplete())
    ValueDiscard[Try[Unit]] {
      Try(context.stop(self))
    }
  }
}

private[monitor] object IOAutomata {
  final case class Subscribe(sub: Subscriber[Notify])
  final case class Unsubscribe(sub: Subscriber[Notify])

  case object Shutdown extends Exception
  final case class UnexpectedException[Event](reason: Throwable, event: ObservedEvent[Event]) extends Exception
  final case class State[IOState](state: IOState, timeout: Option[FiniteDuration], callback: Option[Cancelable])
}
