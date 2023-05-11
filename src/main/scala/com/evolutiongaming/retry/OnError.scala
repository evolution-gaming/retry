package com.evolutiongaming.retry

import cats.Applicative
import cats.implicits._
import com.evolutiongaming.catshelper.Log
import com.evolutiongaming.retry.{Decision => StrategyDecision}

import scala.concurrent.duration.FiniteDuration

trait OnError[F[_], A] {

  def apply(error: A, status: Retry.Status, decision: OnError.Decision): F[Unit]
}

object OnError {

  def empty[F[_] : Applicative, A]: OnError[F, A] = (_, _, _) => ().pure[F]


  def apply[F[_] : Applicative](f: Throwable => F[Unit]): OnError[F, Throwable] = {
    (error: Throwable, _: Retry.Status, decision: Decision) =>
      decision match {
        case OnError.Decision.Retry(_) => f(error)
        case OnError.Decision.GiveUp   => ().pure[F]
      }
  }

  def fullLogOnRetry[F[_]](log: Log[F]): OnError[F, Throwable] =
    onRetry(log) { (error, delay) =>
      log.warn(s"failed, retrying in $delay, error: $error", error)
    }


  def fromLog[F[_]](log: Log[F]): OnError[F, Throwable] =
    onRetry(log) { (error, delay) =>
      log.warn(s"failed, retrying in $delay, error: $error")
    }

  private def onRetry[F[_]](log: Log[F])(callback: (Throwable, FiniteDuration) => F[Unit]): OnError[F, Throwable] = {
    (error: Throwable, status: Retry.Status, decision: Decision) => {
      decision match {
        case OnError.Decision.Retry(delay) =>
          callback(error, delay)

        case OnError.Decision.GiveUp =>
          val retries = status.retries
          val duration = status.delay
          log.error(s"failed after $retries retries within $duration: $error", error)
      }
    }
  }

  sealed abstract class Decision extends Product

  object Decision {

    def retry(delay: FiniteDuration): Decision = Retry(delay)

    def giveUp: Decision = GiveUp


    def apply(decision: StrategyDecision): Decision = {
      decision match {
        case StrategyDecision.Retry(delay, _, _) => Decision.retry(delay)
        case StrategyDecision.GiveUp             => Decision.giveUp
      }
    }


    final case class Retry(delay: FiniteDuration) extends Decision

    case object GiveUp extends Decision
  }
}