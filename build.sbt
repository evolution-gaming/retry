import Dependencies._

name := "retry"

organization := "com.evolutiongaming"

homepage := Some(url("https://github.com/evolution-gaming/retry"))

startYear := Some(2019)

organizationName := "Evolution"

organizationHomepage := Some(url("https://evolution.com"))

publishTo := Some(Resolver.evolutionReleases)

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.14", "2.12.19", "3.3.4")

libraryDependencies ++= Seq(
  `cats-effect`,
  `cats-helper`,
  random,
  scalatest % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings")

ThisBuild / versionScheme := Some("early-semver")

//addCommandAlias("check", "all versionPolicyCheck Compile/doc")
addCommandAlias("check", "show version")
addCommandAlias("build", "+all compile test")
