// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package clue

import cats.syntax.all._
import io.circe._
import io.circe.syntax._

trait GraphQLStreamingClient[F[_], S] extends GraphQLClient[F, S] {
  def status: F[StreamingClientStatus]

  def statusStream: fs2.Stream[F, StreamingClientStatus]

  def close(): F[Unit]

  protected trait StoppableSubscription[D] {
    val stream: fs2.Stream[F, D]
    def stop(): F[Unit]
  }

  type Subscription[D] <: StoppableSubscription[D]

  def subscribe(
    subscription:  GraphQLOperation[S],
    operationName: Option[String] = None
  ): SubscriptionApplied[subscription.Variables, subscription.Data] = {
    import subscription.implicits._
    SubscriptionApplied(subscription, operationName)
  }

  case class SubscriptionApplied[V, D] private (
    subscription:        GraphQLOperation[S],
    operationName:       Option[String] = None
  )(implicit varEncoder: Encoder[V], dataDecoder: Decoder[D]) {
    def apply(variables: V): F[Subscription[D]] =
      subscribeInternal[D](subscription.document, operationName, variables.asJson.some)

    def apply: F[Subscription[D]] =
      subscribeInternal[D](subscription.document, operationName)
  }
  object SubscriptionApplied {
    implicit def withoutVariables[V, D](applied: SubscriptionApplied[V, D]): F[Subscription[D]] =
      applied.apply
  }

  protected def subscribeInternal[D: Decoder](
    document:      String,
    operationName: Option[String] = None,
    variables:     Option[Json] = None
  ): F[Subscription[D]]
}
