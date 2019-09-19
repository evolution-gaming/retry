package com.evolutiongaming.retry

import cats.Applicative
import cats.implicits._
import com.evolutiongaming.retry.{Decision => StrategyDecision}

import scala.concurrent.duration.FiniteDuration

trait OnError[F[_], A] {

  def apply(error: A, status: Retry.Status, decision: OnError.Decision): F[Unit]
}

object OnError {

  def empty[F[_] : Applicative, A]: OnError[F, A] = (_, _, _) => ().pure[F]


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