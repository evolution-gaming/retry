package com.evolutiongaming.retry

import cats.effect.Clock
import cats.effect.GenTemporal
import scala.concurrent.duration.FiniteDuration

trait Sleep[F[_]] extends Clock[F] {

  def sleep(time: FiniteDuration): F[Unit]

}
object Sleep {

  def apply[F[_]](implicit F: Sleep[F]): Sleep[F] = F

  implicit def fromGenTemporal[F[_]](implicit F: GenTemporal[F, ?]): Sleep[F] =
    new Sleep[F] {
      def applicative = F.applicative
      def monotonic = F.monotonic
      def realTime = F.realTime
      def sleep(time: FiniteDuration) = F.sleep(time)
    }

}
