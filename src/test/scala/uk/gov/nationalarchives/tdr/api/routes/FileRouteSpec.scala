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
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.{GraphqlError, getDataFromFile, userId, validUserToken}
import uk.gov.nationalarchives.tdr.api.utils.{FixedUUIDSource, TestRequest}

class FileRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {
  private val addFileJsonFilePrefix: String = "json/addfile_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  override def beforeEach(): Unit = {
    val connection = DbConnection.db.source.createConnection()
    connection.prepareStatement("delete from FileMetadata").executeUpdate()
    connection.prepareStatement("delete from FFIDMetadata").executeUpdate()
    connection.prepareStatement("delete from File").executeUpdate()
    connection.prepareStatement("delete from Consignment").executeUpdate()
    connection.close()
  }

  case class GraphqlMutationData(data: Option[AddFiles], errors: List[GraphqlError] = Nil)
  case class File(fileIds: Seq[UUID])
  case class AddFiles(addFiles: File)

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData = runTestRequest[GraphqlMutationData](addFileJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData = getDataFromFile[GraphqlMutationData](addFileJsonFilePrefix)

  val fixedUuidSource = new FixedUUIDSource()

  "The api" should "add one file if the correct information is provided" in {
    val sql = s"insert into Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_one_file")
    val response: GraphqlMutationData = runTestMutation("mutation_one_file", validUserToken())

    response.data.isDefined should equal(true)
    response.data.get.addFiles should equal(expectedResponse.data.get.addFiles)
    response.data.get.addFiles.fileIds.foreach(checkFileExists)
  }

  "The api" should "add three files if the correct information is provided" in {
    val sql = "insert into Consignment (SeriesId, UserId) VALUES (?,?)"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fixedUuidSource.uuid.toString)
    ps.setString(2, userId.toString)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())

    response.data.isDefined should equal(true)
    response.data.get.addFiles should equal(expectedResponse.data.get.addFiles)

    response.data.get.addFiles.fileIds.foreach(checkFileExists)
  }

  "The api" should "throw an error if the consignment id field is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_consignmentid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingconsignmentid", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "throw an error if the number of files field is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_numberoffiles_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingnumberoffiles", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "throw an error if the user does not own the consignment" in {
    val sql = "insert into Consignment (SeriesId, UserId) VALUES (1,'5ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_error_not_owner")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)
    response.errors.head.extensions.get.code should equal(expectedResponse.errors.head.extensions.get.code)
  }

  "The api" should "throw an error if the consignment already has had files uploaded" in {
    val sqlConsignment = s"insert into Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val psConsignment: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sqlConsignment)
    psConsignment.executeUpdate()
    //Seed DB with initial file for consignment
    runTestMutation("mutation_one_file", validUserToken())

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_error_previous_upload")
    val response: GraphqlMutationData = runTestMutation("mutation_one_file", validUserToken())
    response.errors.head.extensions should equal(expectedResponse.errors.head.extensions)
    response.errors.head.message should equal (expectedResponse.errors.head.message)
    checkNumberOfFiles(1)
  }

  private def checkFileExists(fileId: UUID) = {
    val sql = s"select * from File where FileId = ?"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, fileId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("FileId") should equal(fileId.toString)
  }

  private def checkNumberOfFiles(expectedNumberOfFiles: Int) = {
    val sql = s"select * from File"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.last()
    rs.getRow should equal(expectedNumberOfFiles)
  }
}
