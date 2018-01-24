// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.monitor

import scala.concurrent.duration._
import scala.reflect.ClassTag

import akka.actor._
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.observers.Subscriber
import org.slf4j.Logger

import net.cakesolutions.testkit.monitor.impl._

/**
  * TODO:
  *
  * @param initial
  * @param timeout
  * @param behaviour
  * @tparam IOState
  * @tparam Event
  */
sealed abstract case class Monitor[IOState: ClassTag, Event] private(
  initial: IOState,
  timeout: Option[FiniteDuration],
  behaviour: Behaviour[IOState, Event]
) {

  import IOAutomata._

  /**
    * TODO:
    *
    * @param sensor
    * @param system
    * @param scheduler
    * @param log
    * @return
    */
  def run(
    sensor: Observable[Event]
  )(implicit system: ActorSystem,
    scheduler: Scheduler,
    log: Logger
  ): Observable[Notify] = {
    Observable.create(OverflowStrategy.Unbounded) { (sub: Subscriber[Notify]) =>
      val checker = system.actorOf(Props(new IOAutomata(initial, timeout, behaviour, sensor)))

      checker ! Subscribe(sub)

      new Cancelable {
        def cancel(): Unit = {
          checker ! Unsubscribe(sub)
        }
      }
    }
  }

}

object Monitor {
  /**
    * TODO:
    *
    * @param initial monitor's starting state
    * @param behaviour monitor's behaviour
    * @tparam IOState state type
    * @tparam Event event type
    * @return monitor implementing the given behaviour
    */
  def apply[IOState: ClassTag, Event](
    initial: IOState
  )(
    behaviour: Behaviour[IOState, Event]
  ): Monitor[IOState, Event] = {
    new Monitor(initial, None, behaviour) {}
  }

  /**
    * TODO:
    *
    * @param initial monitor's starting state
    * @param timeout how long we will stay in the initial state
    * @param behaviour monitor's behaviour
    * @tparam IOState state type
    * @tparam Event event type
    * @return monitor implementing the given behaviour
    */
  def apply[IOState: ClassTag, Event](
    initial: IOState,
    timeout: FiniteDuration
  )(
    behaviour: Behaviour[IOState, Event]
  ): Monitor[IOState, Event] = {
    new Monitor(initial, Some(timeout), behaviour) {}
  }
}
