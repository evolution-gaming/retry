package com.evolutiongaming.retry

import java.time.Instant

import com.evolutiongaming.random.Random
import com.evolutiongaming.retry.Retry.Status

import scala.concurrent.duration._


trait Strategy {
  def apply(status: Status, now: Instant): Decision
}

object Strategy {

  def apply(f: (Status, Instant) => Decision): Strategy = (status, now) => f(status, now)

  def const(delay: FiniteDuration): Strategy = {

    def strategy: Strategy = {
      Strategy { (status, _) => Decision.retry(delay, status, strategy) }
    }

    strategy
  }


  def fibonacci(initial: FiniteDuration): Strategy = {
    val unit = initial.unit

    def loop(a: Long, b: Long): Strategy = Strategy {
      (status, _) => {
        val delay = FiniteDuration(b, unit)
        Decision.retry(delay, status, loop(b, a + b))
      }
    }

    loop(0, initial.length)
  }


  def exponential(initial: FiniteDuration): Strategy = {

    def strategy: Strategy = {
      (status, _) => {
        val e = math.pow(2.toDouble, status.retries.toDouble)
        val length = initial.length * e
        val duration = FiniteDuration(length.toLong, initial.unit)
        Decision.retry(duration, status, strategy)
      }
    }

    strategy
  }


  @deprecated("use `exponential(initial).jitter(random)` instead", "1.1.0")
  def fullJitter(initial: FiniteDuration, random: Random.State): Strategy = {

    def loop(random: Random.State): Strategy = Strategy {
      (status, _) =>
        val e = math.pow(2.toDouble, status.retries.toDouble)
        val max = initial.length * e
        val (random1, double) = random.double
        val delay = (max * double).toLong
        val duration = FiniteDuration(delay, initial.unit).toCoarsest
        Decision.retry(duration, status, loop(random1))
    }

    loop(random)
  }

  /** Caps the maximum delay between retries to a specific value. */
  def cap(strategy: Strategy, max: FiniteDuration): Strategy = {

    def loop(strategy: Strategy): Strategy = Strategy {
      (status, now) =>
        strategy(status, now).flatMap { case Decision.Retry(delay, status, strategy) =>
          if (delay <= max) Decision.retry(delay, status, loop(strategy))
          else Decision.retry(max, status, const(max))
        }
    }

    loop(strategy)
  }


  /** Limits the maximum time of the task to a specific value.
    *
    * The strategy will not schedule a new retry if expected duration
    * will exceed the `max` value.
    */
  def limit(strategy: Strategy, max: FiniteDuration): Strategy = {

    def loop(strategy: Strategy): Strategy = Strategy {
      (status, now) =>
        strategy(status, now).flatMap { case Decision.Retry(delay, status, strategy) =>
          if (status.delay + delay > max) Decision.giveUp
          else Decision.retry(delay, status, loop(strategy))
        }
    }

    loop(strategy)
  }


  /** Performs the task up until `end`.
    *
    * The deadline is not inclusive, i.e. it will not perform the task at,
    * exactly, the instant passed as a parameter.
    */
  def until(strategy: Strategy, end: Instant): Strategy = {

    def loop(strategy: Strategy): Strategy = Strategy {
      (status, now) =>
        strategy(status, now).flatMap { case Decision.Retry(delay, status, strategy) =>
          if (now.compareTo(end) >= 0) Decision.giveUp
          else Decision.retry(delay, status, loop(strategy))
        }
    }

    loop(strategy)
  }


  def resetAfter(strategy: Strategy, cooldown: FiniteDuration): Strategy = {

    def loop(strategy1: Strategy): Strategy = Strategy {
      (status, now) =>

        val reset = status.last.toEpochMilli + cooldown.toMillis <= now.toEpochMilli

        val result = {
          if (reset) {
            val status = Status.empty(now)
            strategy(status, now)
          } else {
            strategy1(status, now)
          }
        }
        result.mapStrategy(loop)
    }

    loop(strategy)
  }


  implicit class StrategyOps(val self: Strategy) extends AnyVal {

    def cap(max: FiniteDuration): Strategy = Strategy.cap(self, max)

    def limit(max: FiniteDuration): Strategy = Strategy.limit(self, max)

    def until(end: Instant): Strategy = Strategy.until(self, end)

    def resetAfter(cooldown: FiniteDuration): Strategy = Strategy.resetAfter(self, cooldown)

    /**
      * delay1 = delay * fraction
      * delay1 * random + delay - delay1
      * See https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
      */
    def jitter(random: Random.State, fraction: Double = 1.0): Strategy = {

      val fraction1 = fraction min 1.0 max 0.0

      def loop(strategy: Strategy, random: Random.State): Strategy = {
        (status, now) => {
          strategy(status, now).flatMap { case Decision.Retry(delay, status, strategy) =>
            val (random1, multiplier) = random.double
            val multiplier1 = (multiplier * 100).floor / 100
            val nanos = delay.toNanos
            val nanos1 = nanos * fraction1
            val delay1 = (nanos1 * multiplier1 + nanos - nanos1)
              .toLong
              .nanos
              .toCoarsest
            Decision.Retry(delay1, status, loop(strategy, random1))
          }
        }
      }

      loop(self, random)
    }
  }
}