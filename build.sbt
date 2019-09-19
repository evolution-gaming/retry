import Dependencies._

name := "retry"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/retry"))

startYear := Some(2019)

organizationName := "Evolution Gaming"

organizationHomepage := Some(url("http://evolutiongaming.com"))

bintrayOrganization := Some("evolutiongaming")

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.1", "2.12.10")

resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies ++= Seq(
  Cats.core,
  Cats.kernel,
  Cats.macros,
  Cats.effect,
  `cats-helper`,
  random,
  scalatest % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits", "-no-link-warnings")