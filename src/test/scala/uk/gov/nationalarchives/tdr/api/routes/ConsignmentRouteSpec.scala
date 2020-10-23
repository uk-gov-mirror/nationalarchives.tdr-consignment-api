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
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.tdr.api.utils.{FixedUUIDSource, TestRequest}

class ConsignmentRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addConsignmentJsonFilePrefix: String = "json/addconsignment_"
  private val getConsignmentJsonFilePrefix: String = "json/getconsignment_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  override def beforeEach(): Unit = {
    val conn: Connection = DbConnection.db.source.createConnection()
    conn.prepareStatement("delete from Consignment").executeUpdate()
    conn.prepareStatement("delete from Series").executeUpdate()
    conn.commit()
    conn.close()
  }

  case class GraphqlQueryData(data: Option[GetConsignment], errors: List[GraphqlError] = Nil)
  case class GraphqlMutationData(data: Option[AddConsignment], errors: List[GraphqlError] = Nil)
  case class Consignment(consignmentid: Option[UUID] = None,
                         userid: Option[UUID] = None,
                         seriesid: Option[UUID] = None,
                         totalFiles: Option[Int],
                         fileChecks: Option[FileChecks],
                         parentFolder: Option[String]
                        )
  case class FileChecks(antivirusProgress: Option[AntivirusProgress], checksumProgress: Option[ChecksumProgress], ffidProgress: Option[FfidProgress])
  case class AntivirusProgress(filesProcessed: Option[Int])
  case class ChecksumProgress(filesProcessed: Option[Int])
  case class FfidProgress(filesProcessed: Option[Int])
  case class GetConsignment(getConsignment: Option[Consignment])
  case class AddConsignment(addConsignment: Consignment)

  val runTestQuery: (String, OAuth2BearerToken) => GraphqlQueryData = runTestRequest[GraphqlQueryData](getConsignmentJsonFilePrefix)
  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData = runTestRequest[GraphqlMutationData](addConsignmentJsonFilePrefix)
  val expectedQueryResponse: String => GraphqlQueryData = getDataFromFile[GraphqlQueryData](getConsignmentJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData = getDataFromFile[GraphqlMutationData](addConsignmentJsonFilePrefix)

  "addConsignment" should "create a consignment if the correct information is provided" in {
    createSeries(UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e"))

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)

    checkConsignmentExists(response.data.get.addConsignment.consignmentid.get)
  }

  "addConsignment" should "throw an error if the series id field isn't provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_seriesid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingseriesid", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "addConsignment" should "link a new consignment to the creating user" in {
    createSeries(UUID.fromString(("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")))

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addConsignment should equal(expectedResponse.data.get.addConsignment)

    response.data.get.addConsignment.userid should contain(userId)
  }

  "addConsignment" should "not allow a user to link a consignment to a series from another transferring body" in {
    createSeries(UUID.fromString(("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")))

    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken(body = "some-other-transferring-body"))

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
  }

  "getConsignment" should "return all requested fields" in {
    val sql = "insert into Consignment (ConsignmentId, SeriesId, UserId) VALUES (?, ?, ?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val consignmentId = "b130e097-2edc-4e67-a7e9-5364a09ae9cb"
    val seriesId = "fde450c9-09aa-4ba8-b0df-13f9bac1e587"
    ps.setString(1, consignmentId)
    ps.setString(2, seriesId)
    ps.setString(3, userId.toString)
    ps.executeUpdate()
    val fileOneId = "e7ba59c9-5b8b-4029-9f27-2d03957463ad"
    val fileTwoId = "42910a85-85c3-40c3-888f-32f697bfadb6"
    val fileThreeId = "9757f402-ee1a-43a2-ae2a-81a9ea9729b9"

    createFile(UUID.fromString(fileOneId), UUID.fromString(consignmentId))
    createFile(UUID.fromString(fileTwoId), UUID.fromString(consignmentId))
    createFile(UUID.fromString(fileThreeId), UUID.fromString(consignmentId))

    addParentFolderName(UUID.fromString(consignmentId), "ALL CONSIGNMENT DATA PARENT FOLDER")

    addAntivirusMetadata(fileOneId)

    val propertyId = "f62d1f66-db67-4a25-ac6f-b1ded92767b2"
    addFileProperty(propertyId, SHA256ServerSideChecksum)
    addFileMetadata("06209e0d-95d0-4f13-8933-e5b9d00eb435", fileOneId, propertyId)
    addFileMetadata("c4759aae-dc68-45ec-aee1-5a562c7b42cc", fileTwoId, propertyId)

    addFFIDMetadata(fileOneId)
    addFFIDMetadata(fileTwoId)
    addFFIDMetadata(fileThreeId)

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_all")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())

    response should equal(expectedResponse)
  }

  "getConsignment" should "return the expected data" in {
    val fixedUuidSource = new FixedUUIDSource()
    val sql = "insert into Consignment (ConsignmentId, SeriesId, UserId) VALUES (?,?,?)"
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

  "getConsignment" should "not allow a user to get a consignment that they did not create" in {
    val otherUserId = "73abd1dc-294d-4068-b60d-c1cd4782d08d"
    val sql = s"insert into Consignment (SeriesId, UserId) VALUES (1, '$otherUserId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val response: GraphqlQueryData = runTestQuery("query_somedata", validUserToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
  }

  "getConsignment" should "return an error if a user queries without a consignment id argument" in {
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_error_no_consignmentid")
    val response: GraphqlQueryData = runTestQuery("query_no_consignmentid", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)
  }

  private def checkConsignmentExists(consignmentId: UUID): Unit = {
    val sql = s"select * from Consignment where ConsignmentId = ?"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, consignmentId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ConsignmentId") should equal(consignmentId.toString)
  }

  private def createSeries(bodyId: UUID): Unit = {
    val sql = "insert into Series (SeriesId, BodyId) VALUES (?,?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, "6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")
    ps.setString(2, bodyId.toString)
    ps.executeUpdate()
  }
}
