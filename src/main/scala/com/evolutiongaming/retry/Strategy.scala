package com.evolutiongaming.retry

import java.time.Instant

import com.evolutiongaming.random.Random
import com.evolutiongaming.retry.Retry.{Decide, Status}

import scala.concurrent.duration.FiniteDuration


final case class Strategy(decide: Decide)

object Strategy {

  def const(delay: FiniteDuration): Strategy = {
    def decide: Decide = new Decide {
      def apply(status: Status, now: Instant) = {
        Decision.retry(delay, status, decide)
      }
    }

    Strategy(decide)
  }


  def fibonacci(initial: FiniteDuration): Strategy = {
    val unit = initial.unit

    def recur(a: Long, b: Long): Decide = new Decide {

      def apply(status: Status, now: Instant) = {
        val delay = FiniteDuration(b, unit)
        Decision.retry(delay, status, recur(b, a + b))
      }
    }

    Strategy(recur(0, initial.length))
  }


  /**
    * See https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
    */
  def fullJitter(initial: FiniteDuration, random: Random.State): Strategy = {

    def recur(random: Random.State): Decide = new Decide {

      def apply(status: Status, now: Instant) = {
        val e = math.pow(2.toDouble, status.retries + 1d)
        val max = initial.length * e
        val (random1, double) = random.double
        val delay = (max * double).toLong max initial.length
        val duration = FiniteDuration(delay, initial.unit)
        Decision.retry(duration, status, recur(random1))
      }
    }

    Strategy(recur(random))
  }


  def cap(strategy: Strategy, max: FiniteDuration): Strategy = {

    def recur(decide: Decide): Decide = new Decide {

      def apply(status: Status, now: Instant) = {
        decide(status, now) match {
          case Decision.GiveUp                       => Decision.giveUp
          case Decision.Retry(delay, status, decide) =>
            if (delay <= max) Decision.retry(delay, status, recur(decide))
            else Decision.retry(max, status, const(max).decide)
        }
      }
    }

    Strategy(recur(strategy.decide))
  }


  def limit(strategy: Strategy, max: FiniteDuration): Strategy = {

    def recur(decide: Decide): Decide = new Decide {

      def apply(status: Status, now: Instant) = {
        decide(status, now) match {
          case Decision.GiveUp                       => Decision.giveUp
          case Decision.Retry(delay, status, decide) =>
            if (status.delay + delay > max) Decision.giveUp
            else Decision.retry(delay, status, recur(decide))
        }
      }
    }

    Strategy(recur(strategy.decide))
  }


  def resetAfter(strategy: Strategy, cooldown: FiniteDuration): Strategy = {

    def recur(decide: Decide): Decide = new Decide {

      def apply(status: Status, now: Instant) = {

        val reset = status.last.toEpochMilli + cooldown.toMillis <= now.toEpochMilli

        val result = {
          if (reset) {
            val status = Status.empty(now)
            strategy.decide(status, now)
          } else {
            decide(status, now)
          }
        }
        result.mapDecide(recur)
      }
    }

    Strategy(recur(strategy.decide))
  }
}