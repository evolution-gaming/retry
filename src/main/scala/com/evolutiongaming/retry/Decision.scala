package com.evolutiongaming.retry

import com.evolutiongaming.retry.Retry.{Decide, Status}

import scala.concurrent.duration.FiniteDuration

sealed abstract class Decision extends Product

object Decision {

  def retry(delay: FiniteDuration, status: Status, decide: Decide): Decision = {
    Retry(delay, status, decide)
  }

  def giveUp: Decision = GiveUp


  final case class Retry(delay: FiniteDuration, status: Status, decide: Decide) extends Decision

  case object GiveUp extends Decision


  implicit class StrategyDecisionOps(val self: Decision) extends AnyVal {

    def mapDecide(f: Decide => Decide): Decision = {
      self match {
        case Decision.GiveUp   => Decision.giveUp
        case a: Decision.Retry => a.copy(decide = f(a.decide))
      }
    }
  }
}