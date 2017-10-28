// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.logging

import io.circe.Json
import monix.execution.Scheduler
import monix.reactive.Observable

class TestLogSource(logEvents: LogEvent[Json]*) extends LoggingSource[Json] {

  def source(id: String)(implicit scheduler: Scheduler): Observable[LogEvent[Json]] = {
    Observable(logEvents: _*)
  }
}
