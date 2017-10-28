// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.monitor

sealed trait ObservedEvent[+Event]
case object StateTimeout extends Throwable with ObservedEvent[Nothing]
final case class Observe[Event](event: Event) extends ObservedEvent[Event]
