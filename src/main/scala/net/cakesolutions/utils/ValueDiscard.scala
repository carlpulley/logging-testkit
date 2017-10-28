// Copyright 2016 Carl Pulley

package net.cakesolutions.utils

object ValueDiscard {
  def apply[T](value: => T): Unit = {
    val _ = value
  }
}
