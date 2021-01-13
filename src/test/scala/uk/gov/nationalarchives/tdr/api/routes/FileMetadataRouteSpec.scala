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
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{FileMetadata, SHA256ServerSideChecksum}
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.{GraphqlError, getDataFromFile, validBackendChecksToken, _}

class FileMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addFileMetadataJsonFilePrefix: String = "json/addfilemetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  val defaultFileId: UUID = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  case class GraphqlMutationData(data: Option[AddFileMetadata], errors: List[GraphqlError] = Nil)
  case class AddFileMetadata(addFileMetadata: FileMetadata)

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addFileMetadataJsonFilePrefix)

  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addFileMetadataJsonFilePrefix)

  override def beforeEach(): Unit = {
    resetDatabase()
    seedDatabaseWithDefaultEntries()
    createFileProperty
  }

  "addFileMetadata" should "return all requested fields from inserted checksum file metadata object" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validBackendChecksToken("checksum"))
    response.data.get.addFileMetadata should equal(expectedResponse.data.get.addFileMetadata)

    checkFileMetadataExists(response.data.get.addFileMetadata.fileId)
  }

  "addFileMetadata" should "not allow updating of file metadata with incorrect authorisation" in {
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", invalidBackendChecksToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "not allow updating of file metadata with incorrect client role" in {
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validBackendChecksToken("antivirus"))

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the field file property name is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileproperty_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileproperty", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the field file id is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileid", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the value is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_value_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingvalue", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the file id does not exist" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_not_exists")
    val response: GraphqlMutationData = runTestMutation("mutation_fileidnotexists", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the file property does not exist" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_incorrect_property")
    val response: GraphqlMutationData = runTestMutation("mutation_incorrectproperty", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "add the checksum validation result if this is a checksum update" in {
    createClientFileMetadata(defaultFileId)
    runTestMutation("mutation_alldata", validBackendChecksToken("checksum"))
    checkValidationResultExists(defaultFileId)
  }

  "addFileMetadata" should "not add the checksum validation result if this is not a checksum update" in {
    runTestMutation("mutation_notchecksum", validBackendChecksToken("checksum"))
    checkNoValidationResultExists(defaultFileId)
  }

  private def checkFileMetadataExists(fileId: UUID): Unit = {
    val sql = "SELECT * FROM FileMetadata WHERE FileId = ? AND PropertyName = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    ps.setString(2, SHA256ServerSideChecksum)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }

  private def createFileProperty = {
    val sql = "INSERT INTO FileProperty (Name, Description, Shortname) " +
      "VALUES ('SHA256ServerSideChecksum', 'The checksum calculated after upload', 'Checksum')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("DELETE FROM FileMetadata").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("DELETE FROM FileProperty").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("DELETE FROM FFIDMetadata").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("DELETE FROM File").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("DELETE FROM Consignment").executeUpdate()
  }

  private def checkNoFileMetadataAdded(): Unit = {
    val sql = "select * from FileMetadata WHERE PropertyName = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, SHA256ServerSideChecksum)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    rs.getRow should equal(0)
  }

  private def checkValidationResultExists(fileId: UUID): Unit = {
    val sql = "select ChecksumMatches from File where FileId = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    Option(rs.getObject(1)).isDefined should be(true)
  }

  private def checkNoValidationResultExists(fileId: UUID): Unit = {
    val sql = "select ChecksumMatches from File where FileId = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    Option(rs.getObject(1)).isEmpty should be(true)
  }
}
