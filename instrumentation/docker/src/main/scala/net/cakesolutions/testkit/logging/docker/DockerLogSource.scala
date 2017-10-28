// Copyright 2017-2018 Carl Pulley

package net.cakesolutions.testkit.logging.docker

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import scala.concurrent.{Promise, blocking}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

import akka.event.slf4j.Logger
import io.circe.{DecodingFailure, Json}
import io.circe.parser._
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}

import net.cakesolutions.testkit.logging.{LogEvent, LoggingSource}

object DockerLogSource extends LoggingSource[Json] {
  private val dockerLogsCmd = Seq("docker", "logs", "-f", "-t")
  private val log = Logger("ApplicationLog")

  def source(id: String)(implicit scheduler: Scheduler): Observable[LogEvent[Json]] =
    Observable.create[LogEvent[Json]](OverflowStrategy.Unbounded) { subscriber =>
      val cancelP = Promise[Unit]

      try {
        scheduler.execute(new Runnable {
          def run(): Unit = {
            val handleLogEvent: String => Unit = { event =>
              if (! cancelP.isCompleted) {
                event.toLogEvent(id) match {
                  case Success(value: LogEvent[Json]) =>
                    try {
                      subscriber.onNext(value)
                    } catch {
                      case exn: Throwable =>
                        exn.printStackTrace()
                    }
                  case Failure(exn) =>
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
                throw new RuntimeException(s"Logging exited with value $exit")
              }
              if (! cancelP.isCompleted) {
                cancelP.success(())
                subscriber.onComplete()
              }
            }
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

  private implicit class LogEventHelper(rawLine: String) {
    def toLogEvent(id: String): Try[LogEvent[Json]] = Try {
      val line = rawLine.trim
      log.debug(s"$id $line")

      if (line.nonEmpty) {
        // 2016-06-11T10:10:00.154101534Z log-message
        val logLineRE = "^\\s*(\\d+\\-\\d+\\-\\d+T\\d+:\\d+:\\d+\\.\\d+Z)\\s+(.*)\\s*\\z".r
        val logLineMatch = logLineRE.findFirstMatchIn(line)

        if (logLineMatch.isDefined) {
          val time = logLineMatch.get.group(1)
          val message = logLineMatch.get.group(2).trim
          for {
            json <- parse(message).toTry
          } yield LogEvent[Json](ZonedDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX")), id, json)
        } else {
          for {
            json <- parse(line).toTry
          } yield LogEvent[Json](ZonedDateTime.now(ZoneOffset.UTC), id, json)
        }
      } else {
        Failure[LogEvent[Json]](DecodingFailure("", List()))
      }
    }.flatten
  }
}
