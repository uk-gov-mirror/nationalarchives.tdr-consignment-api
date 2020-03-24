package uk.gov.nationalarchives.tdr.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{as, authenticateOAuth2Async, complete, entity, get, path, post, _}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive0, Route}
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
        KeycloakUtils(url).token(token)
      }
      case _ => Future.successful(None)
    }
  }

  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`(HttpOrigin("http://localhost:9000")),
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
    )
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK)
      .withHeaders(
        `Access-Control-Allow-Methods`(OPTIONS, POST, GET)
      )
    )
  }

  def corsHandler(r: Route): Route = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

  val route: Route =
    corsHandler((post & path("graphql")) {
      authenticateOAuth2Async("tdr", tokenAuthenticator) { accessToken =>
        entity(as[JsValue]) { requestJson =>
          GraphQLServer.endpoint(requestJson, accessToken)
        }
      }
    } ~ (get & path("healthcheck")) {
      complete(StatusCodes.OK)
    })
}
