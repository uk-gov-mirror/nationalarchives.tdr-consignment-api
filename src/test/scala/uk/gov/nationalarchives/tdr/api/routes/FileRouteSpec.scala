package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{PreparedStatement, ResultSet}

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils.{GraphqlError, getDataFromFile, validUserToken}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._

class FileRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach {
  private val addFileJsonFilePrefix: String = "json/addfile_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  override def beforeEach(): Unit = {
    val connection = DbConnection.db.source.createConnection()
    connection.prepareStatement("delete from consignmentapi.File").executeUpdate()
    val resetConsignmentIdCount = "alter table consignmentapi.Consignment alter column ConsignmentId restart with 1"
    val resetFileIdCount = "alter table consignmentapi.File alter column FileId restart with 1"
    connection.prepareStatement(resetConsignmentIdCount).executeUpdate()
    connection.prepareStatement(resetFileIdCount).executeUpdate()
    connection.close()
  }

  case class GraphqlMutationData(data: Option[AddFiles], errors: List[GraphqlError] = Nil)
  case class File(fileIds: List[Long])
  case class AddFiles(addFiles: File)

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData = runTestRequest[GraphqlMutationData](addFileJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData = getDataFromFile[GraphqlMutationData](addFileJsonFilePrefix)

  "The api" should "add one file if the correct information is provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_one_file", validUserToken())

    response.data.isDefined should equal(true)
    response.data.get.addFiles should equal(expectedResponse.data.get.addFiles)
    response.data.get.addFiles.fileIds.foreach(checkFileExists)
  }

  "The api" should "add three files if the correct information is provided" in {
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

  "The api" should "throw an error if the user does not own the consignment" in {
    val sql = "insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'5ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_error_not_owner")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)

  }

  private def checkFileExists(fileId: Long) = {
    val sql = s"select * from consignmentapi.File where FileId = $fileId;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ConsignmentId") should equal(fileId.toString)
  }



}
