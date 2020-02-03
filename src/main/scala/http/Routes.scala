package http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{as, complete, entity, failWith, get, optionalHeaderValueByType, path, post, provide, reject}
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, Directives, Route}
import http.ApiServer.actorSystem
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.AccessToken
import spray.json.JsValue
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import Directives.onComplete
import org.apache.http.auth.InvalidCredentialsException

//noinspection ScalaStyle
object Routes {

  import actorSystem.dispatcher

  val ttl: Int = 60 * 10
  val keycloakDeployment = TdrKeycloakDeployment("https://auth.tdr-integration.nationalarchives.gov.uk/auth", "tdr", ttl)

  val route: Route =
    (post & path("graphql")) {
      bearerToken {
        case Some(token) =>
          onComplete(
            Future.failed[String](new InvalidCredentialsException())
//              Future.apply(AdapterTokenVerifier.verifyToken(token, keycloakDeployment))

          )
          {
            case Success(_) =>
              entity(as[JsValue]) { requestJson =>
                GraphQLServer.endpoint(requestJson)
              }
            case Failure(e) =>
              println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGGGGGGGGGHHHHHHHHHHHHHHHHHH")
              failWith(e)

          }
        case None => failWith(new InvalidCredentialsException())
      }
    } ~ (get & path("")) {
      complete(StatusCodes.OK)
    }

  private def bearerToken: Directive1[Option[String]] = {
    optionalHeaderValueByType(classOf[Authorization])
      .map(
        authHeader =>
          authHeader.collect {
            case Authorization(OAuth2BearerToken(token)) => token
          }
      )
  }
}
