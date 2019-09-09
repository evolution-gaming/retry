import sbt._

object Dependencies {

  val scalatest     = "org.scalatest"       %% "scalatest"   % "3.0.8"
  val `cats-helper` = "com.evolutiongaming" %% "cats-helper" % "0.0.29"
  val random        = "com.evolutiongaming" %% "random"      % "0.0.4"

  object Cats {
    private val version = "1.6.1"
    val core   = "org.typelevel" %% "cats-core"   % version
    val kernel = "org.typelevel" %% "cats-kernel" % version
    val macros = "org.typelevel" %% "cats-macros" % version
    val effect = "org.typelevel" %% "cats-effect" % "1.4.0"
  }
}