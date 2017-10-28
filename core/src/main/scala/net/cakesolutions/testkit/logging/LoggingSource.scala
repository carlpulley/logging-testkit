// Copyright 2017-2018 Carl Pulley

package net.cakesolutions.testkit.logging

import monix.execution.Scheduler
import monix.reactive.Observable

/**
  * TODO:
  */
trait LoggingSource[A] {
  /**
    * TODO:
    *
    * @param id
    * @param scheduler
    * @return
    */
  def source(id: String)(implicit scheduler: Scheduler): Observable[LogEvent[A]]
}
