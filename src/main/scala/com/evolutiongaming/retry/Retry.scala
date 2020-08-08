package com.evolutiongaming.retry

import java.time.Instant

import cats.arrow.FunctionK
import cats.effect.{Clock, Timer}
import cats.implicits._
import cats.{MonadError, ~>}
import com.evolutiongaming.catshelper.ClockHelper._
import com.evolutiongaming.catshelper.{Log, MonadThrowable}

import scala.concurrent.duration._

trait Retry[F[_]] {

  def apply[A](fa: F[A]): F[A]
}

object Retry {

  def apply[F[_]](implicit F: Retry[F]): Retry[F] = F


  def empty[F[_]]: Retry[F] = new Retry[F] {
    def apply[A](fa: F[A]) = fa
  }


  def apply[F[_] : Timer, E](
    strategy: Strategy,
    onError: OnError[F, E])(implicit
    F: MonadError[F, E]
  ): Retry[F] = {

    type S = (Status, Strategy)

    def retry[A](status: Status, strategy: Strategy, error: E): F[Either[S, A]] = {

      def onError1(status: Status, decision: Decision) = {
        val decision1 = OnError.Decision(decision)
        onError(error, status, decision1)
      }

      for {
        now      <- Clock[F].instant
        decision  = strategy(status, now)
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
          zero    = (Status.empty(now), strategy)
          result <- zero.tailRecM[F, A] { case (status, strategy) =>
            fa.redeemWith[Either[S, A]](
              a => retry[A](status, strategy, a),
              a => a.asRight[(Status, Strategy)].pure[F])
          }
        } yield result
      }
    }
  }


  def apply[F[_] : Timer, E](
    strategy: Strategy)(implicit
    F: MonadError[F, E]
  ): Retry[F] = {
    apply(strategy, OnError.empty[F, E])
  }


  implicit class RetryOps[F[_]](val self: Retry[F]) extends AnyVal {

    def mapK[G[_]](fg: F ~> G, gf: G ~> F): Retry[G] = new Retry[G] {
      def apply[A](fa: G[A]) = fg(self(gf(fa)))
    }

    def toFunctionK: FunctionK[F, F] = new FunctionK[F, F] {
      def apply[A](fa: F[A]) = self(fa)
    }
  }


  final case class Status(retries: Int, delay: FiniteDuration, last: Instant) { self =>

    def plus(delay: FiniteDuration): Status = {
      copy(retries = retries + 1, delay = self.delay + delay)
    }
  }

  object Status {
    def empty(last: Instant): Status = Status(0, Duration.Zero, last)
  }


  object implicits {

    implicit class OpsRetry[F[_], A](val self: F[A]) extends AnyVal {

      def retry(implicit retry: Retry[F]): F[A] = retry(self)

      def retry[E](
        strategy: Strategy,
        onError: OnError[F, E])(implicit
        F: MonadError[F, E],
        timer: Timer[F]
      ): F[A] = {
        Retry(strategy, onError).apply(self)
      }

      def retry[E](
        strategy: Strategy)(implicit
        F: MonadError[F, E],
        timer: Timer[F]
      ): F[A] = {
        self.retry(strategy, OnError.empty[F, E])
      }

      def retry(
        strategy: Strategy,
        log: Log[F])(implicit
        F: MonadThrowable[F],
        timer: Timer[F]
      ): F[A] = {
        self.retry(strategy, OnError.fromLog(log))
      }
    }
  }
}


