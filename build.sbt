import Dependencies._

name := "logging-testkit"

CommonProject.settings
ScalaDoc.settings

addCompilerPlugin(Dependencies.kindProjection)
enablePlugins(GhpagesPlugin)
enablePlugins(GitBranchPrompt)
enablePlugins(GitVersioning)
enablePlugins(SiteScaladocPlugin)

Publish.settings

crossSbtVersions := Vector("0.13.16", "1.0.3")

ivyLoggingLevel := UpdateLogging.Quiet
git.useGitDescribe := true

libraryDependencies ++= Seq(
  akka.actor,
  akka.contrib,
  akka.slf4j,
  aws.logs,
  circe.core,
  circe.generic,
  circe.parser,
  eff.core,
  eff.monix,
  monix.core,
  monix.reactive,
  pprint
)
