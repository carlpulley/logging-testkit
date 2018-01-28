// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.monitor

object Interactions {

  /**
    * Observed events that monitors will observe and react to.
    *
    * @tparam Event type of event that we are to observe
    */
  sealed trait EventIn[+Event]

  /**
    * TODO:
    *
    * @tparam Event
    */
  sealed trait EventInternal[+Event]

  /**
    * TODO:
    *
    * @tparam Event
    */
  sealed trait ActionOut[+Event]

  /**
    * Observed an event flow.
    *
    * @param event event that we observed
    * @tparam Event type of event that we are to observe
    */
  final case class Observe[Event](event: Event) extends EventIn[Event] with EventInternal[Event] with ActionOut[Event]

  /**
    * TODO:
    */
  private[monitor] case object Tick extends EventInternal[Nothing]

  /**
    * We timed out whilst waiting for an event to flow in some state.
    */
  case object StateTimeout extends Exception("StateTimeout") with EventIn[Nothing] with EventInternal[Nothing] with ActionOut[Nothing]

  /**
    * TODO:
    */
  case object MonitorTimeout extends Exception("MonitorTimeout") with EventIn[Nothing] with EventInternal[Nothing] with ActionOut[Nothing]

  /**
    * TODO:
    *
    * @param event
    * @tparam Event type of event that we are to observe
    */
  final case class TransitionFailure[Event](event: Event) extends Exception(s"TransitionFailure($event)") with ActionOut[Nothing]

  /**
    * TODO:
    *
    * @param exn
    */
  final case class UnexpectedException(exn: Throwable) extends Exception(s"UnexpectedException($exn)") with ActionOut[Nothing]
}
