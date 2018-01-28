// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.generators

import java.time.{Month, Year, ZonedDateTime, ZoneId}

import scala.collection.JavaConverters._

import org.scalacheck.Gen

object ZonedDateTimeGenerator {

  def zonedDateTimeGen: Gen[ZonedDateTime] =
    for {
      year  <- Gen.choose(-292278994, 292278994)
      month <- Gen.choose(1, 12)
      maxDaysInMonth = Month.of(month).length(Year.of(year).isLeap)
      dayOfMonth   <- Gen.choose(1, maxDaysInMonth)
      hour         <- Gen.choose(0, 23)
      minute       <- Gen.choose(0, 59)
      second       <- Gen.choose(0, 59)
      nanoOfSecond <- Gen.choose(0, 999999999)
      zoneId       <- Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toList)
    } yield ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, ZoneId.of(zoneId))

}
