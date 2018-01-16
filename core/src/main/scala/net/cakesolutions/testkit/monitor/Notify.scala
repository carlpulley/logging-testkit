// Copyright 2016-2018 Carl Pulley

package net.cakesolutions.testkit.monitor

/**
  * FIXME:
  *
  * Notifications are grouped by their event ID.
  *
  * Transactional notification groups contain at least one `Start` notification
  * and at least one `Accept` or `Fail` notification. Transactional
  * notifications are:
  * - accepting if the group contains `Accept` and no `Fail`
  * - failing if the group contains `Fail`
  *
  * The finite duration between the earliest `Start` and last, `Accept` or
  * `Fail` notification is known as the transactions duration or length.
  */
sealed trait Notify {
  def invert: Notify
}

/**
  * TODO:
  *
  * @param failures
  */
final case class Accept(failures: String*) extends Notify {
  override def invert: Notify = {
    Fail(failures: _*)
  }
}

/**
  * TODO:
  *
  * @param reasons
  */
final case class Fail(reasons: String*) extends Exception with Notify {
  override def invert: Notify = {
    Accept(reasons: _*)
  }

  override def toString: String = {
    s"Fail(${reasons.mkString(", ")})"
  }
}
