import Dependencies._

name := "retry"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/retry"))

startYear := Some(2019)

organizationName := "Evolution"

organizationHomepage := Some(url("http://evolution.com"))

publishTo := Some(Resolver.evolutionReleases)

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.6", "2.12.14")

libraryDependencies ++= Seq(
  `cats-effect`,
  `cats-helper`,
  random,
  scalatest % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings")

ThisBuild / versionScheme := Some("early-semver")