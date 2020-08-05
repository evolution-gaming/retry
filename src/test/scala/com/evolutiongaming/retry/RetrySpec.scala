package com.evolutiongaming.retry

import cats._
import cats.arrow.FunctionK
import cats.effect.{Clock, Timer}
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
    val strategy = Strategy.fibonacci(5.millis).cap(200.millis)

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

  test("fullJitter") {
    val rng = Random.State(12345L)
    val policy = Strategy.fullJitter(5.millis, rng).cap(200.millis)

    val call = StateT { _.call }
    val result = Retry(policy, onError).apply(call)

    val initial = State(toRetry = 7)
    val actual = result.run(initial).map(_._1)
    val expected = State(
      records = List(
        Record.retry(delay = 200.millis, retries = 6),
        Record.retry(delay = 133.millis, retries = 5),
        Record.retry(delay = 34.millis, retries = 4),
        Record.retry(delay = 79.millis, retries = 3),
        Record.retry(delay = 26.millis, retries = 2),
        Record.retry(delay = 15.millis, retries = 1),
        Record.retry(delay = 5.millis, retries = 0)),
      delays = List(
        200.millis,
        133.millis,
        34.millis,
        79.millis,
        26.millis,
        15.millis,
        5.millis))
    actual shouldEqual expected
  }

  test("const") {
    val strategy = Strategy.const(1.millis).limit(4.millis)
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
    val strategy =
      Strategy
      .const(1.millis)
      .limit(4.millis)
      .until(Instant.ofEpochMilli(StateT.InitialTime + 2))

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
    val strategy = Strategy.fibonacci(5.millis).resetAfter(0.millis)

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
    val strategy = Strategy.fibonacci(5.millis).resetAfter(1.minute)

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

    val InitialTime = System.currentTimeMillis()

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


    implicit val TimerStateT: Timer[StateT] = new Timer[StateT] {

      val clock = new Clock[StateT] {

        def realTime(unit: TimeUnit) = StateT { s =>
          val delay = s.delays.map(_.toMillis).sum
          (s, (InitialTime + delay).asRight)
        }
        def monotonic(unit: TimeUnit) = StateT { s => (s, 0L.asRight) }

      }

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
