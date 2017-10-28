// Copyright 2016 Carl Pulley

package net.cakesolutions.testkit.monitor

import scala.concurrent.duration.FiniteDuration

/**
  * TODO:
  *
  * @tparam IOState
  */
sealed trait Action[IOState] {
  def emit: Option[Notify]
}

/**
  * TODO:
  *
  * @param state
  * @param forMax
  * @param emit
  * @tparam IOState
  */
final case class Goto[IOState](state: IOState, forMax: Option[FiniteDuration] = None, emit: Option[Notify] = None) extends Action[IOState]
object Goto {
  def apply[IOState](state: IOState, forMax: FiniteDuration): Goto[IOState] = {
    Goto(state, Some(forMax), None)
  }

  def apply[IOState](state: IOState, emit: Notify): Goto[IOState] = {
    Goto(state, None, Some(emit))
  }

  def apply[IOState](state: IOState, forMax: FiniteDuration, emit: Notify): Goto[IOState] = {
    Goto(state, Some(forMax), Some(emit))
  }
}

/**
  * TODO:
  *
  * @param emit
  * @tparam IOState
  */
final case class Stay[IOState](emit: Option[Notify] = None) extends Action[IOState]
object Stay {
  def apply[IOState](emit: Notify): Stay[IOState] = {
    Stay(Some(emit))
  }
}

/**
  * TODO:
  *
  * @param toEmit
  * @tparam IOState
  */
final case class Stop[IOState](toEmit: Notify) extends Action[IOState] {
  override val emit = Some(toEmit)
}
