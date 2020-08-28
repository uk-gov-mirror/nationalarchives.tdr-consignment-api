package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.Origin
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.{Credentials, DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Route, RouteResult}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.config._
import com.typesafe.scalalogging.Logger
import spray.json.JsValue
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, Token}

import scala.concurrent.{ExecutionContext, Future}

class Routes(val config: Config) extends Cors {

  private val logger = Logger(classOf[Routes])

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher

  val ttlSeconds: Int = 10
  val url: String = config.getString("auth.url")

  def tokenAuthenticator(credentials: Credentials): Future[Option[Token]] = {
    credentials match {
      case Credentials.Provided(token) => Future {
        KeycloakUtils(url).token(token)
      }
      case _ => Future.successful(None)
    }
  }

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
