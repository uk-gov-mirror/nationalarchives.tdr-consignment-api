package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{PreparedStatement, ResultSet}

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class ClientFileMetadataRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {

  private val addClientFileMetadataJsonFilePrefix: String = "json/addclientfilemetadata_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  case class GraphqlMutationData(data: Option[AddClientFileMetadata], errors: List[GraphqlError] = Nil)
  case class ClientFileMetadata(
                                fileId: Option[Long] = None,
                                originalPath: Option[String] = None,
                                checksum: Option[String] = None,
                                checksumType: Option[String] = None,
                                lastModified: Option[Long] = None,
                                createdDate: Option[Long] = None,
                                fileSize: Option[BigDecimal] = None,
                                datetime: Option[Long] = None,
                                clientFileMetadataId: Option[Long] = None
                              )
  case class AddClientFileMetadata(addClientFileMetadata: ClientFileMetadata) extends TestRequest

  override def beforeEach(): Unit = {
    resetDatabase()
  }

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addClientFileMetadataJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addClientFileMetadataJsonFilePrefix)

  "The api" should "return all requested fields from inserted Client File metadata object" in {

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addClientFileMetadata should equal(expectedResponse.data.get.addClientFileMetadata)

    checkClientFileMetadataExists(response.data.get.addClientFileMetadata.clientFileMetadataId.get)
  }

  "The api" should "return the expected data from inserted Client File metadata object" in {

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())
    response.data.get.addClientFileMetadata should equal(expectedResponse.data.get.addClientFileMetadata)

    checkClientFileMetadataExists(response.data.get.addClientFileMetadata.clientFileMetadataId.get)
  }

  "The api" should "throw an error if the file id field is not provided" in {

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_fileid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingfileid", validUserToken())

    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  private def checkClientFileMetadataExists(clientFileMetadataId: Long): Unit = {
    val sql = s"select * from consignmentapi.ClientFileMetadata where ClientFileMetadataId = $clientFileMetadataId;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ClientFileMetadataId") should equal(clientFileMetadataId.toString)
  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.ClientFileMetadata").executeUpdate()

    val resetClientFileMetadataIdCount =
      "alter table consignmentapi.ClientFileMetadata alter column ClientFileMetadataId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetClientFileMetadataIdCount).executeUpdate()
  }
}
