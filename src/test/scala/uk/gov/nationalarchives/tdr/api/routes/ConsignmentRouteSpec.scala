package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class ConsignmentRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addConsignmentJsonFilePrefix: String = "json/addconsignment_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Consignment").executeUpdate()
    val resetIdCount = "alter table consignmentapi.Consignment alter column ConsignmentId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetIdCount).executeUpdate()
  }

  case class GraphqlError(message: String, locations: List[Locations])
  case class GraphqlMutationData(data: Option[AddConsignment], errors: List[GraphqlError] = Nil)
  case class Consignment(consignmentid: Option[Long] = None, userid: Option[Long] = None, seriesid: Option[Long] = None)
  case class AddConsignment(addConsignment: Consignment) extends TestRequest

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData = runTestRequest[GraphqlMutationData](addConsignmentJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData = getDataFromFile[GraphqlMutationData](addConsignmentJsonFilePrefix)

  "The api" should "create a consignment if the correct information is provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validAdminToken)
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)
  }

  "The api" should "throw an error if the series id field isn't provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_seriesid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingseriesid", validAdminToken)
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "throw an error if the user id field isn't provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_userid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missinguserid", validAdminToken)
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "allow a tdr_admin user to create a consignment" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validAdminToken)
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)
  }

  "The api" should "allow a tdr_user user to create a consignment" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)
  }
}
