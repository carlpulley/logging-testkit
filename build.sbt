// Copyright 2018 Carl Pulley

import Dependencies._

//enablePlugins(GhpagesPlugin)
enablePlugins(GitBranchPrompt)
enablePlugins(GitVersioning)
enablePlugins(SiteScaladocPlugin)

Publish.settings

git.useGitDescribe := true
ivyLoggingLevel := UpdateLogging.Quiet

lazy val core = project.in(file("core"))
  .settings(CommonProject.settings)
  .settings(ScalaDoc.settings)
  .settings(
    name := "logging-testkit",
    libraryDependencies ++= Seq(
      circe.core,
      circe.generic,
      circe.parser,
      logback,
      logging,
      monix.core,
      monix.reactive,
      scalatest % Test,
      scalacheck % Test
    ),
    coverageMinimum := 60
  )

lazy val docker = project.in(file("instrumentation/docker"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(
    name := "logging-testkit-docker",
    coverageMinimum := 0
  )

lazy val elasticsearch = project.in(file("instrumentation/elasticsearch"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(
    name := "logging-testkit-elasticsearch",
    libraryDependencies += aws.logs,
    coverageMinimum := 0
  )
