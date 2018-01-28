// Copyright 2017-2018 Carl Pulley

package net.cakesolutions.testkit.logging

import scala.concurrent.Promise
import scala.util.control.NonFatal

import com.typesafe.scalalogging.Logger
import io.circe.Json
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.observers.Subscriber

/**
  * TODO:
  */
trait LoggingSource[A] {

  private val log = Logger("LoggingTestkit")

  /**
    * TODO:
    *
    * @param id
    * @param subscriber
    * @param cancelP
    * @param scheduler
    */
  protected def subscriberPolling(id: String, subscriber: Subscriber[LogEvent[Json]], cancelP: Promise[Unit])(implicit scheduler: Scheduler): Unit

  /**
    * TODO:
    *
    * @param id
    * @param scheduler
    * @return
    */
  final def source(id: String)(implicit scheduler: Scheduler): Observable[LogEvent[Json]] =
    Observable.create[LogEvent[Json]](OverflowStrategy.Unbounded) { subscriber =>
      val cancelP = Promise[Unit]

      try {
        scheduler.execute(new Runnable {
          def run(): Unit = {
            subscriberPolling(id, subscriber, cancelP)
          }
        })
      } catch {
        case NonFatal(exn) =>
          log.error("Log parsing error", exn)
          if (! cancelP.isCompleted) {
            cancelP.failure(exn)
            subscriber.onError(exn)
          }
      }

      new Cancelable {
        override def cancel(): Unit = {
          if (! cancelP.isCompleted) {
            cancelP.success(())
            subscriber.onComplete()
          }
        }
      }
    }
}
