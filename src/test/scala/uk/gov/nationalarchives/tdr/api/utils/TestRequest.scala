package uk.gov.nationalarchives.tdr.api.utils

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.{GraphqlError, getDataFromFile, unmarshalResponse, validUserToken}
import uk.gov.nationalarchives.tdr.api.http.Routes.route

import scala.io.Source.fromResource
import scala.reflect.ClassTag

trait TestRequest extends AnyFlatSpec with ScalatestRouteTest with Matchers {
  def runTestRequest[A](prefix: String)(queryFileName: String, token: OAuth2BearerToken)
                       (implicit decoder: Decoder[A], classTag: ClassTag[A])
  : A = {
    implicit val unmarshaller: FromResponseUnmarshaller[A] = unmarshalResponse[A]()
    val getSeriesQuery: String = fromResource(prefix + s"$queryFileName.json").mkString
    Post("/graphql").withEntity(ContentTypes.`application/json`, getSeriesQuery) ~> addCredentials(token) ~> route ~> check {
      responseAs[A]
    }
  }

}
