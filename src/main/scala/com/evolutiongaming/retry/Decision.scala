package com.evolutiongaming.retry

import com.evolutiongaming.retry.Retry.Status

import scala.concurrent.duration.FiniteDuration

sealed abstract class Decision extends Product

object Decision {

  def retry(delay: FiniteDuration, status: Status, strategy: Strategy): Decision = {
    Retry(delay, status, strategy)
  }

  def giveUp: Decision = GiveUp


  final case class Retry(delay: FiniteDuration, status: Status, strategy: Strategy) extends Decision

  case object GiveUp extends Decision


  implicit class StrategyDecisionOps(val self: Decision) extends AnyVal {

    def flatMap(f: Retry => Decision): Decision = {
      self match {
        case Decision.GiveUp   => Decision.giveUp
        case a: Decision.Retry => f(a)
      }
    }

    def mapStrategy(f: Strategy => Strategy): Decision = {
      self.flatMap { a => a.copy(strategy = f(a.strategy)) }
    }
  }
}