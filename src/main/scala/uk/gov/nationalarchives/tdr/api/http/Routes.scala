package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.Origin
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{Credentials, DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Route, RouteResult}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config._
import com.typesafe.scalalogging.Logger
import spray.json.JsValue
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, Token}

import scala.concurrent.{ExecutionContext, Future}

class Routes(val config: Config) extends Cors {

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = Logger("ApiServer")
  val ttlSeconds: Int = 10
  val url: String = config.getString("auth.url")

  def unmarshalEntity(entity: ResponseEntity): String = {
    Unmarshal(entity).to[String].toString
  }

  def isNon200Status(status: StatusCode): Boolean = {
    val code = status.intValue.toString
    !code.startsWith("2")
  }

  def logging: Directive0 = {
    def logNon200Response(req: HttpRequest): RouteResult => Unit = {
      case RouteResult.Complete(res) =>
        if (isNon200Status(res.status)) {
          val responseEntity = unmarshalEntity(res.entity)
          logger.info(s"Non 200 Response: $req\nStatus Code: ${res.status}\nResponse Entity: $responseEntity")
        }
      case RouteResult.Rejected(rej) =>
        logger.info(s"Rejected: " + rej.mkString(", "), Logging.InfoLevel)
    }
    DebuggingDirectives.logRequestResult(LoggingMagnet(_ => logNon200Response))
  }

  def tokenAuthenticator(credentials: Credentials): Future[Option[Token]] = {
    credentials match {
      case Credentials.Provided(token) => Future {
        KeycloakUtils(url).token(token)
      }
      case _ => Future.successful(None)
    }
  }

  val route: Route = logging {
    optionalHeaderValueByType[Origin](()) { originHeader =>
      corsHandler((post & path("graphql")) {
        authenticateOAuth2Async("tdr", tokenAuthenticator) { accessToken =>
          entity(as[JsValue]) { requestJson =>
            GraphQLServer.endpoint(requestJson, accessToken)
          }
        }
      }, originHeader)
    } ~ (get & path("healthcheck")) {
      complete(StatusCodes.OK)
    }
  }
}
