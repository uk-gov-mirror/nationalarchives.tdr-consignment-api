package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, authenticateOAuth2Async, complete, entity, get, path, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.directives.Credentials
import com.typesafe.config._
import com.typesafe.scalalogging.Logger
import spray.json.JsValue
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, Token}

import scala.concurrent.{ExecutionContext, Future}


object Routes {

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = Logger("ApiServer")
  val ttlSeconds: Int = 10
  val url: String = ConfigFactory.load().getString("auth.url")


  def tokenAuthenticator(credentials: Credentials): Future[Option[Token]] = {
    credentials match {
      case Credentials.Provided(token) => Future {
        Some(KeycloakUtils(url).token(token))
      }
      case _ => Future.successful(None)
    }
  }

  val route: Route =
    (post & path("graphql")) {
      authenticateOAuth2Async("tdr", tokenAuthenticator) { accessToken =>
        entity(as[JsValue]) { requestJson =>
          GraphQLServer.endpoint(requestJson, accessToken)
        }
      }
    } ~ (get & path("healthcheck")) {
      complete(StatusCodes.OK)
    }
}
