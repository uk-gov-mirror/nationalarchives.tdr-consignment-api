package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{PreparedStatement, ResultSet}
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.tdr.api.utils.{TestDatabase, TestRequest}

class ClientFileMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with TestDatabase  {

  private val addClientFileMetadataJsonFilePrefix: String = "json/addclientfilemetadata_"
  private val getClientFileMetadataJsonFilePrefix: String = "json/getclientfilemetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  val defaultFileId: UUID = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  case class GraphqlMutationData(data: Option[AddClientFileMetadata], errors: List[GraphqlError] = Nil)
  case class GraphqlQueryData(data: Option[GetClientFileMetadata], errors: List[GraphqlError] = Nil)
  case class ClientFileMetadata(
                                fileId: Option[UUID],
                                originalPath: Option[String] = None,
                                checksum: Option[String] = None,
                                checksumType: Option[String] = None,
                                lastModified: Option[Long] = None,
                                fileSize: Option[Long] = None
                              )
  case class AddClientFileMetadata(addClientFileMetadata: List[ClientFileMetadata]) extends TestRequest
  case class GetClientFileMetadata(getClientFileMetadata: ClientFileMetadata) extends TestRequest

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addClientFileMetadataJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addClientFileMetadataJsonFilePrefix)
  val runTestQuery: (String, OAuth2BearerToken) => GraphqlQueryData =
    runTestRequest[GraphqlQueryData](getClientFileMetadataJsonFilePrefix)
  val expectedQueryResponse: String => GraphqlQueryData =
    getDataFromFile[GraphqlQueryData](getClientFileMetadataJsonFilePrefix)

  "addClientFileMetadata" should "return all requested fields from inserted Client File metadata object" in {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addClientFileMetadata should equal(expectedResponse.data.get.addClientFileMetadata)

    checkClientFileMetadataExists(response.data.get.addClientFileMetadata.head.fileId.get)
  }

  "addClientFileMetadata" should "return the expected data from inserted Client File metadata object" in {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())
    response.data.get.addClientFileMetadata should equal(expectedResponse.data.get.addClientFileMetadata)

    checkClientFileMetadataExists(response.data.get.addClientFileMetadata.head.fileId.get)
  }

  "addClientFileMetadata" should "not allow a user to add metadata to a consignment they do not own" in {
    val otherUserId = UUID.fromString("29f65c4e-0eb8-4719-afdb-ace1bcbae4b6")
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, otherUserId)
    createFile(defaultFileId, consignmentId)

    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
  }

  "addClientFileMetadata" should "not allow a user to add metadata if they only own some of the files" in {
    val otherUserId = UUID.fromString("29f65c4e-0eb8-4719-afdb-ace1bcbae4b6")
    val otherUsersConsignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    val otherUsersFileId = UUID.fromString("dd7a6739-2843-496a-8f17-4a2cfbe297f6")
    createConsignment(otherUsersConsignmentId, otherUserId)
    createFile(otherUsersFileId, otherUsersConsignmentId)

    val thisUsersConsignmentId = UUID.fromString("a8b58ed6-6922-415a-93a0-5b5fe1171088")
    val thisUsersFileId = UUID.fromString("587b2a7c-a92e-430f-aa61-431117398e64")
    createConsignment(thisUsersConsignmentId, userId)
    createFile(thisUsersFileId, thisUsersConsignmentId)

    val response: GraphqlMutationData = runTestMutation("mutation_mixedfileownership", validUserToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
  }

  "addClientFileMetadata" should "throw an error if the file id field is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileid", validUserToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "getClientFileMetadata" should "return the requested fields" in {
    seedDatabaseWithDefaultEntries()
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_all")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validBackendChecksToken("client_file_metadata"))
    val responseData: ClientFileMetadata = response.data.get.getClientFileMetadata
    val expectedData = expectedResponse.data.get.getClientFileMetadata
    responseData.fileId should equal(expectedData.fileId)
    responseData.originalPath should equal(expectedData.originalPath)
    responseData.checksum should equal(expectedData.checksum)
    responseData.checksumType should equal(expectedData.checksumType)
    responseData.fileSize should equal(expectedData.fileSize)
  }

  "getClientFileMetadata" should "throw an error if the file id does not exist" in {
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_fileid_not_exists")
    val response: GraphqlQueryData = runTestQuery("query_fileidnotexists", validBackendChecksToken("client_file_metadata"))

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    response.errors.head.extensions.get.code should equal(expectedResponse.errors.head.extensions.get.code)
  }

  "getClientFileMetadata" should "throw an error if the user does not have the file format role" in {
    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_no_file_format_role")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    response.errors.head.extensions.get.code should equal(expectedResponse.errors.head.extensions.get.code)
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

  private def checkClientFileMetadataExists(fileId: UUID): Unit = {
    val sql = "SELECT * FROM FileMetadata WHERE FileId = ? AND PropertyName IN (?,?,?,?);"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    clientSideProperties.zipWithIndex.foreach {
      case (a, b) => ps.setString(b + 2, a)
    }
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }
}
