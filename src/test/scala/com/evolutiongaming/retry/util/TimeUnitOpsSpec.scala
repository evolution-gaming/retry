package com.evolutiongaming.retry.util

import com.evolutiongaming.retry.util.TimeUnitHelper._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TimeUnitOpsSpec extends AnyFunSuite with Matchers {

  private val timeUnits = TimeUnitObj
    .values
    .zipWithIndex
    .map { _.swap }
    .toMap

  for {
    (idx, timeUnit) <- timeUnits
  } yield {
    test(s"$timeUnit.increment") {
      val expected = timeUnits.get(idx + 1)
      timeUnit.increment shouldEqual expected
    }

    test(s"$timeUnit.decrement") {
      val expected = timeUnits.get(idx - 1)
      timeUnit.decrement shouldEqual expected
    }
  }
}
