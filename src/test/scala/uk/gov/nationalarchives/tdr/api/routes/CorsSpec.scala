package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.HttpMethods.{GET, OPTIONS, POST}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.http.Routes

class CorsSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  private val baseConfig = ConfigFactory.load()

  "the pre-flight request" should "allow requests from the frontend" in {
    val frontendUrl = "https://some-frontend.example.com"

    val testConfig = baseConfig.withValue("frontend.url", ConfigValueFactory.fromAnyRef(frontendUrl))
    val route = new Routes(testConfig).route

    Options("/graphql") ~> route ~> check {
      header[`Access-Control-Allow-Origin`].map(_.value) should contain(frontendUrl)
      header[`Access-Control-Allow-Credentials`].map(_.value) should contain("true")
      header[`Access-Control-Allow-Headers`] should contain(`Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"))
      header[`Access-Control-Allow-Methods`] should contain(`Access-Control-Allow-Methods`(OPTIONS, POST, GET))
    }
  }

  "the pre-flight request" should "return the default origin if a different origin is given" in {
    val frontendUrl = "https://some-frontend.example.com"

    val testConfig = baseConfig.withValue("frontend.url", ConfigValueFactory.fromAnyRef(frontendUrl))
    val route = new Routes(testConfig).route

    val headers = List(Origin("https://some-other-domain.example.com"))
    Options("/graphql").withHeaders(headers) ~> route ~> check {
      header[`Access-Control-Allow-Origin`].map(_.value) should contain(frontendUrl)
    }
  }
}
