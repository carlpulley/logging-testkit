package net.cakesolutions.testkit.monitor

import scala.concurrent.duration.Deadline

import monix.reactive.{Notification, Observable}

import net.cakesolutions.testkit.monitor.Interactions.ActionOut

private[monitor] sealed trait MonitorState[IOState]

private[monitor] final case class RunningState[IOState](
  state: IOState,
  stateTimeout: Option[Deadline] = None,
  overallTimeout: Option[Deadline] = None,
  actions: Observable[Notification[ActionOut[Notify]]] = Observable.empty
) extends MonitorState[IOState]

private[monitor] final case class ShutdownState[IOState](
  state: IOState,
  stateTimeout: Option[Deadline] = None,
  overallTimeout: Option[Deadline] = None,
  actions: Observable[Notification[ActionOut[Notify]]] = Observable.empty
) extends MonitorState[IOState]
