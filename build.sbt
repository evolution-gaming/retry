import Dependencies.*
import sbtversionpolicy.Compatibility.BinaryCompatible

name := "retry"

organization := "com.evolutiongaming"

homepage := Some(uri("https://github.com/evolution-gaming/retry"))

startYear := Some(2019)

organizationName := "Evolution"

organizationHomepage := Some(uri("https://evolution.com"))

publishTo := Some(Resolver.evolutionReleases)

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.14", "2.12.19", "3.3.3")

libraryDependencies ++= Seq(
  `cats-effect`,
  `cats-helper`,
  random,
  scalatest % Test,
)

licenses := Seq(("MIT", uri("https://opensource.org/licenses/MIT")))

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings")

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / versionPolicyIntention := BinaryCompatible

addCommandAlias("check", "show versionPolicyCheck scalafmtCheckRepo Compile/doc")
addCommandAlias("fmt", "scalafmtRepo")
addCommandAlias("build", "+all compile testFull")
