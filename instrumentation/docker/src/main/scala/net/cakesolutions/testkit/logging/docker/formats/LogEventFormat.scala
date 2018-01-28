// Copyright 2017-2018 Carl Pulley

package net.cakesolutions.testkit.logging.docker.formats

import java.time.{ZonedDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import cats.syntax.either._
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.parser.parse

import net.cakesolutions.testkit.logging.LogEvent

/**
  * TODO:
  *
  * @param id
  */
class LogEventFormat(id: String) {
  private val log = Logger("LoggingTestkit")
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX")

  implicit val encodeLogEvent: Encoder[LogEvent[Json]] = Encoder.encodeString.contramap[LogEvent[Json]] { logEvent =>
    s"${logEvent.time.format(formatter)} ${logEvent.message}"
  }

  implicit val decodeLogEvent: Decoder[LogEvent[Json]] = Decoder.decodeString.emap { rawLine =>
    Either.catchNonFatal {
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
            json <- parse(message)
          } yield LogEvent[Json](ZonedDateTime.parse(time, formatter), id, json)
        } else {
          for {
            json <- parse(line)
          } yield LogEvent[Json](ZonedDateTime.now(ZoneOffset.UTC), id, json)
        }
      } else {
        Left(DecodingFailure("", List()))
      }
    }.joinRight.leftMap(_.toString)
  }
}
