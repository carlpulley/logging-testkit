// Copyright 2016-2018 Carl Pulley

import sbt.Keys._
import sbt._

object Dependencies {
  object akka {
    private val version = "2.5.9"

    val actor: ModuleID = "com.typesafe.akka" %% "akka-actor" % version
    val contrib: ModuleID = "com.typesafe.akka" %% "akka-contrib" % version

    object http {
      private val httpVersion = "10.0.9"

      val core: ModuleID = "com.typesafe.akka" %% "akka-http-core" % httpVersion
      val experimental: ModuleID = "com.typesafe.akka" %% "akka-http" % httpVersion
      val testkit: ModuleID = "com.typesafe.akka" %% "akka-http-testkit" % httpVersion
    }

    val slf4j: ModuleID = "com.typesafe.akka" %% "akka-slf4j" % version
  }

  object aws {
    private val version = "1.11.265"

    val logs: ModuleID = "com.amazonaws" % "aws-java-sdk-logs" % version
  }

  object circe {
    private val version = "0.9.0"

    val core: ModuleID = "io.circe" %% "circe-core" % version
    val generic: ModuleID = "io.circe" %% "circe-generic" % version
    val optics: ModuleID = "io.circe" %% "circe-optics" % version
    val parser: ModuleID = "io.circe" %% "circe-parser" % version
  }

  val config: ModuleID = "com.typesafe" % "config" % "1.3.2"
  val logback: ModuleID = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val logging: ModuleID = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

  object monix {
    private val version = "3.0.0-668a8b9"

    val core: ModuleID = "io.monix" %% "monix" % version
    val reactive: ModuleID = "io.monix" %% "monix-reactive" % version
  }

  val pprint: ModuleID = "com.lihaoyi" %% "pprint" % "0.5.3"
  val scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % "1.13.5"
  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.4"
}
