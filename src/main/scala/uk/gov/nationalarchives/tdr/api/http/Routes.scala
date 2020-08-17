package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Origin
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.typesafe.config._
import spray.json.JsValue
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, Token}

import scala.concurrent.{ExecutionContext, Future}

class Routes(val config: Config) extends Cors {

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
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

  val route: Route = RouteLogging.logging {
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
