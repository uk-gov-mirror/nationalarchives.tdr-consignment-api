package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.HttpMethods.{GET, OPTIONS, POST}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.http.Routes

class CorsSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  val route = new Routes(ConfigFactory.load()).route

  "the pre-flight request" should "allow requests from the frontend" in {
    Options("/graphql") ~> route ~> check {
      header[`Access-Control-Allow-Origin`].map(_.value) should contain("https://tdr-frontend.example.com")
      header[`Access-Control-Allow-Credentials`].map(_.value) should contain("true")
      header[`Access-Control-Allow-Headers`] should contain(`Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"))
      header[`Access-Control-Allow-Methods`] should contain(`Access-Control-Allow-Methods`(OPTIONS, POST, GET))
    }
  }
}
