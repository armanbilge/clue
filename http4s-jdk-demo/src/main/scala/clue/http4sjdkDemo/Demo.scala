package clue.http4sjdkDemo

import cats.Applicative
import cats.effect.Async
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.all._
import clue.ApolloWebSocketClient
import clue.GraphQLOperation
import clue.PersistentStreamingClient
import clue.http4sjdk.Http4sJDKWSBackend
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto._
import org.http4s.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._
import clue.TransactionalClient
import scala.util.Random

object Demo extends IOApp.Simple {

  object Query extends GraphQLOperation[Unit] {
    type Data      = Json
    type Variables = Json

    override val document: String = """
    |query {
    |  observations(programId: "p-2") {
    |    nodes {
    |      id
    |      name
    |      status
    |    }
    |  }
    |}""".stripMargin

    override val varEncoder: Encoder[Variables] = Encoder[Json]

    override val dataDecoder: Decoder[Data] = Decoder[Json]
  }

  object Subscription extends GraphQLOperation[Unit] {
    type Data      = Json
    type Variables = Json

    override val document: String = """
    |subscription {
    |  observationEdit(programId:"p-2") {
    |    id
    |  }
    |}""".stripMargin

    override val varEncoder: Encoder[Variables] = Encoder[Json]

    override val dataDecoder: Decoder[Data] = Decoder[Json]
  }

  object Mutation extends GraphQLOperation[Unit] {
    type Data = Json
    case class Variables(observationId: String, status: String)

    override val document: String = """
    |mutation ($observationId: ObservationId!, $status: ObsStatus!){
    |  updateObservation(input: {observationId: $observationId, status: $status}) {
    |    id
    |  }
    |}""".stripMargin

    override val varEncoder: Encoder[Variables] = deriveEncoder

    override val dataDecoder: Decoder[Data] = Decoder[Json]
  }

  def withLogger[F[_]: Sync]: Resource[F, Logger[F]] =
    Resource.make(Slf4jLogger.create[F])(_ => Applicative[F].unit)

  def withStreamingClient[F[_]: Async: Logger]
    : Resource[F, PersistentStreamingClient[F, Unit, _, _]] =
    for {
      backend <- Http4sJDKWSBackend[F]
      uri      = uri"wss://lucuma-odb-development.herokuapp.com/ws"
      sc      <- Resource.eval(ApolloWebSocketClient.of[F, Unit](uri)(Async[F], Logger[F], backend))
      _       <- Resource.make(sc.connect() >> sc.initialize())(_ => sc.terminate() >> sc.disconnect())
    } yield sc

  val allStatus =
    List("NEW", "INCLUDED", "PROPOSED", "APPROVED", "FOR_REVIEW", "READY", "ONGOING", "OBSERVED")

  def randomMutate(client: TransactionalClient[IO, Unit], ids: List[String]) =
    for {
      id     <- IO(ids(Random.between(0, ids.length)))
      status <- IO(allStatus(Random.between(0, allStatus.length)))
      _      <- client.request(Mutation)(Mutation.Variables(id, status))
    } yield ()

  def mutator(client: TransactionalClient[IO, Unit], ids: List[String]) =
    for {
      _ <- IO.sleep(3.seconds)
      _ <- randomMutate(client, ids)
      _ <- IO.sleep(2.seconds)
      _ <- randomMutate(client, ids)
      _ <- IO.sleep(3.seconds)
      _ <- randomMutate(client, ids)
    } yield ()

  def run =
    withLogger[IO].use { implicit logger =>
      withStreamingClient[IO].use { implicit client =>
        for {
          result       <- client.request(Query)
          _            <- IO.println(result)
          subscription <- client.subscribe(Subscription)
          fiber        <- subscription.stream.evalTap(_ => IO.println("UPDATE!")).compile.drain.start
          _            <- mutator(client, (result \\ "id").map(_.as[String].toOption.get)).start
          _            <- IO.sleep(10.seconds)
          _            <- subscription.stop()
          _            <- fiber.join
          result       <- client.request(Query)
          _            <- IO.println(result)

        } yield ()
      }
    }
}
