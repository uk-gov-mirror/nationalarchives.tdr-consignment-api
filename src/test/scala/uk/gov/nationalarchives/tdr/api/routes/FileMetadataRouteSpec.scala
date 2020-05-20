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
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.FileMetadata
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.{GraphqlError, getDataFromFile, validBackendChecksToken}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class FileMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addFileMetadataJsonFilePrefix: String = "json/addfilemetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  val defaultFileId: UUID = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  case class GraphqlMutationData(data: Option[AddFileMetadata], errors: List[GraphqlError] = Nil)
  case class AddFileMetadata(addFileMetadata: List[FileMetadata])

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

    checkFileMetadataExists(response.data.get.addFileMetadata.head.fileId)
  }

  "addFileMetadata" should "add multiple metadata entries for multiple files" in {
    createFile(UUID.fromString("208bcd34-294e-47c2-8b3d-22bf4ab1af68"), UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506"))

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_multiple")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata_multiple", validBackendChecksToken("checksum"))
    response.data.get.addFileMetadata should equal(expectedResponse.data.get.addFileMetadata)

    checkFileMetadataExists(response.data.get.addFileMetadata.head.fileId)
    checkFileMetadataExists(response.data.get.addFileMetadata(1).fileId)

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

  "addFileMetadata" should "throw an error if one file id does not exist and one does" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_one_fileid_not_exists")
    val response: GraphqlMutationData = runTestMutation("mutation_onefileidnotexists", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }

  "addFileMetadata" should "throw an error if the file property does not exist" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_incorrect_property")
    val response: GraphqlMutationData = runTestMutation("mutation_incorrectproperty", validBackendChecksToken("checksum"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoFileMetadataAdded()
  }



  private def checkFileMetadataExists(fileId: UUID): Unit = {
    val sql = "select * from FileMetadata where FileId = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }

  private def createFileProperty = {
    val sql = "insert into FileProperty (PropertyId , Name, Description, Shortname) " +
      "VALUES (?, 'ServerSideChecksum', 'The checksum calculated after upload', 'Checksum')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, UUID.randomUUID().toString)
    ps.executeUpdate()
  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from FileMetadata").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from FileProperty").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from File").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from Consignment").executeUpdate()
  }

  private def checkNoFileMetadataAdded(): Unit = {
    val sql = "select * from FileMetadata;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    rs.getRow should equal(0)
  }
}
