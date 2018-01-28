// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.logging

import scala.concurrent.duration._

import io.circe.Json
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck.Gen._

import net.cakesolutions.testkit.generators.TestGenerators
import net.cakesolutions.testkit.matchers.ObservableMatcher._

class LoggingSourceTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  import TestGenerators._

  implicit val timeout: FiniteDuration = 3.seconds

  "LoggingSource should see all logged events" in {
    forAll(listOf(logEventGen())) { logEvents =>
      val logSource = new TestLogSource(Observable(logEvents: _*))

      logSource.source("test") should observe[LogEvent[Json]](logEvents: _*)
    }
  }
}
