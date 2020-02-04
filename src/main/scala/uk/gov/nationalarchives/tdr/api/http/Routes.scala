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
import http.GraphQLServer
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object Routes {

  implicit val system: ActorSystem = ActorSystem("consignmentApi")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = Logger("ApiServer")
  val ttlSeconds: Int = 60 * 10
  val url: String = ConfigFactory.load().getString("auth.url")
  val keycloakDeployment = TdrKeycloakDeployment(url, "tdr", ttlSeconds)


  def verifyToken(token: String): Boolean = {
    val tryVerify = Try {
      AdapterTokenVerifier.verifyToken(token, keycloakDeployment)
    }
    tryVerify match {
      case Success(_) => true
      case Failure(e) =>
        logger.warn(e.getMessage)
        false
    }
  }

  def tokenAuthenticator(credentials: Credentials): Future[Option[String]] = {
    credentials match {
      case Credentials.Provided(token) => Future {
        Some(token).filter(verifyToken)
      }
      case _ => Future.successful(None)
    }
  }

  val route: Route =
    (post & path("graphql")) {
      authenticateOAuth2Async("tdr", tokenAuthenticator) { _ =>
        entity(as[JsValue]) { requestJson =>
          GraphQLServer.endpoint(requestJson)
        }
      }
    } ~ (get & path("healthcheck")) {
      complete(StatusCodes.OK)
    }
}
