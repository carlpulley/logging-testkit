// Copyright 2017 Carl Pulley

package net.cakesolutions.testkit.docker.ecs

import java.time.{Instant, ZoneOffset, ZonedDateTime}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal

import akka.event.slf4j.Logger
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder
import io.circe.Json
import io.circe.parser.parse
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}

import net.cakesolutions.testkit.docker.{LogEvent, LoggingSource}

object ApplicationLog extends LoggingSource[Json] {
  private val log = Logger("ApplicationLog")

  def source(id: String)(implicit scheduler: Scheduler): Observable[LogEvent[Json]] =
    Observable.create[LogEvent[Json]](OverflowStrategy.Unbounded) { subscriber =>
      try {
        scheduler.execute(new Runnable {
          def run(): Unit = {
            val awsLogClient = AWSLogsAsyncClientBuilder.defaultClient()

            Observable
              .fromFuture(Future {
                awsLogClient.getLogEventsAsync(???).get()
              })
              .flatMap { log =>
                val events = log.getEvents.asScala.map(e => LogEvent(ZonedDateTime.ofInstant(Instant.ofEpochSecond(e.getTimestamp), ZoneOffset.UTC), id, parse(e.getMessage)))

                Observable(events: _*)
              }
          }
        })
      } catch {
        case NonFatal(exn) =>
          log.error("Log parsing error", exn)
          subscriber.onError(exn)
      }

      new Cancelable {
        override def cancel(): Unit = {
          subscriber.onComplete()
        }
      }
    }
}
