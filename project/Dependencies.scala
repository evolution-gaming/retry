import sbt._

object Dependencies {

  val scalatest     = "org.scalatest"       %% "scalatest"   % "3.1.1"
  val `cats-helper` = "com.evolutiongaming" %% "cats-helper" % "1.7.0"
  val random        = "com.evolutiongaming" %% "random"      % "0.0.6"

  object Cats {
    private val version = "2.0.0"
    val core   = "org.typelevel" %% "cats-core"   % version
    val kernel = "org.typelevel" %% "cats-kernel" % version
    val macros = "org.typelevel" %% "cats-macros" % version
    val effect = "org.typelevel" %% "cats-effect" % "2.0.0"
  }
}