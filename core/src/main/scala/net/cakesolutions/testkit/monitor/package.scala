// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit

package object monitor {
  type Behaviour[IOState, Event] = PartialFunction[IOState, PartialFunction[ObservedEvent[Event], Action[IOState]]]
}
