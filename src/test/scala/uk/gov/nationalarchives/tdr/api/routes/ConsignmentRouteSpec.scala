package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.tdr.api.http.Routes.route

import scala.io.Source.fromResource

class ConsignmentRouteSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  private val addConsignmentJsonFilePrefix: String = "json/addconsignment_"

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Consignment").executeUpdate()
    val resetIdCount = "alter table consignmentapi.Consignment alter column ConsignmentId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetIdCount).executeUpdate()
  }

  private def runTestMutation(mutation: String, expectedResult: String, token: OAuth2BearerToken = validUserToken()) = {
    runTestRequest(addConsignmentJsonFilePrefix, mutation, expectedResult, token)
  }

  private def runTestRequest(prefix: String, mutationPath: String, expectedResultPath: String, token: OAuth2BearerToken = validUserToken()) = {
    val mutation: String = fromResource(prefix + mutationPath).mkString
    val expectedResult: String = fromResource(prefix + expectedResultPath).mkString
    Post("/graphql").withEntity(ContentTypes.`application/json`, mutation) ~> addCredentials(token) ~> route ~> check {
      print(responseAs[String])
      responseAs[String] shouldEqual expectedResult
    }
  }

  "The api" should "create a consignment if the correct information is provided" in {
    runTestMutation("mutation_alldata.json", "data_all.json")
  }

  "The api" should "throw an error if the series id field isn't provided" in {
    runTestMutation("mutation_missingseriesid.json", "data_seriesid_missing.json")
  }

  "The api" should "throw an error if the user id field isn't provided" in {
    runTestMutation("mutation_missinguserid.json", "data_userid_missing.json")
  }

  "The api" should "allow an tdr_admin user to create a consignment" in {
    runTestMutation("mutation_alldata.json", "data_all.json", validAdminToken)
  }

  "The api" should "allow a tdr_user user to create a consignment" in {
    runTestMutation("mutation_alldata.json", "data_all.json")
  }
}
