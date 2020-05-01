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

class AVMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {

  private val addAVMetadataJsonFilePrefix: String = "json/addavmetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  val defaultFileId = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")

  case class GraphqlMutationData(data: Option[AddAVMetadata], errors: List[GraphqlError] = Nil)
  case class AVMetadata(
                                fileId: UUID,
                                software: Option[String] = None,
                                value: Option[String] = None,
                                softwareVersion: Option[String] = None,
                                databaseVersion: Option[String] = None,
                                result: Option[String] = None,
                                datetime: Long
                              )
  case class AddAVMetadata(addAVMetadata: List[AVMetadata]) extends TestRequest

  override def beforeEach(): Unit = {
    resetDatabase()
  }

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addAVMetadataJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addAVMetadataJsonFilePrefix)

  "addAVMetadata" should "return all requested fields from inserted anti-virus metadata object" in {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addAVMetadata should equal(expectedResponse.data.get.addAVMetadata)

    checkAVMetadataExists(response.data.get.addAVMetadata.head.fileId)
  }

  "addAVMetadata" should "return the expected data from inserted anti-virus metadata object" in {
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, userId)
    createFile(defaultFileId, consignmentId)

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())
    response.data.get.addAVMetadata should equal(expectedResponse.data.get.addAVMetadata)

    checkAVMetadataExists(response.data.get.addAVMetadata.head.fileId)
  }

  "addAVMetadata" should "not allow a user to add anti-virus metadata to a file they do not own" in {
    val otherUserId = UUID.fromString("29f65c4e-0eb8-4719-afdb-ace1bcbae4b6")
    val consignmentId = UUID.fromString("eb197bfb-43f7-40ca-9104-8f6cbda88506")
    createConsignment(consignmentId, otherUserId)
    createFile(defaultFileId, consignmentId)

    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())

    response.errors should have size 1
    response.errors.head.extensions.get.code should equal("NOT_AUTHORISED")
    checkNoAVMetadataAdded()
  }

  "addAVMetadata" should "not allow a user to add anti-virus metadata if they only own some of the files" in {
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
    checkNoAVMetadataAdded()
  }

  "addAVFileMetadata" should "throw an error if the file id field is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileid", validUserToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNoAVMetadataAdded()
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

  private def checkAVMetadataExists(fileId: UUID): Unit = {
    val sql = "select * from AVMetadata where FileId = ?;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }

  private def checkNoAVMetadataAdded(): Unit = {

  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from AVMetadata").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from File").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from Consignment").executeUpdate()
  }
}
