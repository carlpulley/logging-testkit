// Copyright 2018 Carl Pulley

import Dependencies._

//enablePlugins(GhpagesPlugin)
enablePlugins(GitBranchPrompt)
enablePlugins(GitVersioning)
enablePlugins(SiteScaladocPlugin)

Publish.settings

crossSbtVersions := Vector("0.13.16", "1.1.0")

git.useGitDescribe := true
ivyLoggingLevel := UpdateLogging.Quiet

lazy val core = project.in(file("core"))
  .settings(CommonProject.settings)
  .settings(ScalaDoc.settings)
  .settings(
    name := "logging-testkit",
    libraryDependencies ++= Seq(
      akka.actor,
      akka.contrib,
      akka.slf4j,
      circe.core,
      circe.generic,
      circe.parser,
      logback,
      monix.core,
      monix.reactive,
      pprint,
      scalatest % Test,
      scalacheck % Test
    )
)

lazy val docker = project.in(file("instrumentation/docker"))
  .dependsOn(core)
  .settings(
    name := "logging-testkit-docker"
  )

lazy val elasticsearch = project.in(file("instrumentation/elasticsearch"))
  .dependsOn(core)
  .settings(
    name := "logging-testkit-elasticsearch",
    libraryDependencies += aws.logs
  )
