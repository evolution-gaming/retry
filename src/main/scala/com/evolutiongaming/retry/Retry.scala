package com.evolutiongaming.retry

import java.time.Instant

import cats.arrow.FunctionK
import cats.effect.{Bracket, Clock, Timer}
import cats.implicits._
import cats.~>
import com.evolutiongaming.catshelper.ClockHelper._
import com.evolutiongaming.catshelper.EffectHelper._

import scala.concurrent.duration._

trait Retry[F[_]] {

  def apply[A](fa: F[A]): F[A]
}

object Retry {

  def apply[F[_]](implicit F: Retry[F]): Retry[F] = F


  def apply[F[_] : Timer, E](
    strategy: Strategy,
    onError: OnError[F, E])(implicit
    bracket: Bracket[F, E]
  ): Retry[F] = {

    type S = (Status, Decide)

    def retry[A](status: Status, decide: Decide, error: E): F[Either[S, A]] = {

      def onError1(status: Status, decision: Decision) = {
        val decision1 = OnError.Decision(decision)
        onError(error, status, decision1)
      }

      for {
        now      <- Clock[F].instant
        decision  = decide(status, now)
        result   <- decision match {
          case Decision.GiveUp =>
            for {
              _      <- onError1(status, decision)
              result <- error.raiseError[F, Either[S, A]]
            } yield result

          case Decision.Retry(delay, status, decide) =>
            for {
              _ <- onError1(status, decision)
              _ <- Timer[F].sleep(delay)
            } yield {
              (status.plus(delay), decide).asLeft[A]
            }
        }
      } yield result
    }

    new Retry[F] {

      def apply[A](fa: F[A]) = {
        for {
          now    <- Clock[F].instant
          zero    = (Status.empty(now), strategy.decide)
          result <- zero.tailRecM[F, A] { case (status, decide) =>
            fa.redeemWith[Either[S, A], E](
              error => retry[A](status, decide, error),
              result => result.asRight[(Status, Decide)].pure[F])
          }
        } yield result
      }
    }
  }


  def apply[F[_] : Timer, E](
    strategy: Strategy)(implicit
    bracket: Bracket[F, E]
  ): Retry[F] = {
    apply(strategy, OnError.empty[F, E])
  }


  def empty[F[_]]: Retry[F] = new Retry[F] {
    def apply[A](fa: F[A]) = fa
  }


  implicit class RetryOps[F[_]](val self: Retry[F]) extends AnyVal {

    def mapK[G[_]](fg: F ~> G, gf: G ~> F): Retry[G] = new Retry[G] {
      def apply[A](fa: G[A]) = fg(self(gf(fa)))
    }

    def toFunctionK: FunctionK[F, F] = new FunctionK[F, F] {
      def apply[A](fa: F[A]) = self(fa)
    }
  }


  trait Decide {
    def apply(status: Status, now: Instant): Decision
  }


  final case class Status(retries: Int, delay: FiniteDuration, last: Instant) { self =>

    def plus(delay: FiniteDuration): Status = {
      copy(retries = retries + 1, delay = self.delay + delay)
    }
  }

  object Status {
    def empty(last: Instant): Status = Status(0, Duration.Zero, last)
  }


  implicit class StrategyOps(val self: Strategy) extends AnyVal {

    def cap(max: FiniteDuration): Strategy = Strategy.cap(self, max)

    def limit(max: FiniteDuration): Strategy = Strategy.limit(self, max)

    def resetAfter(cooldown: FiniteDuration): Strategy = Strategy.resetAfter(self, cooldown)
  }
}


