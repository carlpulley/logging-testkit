// Copyright 2016 Carl Pulley

package net.cakesolutions.testkit.monitor

/**
  * TODO:
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
}
