// Copyright 2018 Carl Pulley

package net.cakesolutions.testkit.generators

import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}

object JsonGenerators {
  val nullGen: Gen[Json] = Json.Null

  val boolGen: Gen[Json] = for {
    value <- Arbitrary.arbitrary[Boolean]
  } yield Json.fromBoolean(value)

  val doubleGen: Gen[Json] = for {
    value <- Arbitrary.arbitrary[Double] suchThat(value => (!java.lang.Double.isNaN(value)) && (!java.lang.Double.isInfinite(value)))
  } yield Json.fromDouble(value).get

  val intGen: Gen[Json] = for {
    value <- Arbitrary.arbitrary[Int]
  } yield Json.fromInt(value)

  val stringGen: Gen[Json] = for {
    value <- Gen.frequency(
      1 -> Arbitrary.arbitrary[String],
      5 -> Gen.alphaStr
    )
  } yield Json.fromString(value)

  val arrayGen: Gen[Json] = for {
    length <- Gen.choose(1, 10)
    values <- Gen.listOfN[Json](length, jsonGen)
  } yield Json.fromValues(values)

  val fieldGen: Gen[(String, Json)] = for {
    label <- Gen.frequency(
      1 -> Arbitrary.arbitrary[String],
      10 -> Gen.identifier
    )
    json <- jsonGen
  } yield (label, json)

  val objectGen: Gen[Json] = for {
    length <- Gen.frequency(
      5 -> Gen.choose(1, 20),
      2 -> Gen.choose(21, 50),
      1 -> Gen.choose(51, 200)
    )
    fields <- Gen.listOfN[(String, Json)](length, fieldGen)
  } yield Json.fromFields(fields)

  def jsonGen: Gen[Json] = Gen.lzy(
    Gen.frequency(
      1 -> nullGen,
      100 -> doubleGen,
      50 -> stringGen,
      100 -> intGen,
      10 -> arrayGen,
      5 -> objectGen
    )
  )
}
