package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{PreparedStatement, ResultSet}
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class AntivirusMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {

  private val addAVMetadataJsonFilePrefix: String = "json/addavmetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  val defaultFileId = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  case class GraphqlMutationData(data: Option[AddAntivirusMetadata], errors: List[GraphqlError] = Nil)
  case class AntivirusMetadata(
                                fileId: UUID,
                                software: Option[String] = None,
                                value: Option[String] = None,
                                softwareVersion: Option[String] = None,
                                databaseVersion: Option[String] = None,
                                result: Option[String] = None,
                                datetime: Long
                              )
  case class AddAntivirusMetadata(addAntivirusMetadata: List[AntivirusMetadata]) extends TestRequest

  override def beforeEach(): Unit = {
    resetDatabase()
  }

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addAVMetadataJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addAVMetadataJsonFilePrefix)

  "addAntivirusMetadata" should "return all requested fields from inserted anti-virus metadata object" in {
    seedDatabaseWithDefaultEntries()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validBackendChecksToken())
    response.data.get.addAntivirusMetadata should equal(expectedResponse.data.get.addAntivirusMetadata)

    checkAntivirusMetadataExists(response.data.get.addAntivirusMetadata.head.fileId)
  }

  "addAntivirusMetadata" should "return the expected data from inserted anti-virus metadata object" in {
    seedDatabaseWithDefaultEntries()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validBackendChecksToken())
    response.data.get.addAntivirusMetadata should equal(expectedResponse.data.get.addAntivirusMetadata)

    checkAntivirusMetadataExists(response.data.get.addAntivirusMetadata.head.fileId)
  }

  "addAntivirusMetadata" should "not allow updating of anti-virus metadata with incorrect authorisation" in {
    seedDatabaseWithDefaultEntries()

    val response: GraphqlMutationData = runTestMutation("mutation_somedata", invalidBackendChecksToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
    checkNoAntivirusMetadataAdded()
  }

  "addAntivirusMetadata" should "throw an error if the field file id is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileid", validBackendChecksToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoAntivirusMetadataAdded()
  }

  "addAntivirusMetadata" should "throw an error if the field datetime is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_datetime_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingdatetime", validBackendChecksToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoAntivirusMetadataAdded()
  }

  private def seedDatabaseWithDefaultEntries(): Unit = {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)
  }

  private def createConsignment(consignmentId: UUID, userId: UUID): Unit = {
    val sql = s"insert into Consignment (ConsignmentId, SeriesId, UserId) VALUES ('$consignmentId', 1, '$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
  }

  private def createFile(fileId: UUID, consignmentId: UUID): Unit = {
    val sql = s"insert into File (FileId, ConsignmentId) VALUES ('$fileId', '$consignmentId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
  }

  private def checkAntivirusMetadataExists(fileId: UUID): Unit = {
    val sql = "select * from AVMetadata where FileId = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }

  private def checkNoAntivirusMetadataAdded(): Unit = {
    val sql = "select * from AVMetadata;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    rs.getRow should equal(0)
  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from AVMetadata").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from File").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from Consignment").executeUpdate()
  }
}
