package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.{FixedUUIDSource, TestRequest}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class ConsignmentRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addConsignmentJsonFilePrefix: String = "json/addconsignment_"
  private val getConsignmentJsonFilePrefix: String = "json/getconsignment_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  override def beforeEach(): Unit = {
    val conn: Connection = DbConnection.db.source.createConnection()
    conn.prepareStatement("delete from consignmentapi.Consignment").executeUpdate()
    conn.commit()
    conn.close()
  }


  case class GraphqlQueryData(data: Option[GetConsignment], errors: List[GraphqlError] = Nil)
  case class GraphqlMutationData(data: Option[AddConsignment], errors: List[GraphqlError] = Nil)
  case class Consignment(consignmentid: Option[UUID] = None, userid: Option[UUID] = None, seriesid: Option[UUID] = None)
  case class GetConsignment(getConsignment: Option[Consignment]) extends TestRequest
  case class AddConsignment(addConsignment: Consignment) extends TestRequest

  val runTestQuery: (String, OAuth2BearerToken) => GraphqlQueryData = runTestRequest[GraphqlQueryData](getConsignmentJsonFilePrefix)
  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData = runTestRequest[GraphqlMutationData](addConsignmentJsonFilePrefix)
  val expectedQueryResponse: String => GraphqlQueryData = getDataFromFile[GraphqlQueryData](getConsignmentJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData = getDataFromFile[GraphqlMutationData](addConsignmentJsonFilePrefix)

  val fixedUuidSource = new FixedUUIDSource()

  "The api" should "create a consignment if the correct information is provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)

    checkConsignmentExists(response.data.get.addConsignment.consignmentid.get)
  }

  "The api" should "throw an error if the series id field isn't provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_seriesid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingseriesid", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "link a new consignment to the creating user" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)

    response.data.get.addConsignment.userid should contain(userId)
  }

  "The api" should "return all requested fields" in {
    val sql = "insert into consignmentapi.Consignment (ConsignmentId, SeriesId, UserId) VALUES (?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val uuid = fixedUuidSource.uuid.toString
    ps.setString(1, uuid)
    ps.setString(2, uuid)
    ps.setString(3, userId.toString)
    ps.executeUpdate()
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_all")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())
    response.data should equal(expectedResponse.data)
  }

  "The api" should "return the expected data" in {
    val sql = "insert into consignmentapi.Consignment (ConsignmentId, SeriesId, UserId) VALUES (?,?,?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val uuid = fixedUuidSource.uuid.toString
    ps.setString(1, uuid)
    ps.setString(2, uuid)
    ps.setString(3, userId.toString)
    ps.executeUpdate()

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_some")
    val response: GraphqlQueryData = runTestQuery("query_somedata", validUserToken())
    response.data should equal(expectedResponse.data)
  }

  "The api" should "return an error if a user queries without a consignment id argument" in {
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_error_no_consignmentid")
    val response: GraphqlQueryData = runTestQuery("query_no_consignmentid", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)
  }

  private def checkConsignmentExists(consignmentId: UUID): Unit = {
    val sql = s"select * from consignmentapi.Consignment where ConsignmentId = ?"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, consignmentId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ConsignmentId") should equal(consignmentId.toString)
  }
}
