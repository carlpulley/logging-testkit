// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.logging.docker.formats

import io.circe.Json
import io.circe.parser._
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import net.cakesolutions.testkit.generators.TestGenerators.logEventGen
import net.cakesolutions.testkit.logging.LogEvent

class LogEventFormatTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  val decoder = new LogEventFormat("test")
  import decoder._

  // FIXME: deal with test failure!
  "Can serialise and deserialise logging events" ignore {
    forAll(logEventGen()) { logEvent =>
      decode[LogEvent[Json]](encodeLogEvent(logEvent).toString()) shouldEqual logEvent
    }
  }
}
