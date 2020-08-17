package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, RouteResult}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.scalalogging.Logger

object RouteLogging {
  implicit val actorSystem: ActorSystem = ActorSystem("graphql-server")
  implicit val materializer: Materializer = Materializer(actorSystem)

  private val logger = Logger(classOf[Routes])

  def logging: Directive0 = {
    def logNon200Response(req: HttpRequest)(res: RouteResult): Unit = {
      res match {
        case Complete(resp) =>
          if (resp.status.isFailure()) {
            logger.warn(s"Non 200 Response - Request: $req, Request Entity: ${Unmarshal(req.entity).to[String].toString}, " +
              s"Response: ${Unmarshal(resp).to[String].toString}")
          }
        case Rejected(reason) =>
          logger.warn(s"Rejected Reason: " + reason.mkString(", "))
      }
    }
    DebuggingDirectives.logRequestResult(LoggingMagnet(_ => logNon200Response))
  }
}
