// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.monitor

/**
  * Observed events that monitors will observe and react to.
  *
  * @tparam Event type of event that we are to observe
  */
sealed trait ObservedEvent[+Event]

/**
  * We timed out whilst waiting for an event to flow in some state.
  */
case object StateTimeout extends Throwable with ObservedEvent[Nothing] {
  override def toString: String = {
    "StateTimeout"
  }
}

/**
  * Observed an event flow.
  *
  * @param event event that we observed
  * @tparam Event type of event that we are to observe
  */
final case class Observe[Event](event: Event) extends ObservedEvent[Event]
