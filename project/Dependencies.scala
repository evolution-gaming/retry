import sbt._

object Dependencies {

  val scalatest     = "org.scalatest"       %% "scalatest"   % "3.1.2"
  val `cats-helper` = "com.evolutiongaming" %% "cats-helper" % "2.0.2"
  val random        = "com.evolutiongaming" %% "random"      % "0.0.7"

  object Cats {
    private val version = "2.1.1"
    val core   = "org.typelevel" %% "cats-core"   % version
    val kernel = "org.typelevel" %% "cats-kernel" % version
    val macros = "org.typelevel" %% "cats-macros" % version
    val effect = "org.typelevel" %% "cats-effect" % "2.1.2"
  }
}