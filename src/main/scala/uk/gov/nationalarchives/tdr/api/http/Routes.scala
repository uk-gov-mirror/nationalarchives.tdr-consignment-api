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
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.AccessToken
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object Routes {

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = Logger("ApiServer")
  val ttlSeconds: Int = 10
  val url: String = ConfigFactory.load().getString("auth.url")
  val keycloakDeployment = TdrKeycloakDeployment(url, "tdr", ttlSeconds)


  def verifyToken(token: String): Option[AccessToken] = {
    val tryVerify = Try {
      AdapterTokenVerifier.verifyToken(token, keycloakDeployment)
    }
    tryVerify match {
      case Success(token) => Some(token)
      case Failure(e) =>
        logger.warn(e.getMessage)
        Option.empty
    }
  }

  def tokenAuthenticator(credentials: Credentials): Future[Option[AccessToken]] = {
    credentials match {
      case Credentials.Provided(token) => Future {
        verifyToken(token)
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
