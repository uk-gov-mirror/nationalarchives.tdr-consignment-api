package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.http.Routes.route
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.io.Source.fromResource

class TransfersAgreementRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach  {

  private val addTransferAgreementJsonFilePrefix: String = "json/addtransferagreement_"

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.TransferAgreement").executeUpdate()
  }

  private def runTestMutation(mutation: String, expectedResult: String, token: OAuth2BearerToken = validUserToken()) = {
    runTestRequest(addTransferAgreementJsonFilePrefix, mutation, expectedResult, token)
  }

  private def runTestRequest(prefix: String, mutationPath: String, expectedResultPath: String, token: OAuth2BearerToken = validUserToken()) = {
    val mutation: String = fromResource(prefix + mutationPath).mkString
    val expectedResult: String = fromResource(prefix + expectedResultPath).mkString
    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(token) ~> route ~> check {
      print(responseAs[String])
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "return all requested fields from inserted Transfer Agreement object" in {
    runTestMutation("mutation_alldata.json", "data_all.json")
  }

  "The api" should "return the expected data from inserted Transfer Agreement object" in {
    runTestMutation("mutation_somedata.json", "data_some.json")
  }
}
