// Copyright 2017 Carl Pulley

package net.cakesolutions.testkit.docker

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
