package com.evolutiongaming.retry

import cats._
import cats.arrow.FunctionK
import cats.implicits._
import com.evolutiongaming.random.Random
import com.evolutiongaming.retry.Retry._
import com.evolutiongaming.retry.Retry.implicits._
import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.annotation.tailrec
import scala.concurrent.duration._

class RetrySpec extends AnyFunSuite with Matchers {
  import RetrySpec.StateT._
  import RetrySpec._

  test("fibonacci") {
    val strategy = Strategy
      .fibonacci(5.millis)
      .cap(200.millis)

    val call = StateT { _.call }
    val result = Retry(strategy, onError).mapK(FunctionK.id, FunctionK.id).apply(call)

    val initial = State(toRetry = 10)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 200.millis, retries = 9),
        Record.retry(delay = 170.millis, retries = 8),
        Record.retry(delay = 105.millis, retries = 7),
        Record.retry(delay = 65.millis, retries = 6),
        Record.retry(delay = 40.millis, retries = 5),
        Record.retry(delay = 25.millis, retries = 4),
        Record.retry(delay = 15.millis, retries = 3),
        Record.retry(delay = 10.millis, retries = 2),
        Record.retry(delay = 5.millis, retries = 1),
        Record.retry(delay = 5.millis, retries = 0)),
      delays = List(
        200.millis,
        170.millis,
        105.millis,
        65.millis,
        40.millis,
        25.millis,
        15.millis,
        10.millis,
        5.millis,
        5.millis))
    actual shouldEqual expected
  }

  test("attempts") {
    val strategy = Strategy
      .const(10.millis)
      .attempts(3)

    val call = StateT { _.call }
    val result = Retry(strategy, onError).mapK(FunctionK.id, FunctionK.id).apply(call)

    val initial = State(toRetry = 4)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record(decision = OnError.Decision.giveUp, retries = 3),
        Record.retry(delay = 10.millis, retries = 2),
        Record.retry(delay = 10.millis, retries = 1),
        Record.retry(delay = 10.millis, retries = 0)),
      delays = List(
        10.millis,
        10.millis,
        10.millis))
    actual shouldEqual expected
  }

  test("exponential.jitter.cap") {
    val random = Random.State(12345L)
    val policy = Strategy
      .exponential(5.millis)
      .jitter(random)
      .cap(200.millis)

    val call = StateT { _.call }
    val result = Retry(policy, onError).apply(call)

    val initial = State(toRetry = 7)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 200.millis, retries = 6),
        Record.retry(delay = 65600.microseconds, retries = 5),
        Record.retry(delay = 16800.microseconds, retries = 4),
        Record.retry(delay = 39600.microseconds, retries = 3),
        Record.retry(delay = 13200.microseconds, retries = 2),
        Record.retry(delay = 7900.microseconds, retries = 1),
        Record.retry(delay = 0.nanoseconds, retries = 0)),
      delays = List(
        200.millis,
        65600.microseconds,
        16800.microseconds,
        39600.microseconds,
        13200.microseconds,
        7900.microseconds,
        0.nanoseconds))
    actual shouldEqual expected
  }

  test("const") {
    val strategy = Strategy
      .const(1.millis)
      .limit(4.millis)
    val call = StateT { _.call }
    val result = call.retry(strategy, onError)

    val initial = State(toRetry = 6)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      toRetry = 1,
      records = List(
        Record(decision = OnError.Decision.giveUp, retries = 4),
        Record.retry(delay = 1.millis, retries = 3),
        Record.retry(delay = 1.millis, retries = 2),
        Record.retry(delay = 1.millis, retries = 1),
        Record.retry(delay = 1.millis, retries = 0)),
      delays = List(
        1.millis,
        1.millis,
        1.millis,
        1.millis))
    actual shouldEqual expected
  }

  test("until") {
    val untilTime = StateT.InitialTime + 2.milliseconds
    val strategy =
      Strategy
      .const(1.millis)
      .limit(4.millis)
      .until(Instant.ofEpochMilli(untilTime.toMillis))

    val call = StateT { _.call }
    val result = Retry(strategy, onError).apply(call)

    val initial = State(toRetry = 4)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      toRetry = 1,
      records = List(
        Record(decision = OnError.Decision.giveUp, retries = 2),
        Record.retry(delay = 1.millis, retries = 1),
        Record.retry(delay = 1.millis, retries = 0)),
      delays = List(
        1.millis,
        1.millis))
    actual shouldEqual expected
  }

  test("resetAfter 0.millis") {
    val strategy = Strategy
      .fibonacci(5.millis)
      .resetAfter(0.millis)

    val call = StateT { _.call }
    val result = Retry(strategy, onError).apply(call)

    val initial = State(toRetry = 3)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 5.millis, retries = 0),
        Record.retry(delay = 5.millis, retries = 0),
        Record.retry(delay = 5.millis, retries = 0)),
      delays = List(
        5.millis,
        5.millis,
        5.millis))
    actual shouldEqual expected
  }

  test("resetAfter 1.minute") {
    val strategy = Strategy
      .fibonacci(5.millis)
      .resetAfter(1.minute)

    val call = StateT { _.call }
    val result = Retry(strategy, onError).apply(call)

    val initial = State(toRetry = 3)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 10.millis, retries = 2),
        Record.retry(delay = 5.millis, retries = 1),
        Record.retry(delay = 5.millis, retries = 0)),
      delays = List(
        10.millis,
        5.millis,
        5.millis))
    actual shouldEqual expected
  }

  test("exponential") {
    val strategy = Strategy.exponential(1.millis)

    val call = StateT { _.call }
    val result = Retry(strategy, onError).apply(call)

    val initial = State(toRetry = 4)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 8.millis, retries = 3),
        Record.retry(delay = 4.millis, retries = 2),
        Record.retry(delay = 2.millis, retries = 1),
        Record.retry(delay = 1.millis, retries = 0)),
      delays = List(
        8.millis,
        4.millis,
        2.millis,
        1.millis))
    actual shouldEqual expected
  }

  test("jitter") {
    val random = Random.State(12345L)
    val policy = Strategy
      .const(1.second)
      .jitter(random)

    val call = StateT { _.call }
    val result = Retry(policy, onError).apply(call)

    val initial = State(toRetry = 7)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 820.millis, retries = 6),
        Record.retry(delay = 410.millis, retries = 5),
        Record.retry(delay = 210.millis, retries = 4),
        Record.retry(delay = 990.millis, retries = 3),
        Record.retry(delay = 660.millis, retries = 2),
        Record.retry(delay = 790.millis, retries = 1),
        Record.retry(delay = 0.millis, retries = 0)),
      delays = List(
        820.millis,
        410.millis,
        210.millis,
        990.millis,
        660.millis,
        790.millis,
        0.millis))
    actual shouldEqual expected
  }


  test("jitter half") {
    val random = Random.State(12345L)
    val policy = Strategy
      .const(1.second)
      .jitter(random, 0.5)

    val call = StateT { _.call }
    val result = Retry(policy, onError).apply(call)

    val initial = State(toRetry = 7)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 910.millis, retries = 6),
        Record.retry(delay = 705.millis, retries = 5),
        Record.retry(delay = 605.millis, retries = 4),
        Record.retry(delay = 995.millis, retries = 3),
        Record.retry(delay = 830.millis, retries = 2),
        Record.retry(delay = 895.millis, retries = 1),
        Record.retry(delay = 500.millis, retries = 0)),
      delays = List(
        910.millis,
        705.millis,
        605.millis,
        995.millis,
        830.millis,
        895.millis,
        500.millis))
    actual shouldEqual expected
  }
}

object RetrySpec {

  type Error = Unit

  type FE[A] = Either[Error, A]

  val onError: OnError[StateT, Error] = (_, status: Retry.Status, decision: OnError.Decision) => {
    StateT { state =>
      val details = Record(decision, status.retries)
      val state1 = state.copy(records = details :: state.records)
      (state1, ().asRight)
    }
  }

  type StateT[A] = cats.data.StateT[Id, State, FE[A]]

  object StateT {

    val InitialTime = FiniteDuration(
      length = Instant.parse("2022-01-15T12:34:56Z").toEpochMilli(),
      unit = MILLISECONDS
    )

    implicit val MonadErrorStateT: MonadError[StateT, Error] = new MonadError[StateT, Error] {

      def flatMap[A, B](fa: StateT[A])(f: A => StateT[B]) = {
        StateT[B] { s =>
          val (s1, a) = fa.run(s)
          a.fold(a => (s1, a.asLeft), a => f(a).run(s1))
        }
      }

      def tailRecM[A, B](a: A)(f: A => StateT[Either[A, B]]) = {

        @tailrec
        def apply(s: State, a: A): (State, FE[B]) = {
          val (s1, b) = f(a).run(s)
          b match {
            case Right(Right(b)) => (s1, b.asRight)
            case Right(Left(b))  => apply(s1, b)
            case Left(b)         => (s1, b.asLeft)
          }
        }

        StateT { s => apply(s, a) }
      }

      def raiseError[A](e: Error) = {
        StateT { s => (s, e.asLeft) }
      }

      def handleErrorWith[A](fa: StateT[A])(f: Error => StateT[A]) = {
        StateT { s =>
          val (s1, a) = fa.run(s)
          a.fold(a => f(a).run(s1), a => (s1, a.asRight))
        }
      }

      def pure[A](a: A) = StateT { s => (s, a.asRight) }
    }


    implicit val SleepStateT: Sleep[StateT] = new Sleep[StateT] {

      def applicative = Applicative[StateT]

      def realTime = StateT { s =>
        val delay = s.delays.fold(0.milliseconds)(_ + _)
        (s, (InitialTime + delay).asRight)
      }

      def monotonic = StateT { s => (s, 0.milliseconds.asRight) }

      def sleep(duration: FiniteDuration) = {
        StateT { s => (s.sleep(duration), ().asRight) }
      }

    }

    def apply[A](f: State => (State, FE[A])): StateT[A] = {
      cats.data.StateT[Id, State, FE[A]](f)
    }
  }


  final case class State(
    toRetry: Int = 0,
    records: List[Record] = Nil,
    delays: List[FiniteDuration] = Nil
  ) { self =>

    def sleep(duration: FiniteDuration): State = {
      copy(delays = duration :: delays)
    }

    def call: (State, FE[Unit]) = {
      if (toRetry > 0) {
        (copy(toRetry = toRetry - 1), ().asLeft)
      } else {
        (self, ().asRight)
      }
    }
  }


  final case class Record(decision: OnError.Decision, retries: Int)

  object Record {

    def retry(delay: FiniteDuration, retries: Int): Record = {
      val decision = OnError.Decision.retry(delay)
      Record(decision, retries)
    }
  }
}
