package uk.gov.nationalarchives.tdr.api.http

import akka.http.scaladsl.model.HttpMethods.{GET, OPTIONS, POST}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.server.Directives.{complete, options, respondWithHeaders}
import akka.http.scaladsl.server.{Directive0, Route}
import com.typesafe.config.ConfigFactory

trait Cors {

  val frontendUrl: String = ConfigFactory.load().getString("frontend.url")

  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`(HttpOrigin(frontendUrl)),
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

}
