import sbt._

object Dependencies {
  object dev {
    object zio {
      val zio =
        "dev.zio" %% "zio" % "1.0.18"
    }
  }

  object org {
    object scalatest {
      val scalatest =
        "org.scalatest" %% "scalatest" % "3.2.15"
    }

    object scalatestplus {
      val `scalacheck-1-17` =
        "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0"
    }
  }
}
