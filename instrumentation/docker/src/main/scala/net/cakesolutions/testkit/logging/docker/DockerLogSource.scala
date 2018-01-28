// Copyright 2017-2018 Carl Pulley

package net.cakesolutions.testkit.logging.docker

import scala.concurrent.{blocking, Promise}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.control.NonFatal

import com.typesafe.scalalogging.Logger
import io.circe.Json
import io.circe.parser._
import monix.execution.Scheduler
import monix.reactive.observers.Subscriber

import net.cakesolutions.testkit.logging.{LogEvent, LoggingSource}
import net.cakesolutions.testkit.logging.docker.formats.LogEventFormat

object DockerLogSource extends LoggingSource[Json] {

  private val log = Logger("LoggingTestkit")
  private val dockerLogsCmd = Seq("docker", "logs", "-f", "-t")

  final case class ProcessTerminated(exitCode: Int) extends Exception(s"ProcessTerminated($exitCode)")

  /** @inheritdoc */
  override protected def subscriberPolling(id: String, subscriber: Subscriber[LogEvent[Json]], cancelP: Promise[Unit])(implicit scheduler: Scheduler): Unit = {
    val decoder = new LogEventFormat(id)
    import decoder._

    val handleLogEvent: String => Unit = { event =>
      if (! cancelP.isCompleted) {
        decode[LogEvent[Json]](event) match {
          case Right(value: LogEvent[Json]) =>
            try {
              subscriber.onNext(value)
            } catch {
              case NonFatal(exn) =>
                log.error("Unexpected exception sending data to a subscriber", exn)
            }
          case Left(exn) =>
            subscriber.onError(exn)
        }
      }
    }

    blocking {
      val process = Process(dockerLogsCmd :+ id).run(ProcessLogger(handleLogEvent, handleLogEvent))

      cancelP.future.onComplete(_ => process.destroy())(scheduler)

      // 143 = 128 + SIGTERM
      val exit = process.exitValue()
      if (exit != 0 && exit != 143) {
        throw ProcessTerminated(exit) // FIXME: really?
      }
      if (! cancelP.isCompleted) {
        cancelP.success(())
        subscriber.onComplete()
      }
    }
  }
}
