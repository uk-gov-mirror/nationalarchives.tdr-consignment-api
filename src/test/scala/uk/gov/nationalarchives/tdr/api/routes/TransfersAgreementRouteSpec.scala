package uk.gov.nationalarchives.tdr.api.routes

import java.sql.{PreparedStatement, ResultSet}

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.generic.extras.Configuration
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestRequest
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import io.circe.generic.extras.auto._

class TransfersAgreementRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {

  private val addTransferAgreementJsonFilePrefix: String = "json/addtransferagreement_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  case class GraphqlMutationData(data: Option[AddTransferAgreement], errors: List[GraphqlError] = Nil)
  case class TransferAgreement(
                                consignmentid: Option[Long] = None,
                                allPublicRecords: Option[Boolean] = None,
                                allCrownCopyright: Option[Boolean] = None,
                                allEnglish: Option[Boolean] = None,
                                allDigital: Option[Boolean] = None,
                                appraisalSelectionSignedOff: Option[Boolean] = None,
                                sensitivityReviewSignedOff: Option[Boolean] = None,
                                transferAgreementId: Option[Long] = None
                              )
  case class AddTransferAgreement(addTransferAgreement: TransferAgreement) extends TestRequest

  override def beforeEach(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.TransferAgreement").executeUpdate()
    val resetIdCount = "alter table consignmentapi.TransferAgreement alter column TransferAgreementId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetIdCount).executeUpdate()
  }

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addTransferAgreementJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addTransferAgreementJsonFilePrefix)

  "The api" should "return all requested fields from inserted Transfer Agreement object" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addTransferAgreement should equal(expectedResponse.data.get.addTransferAgreement)

    checkTransferAgreementExists(response.data.get.addTransferAgreement.transferAgreementId.get)
  }

  "The api" should "return the expected data from inserted Transfer Agreement object" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())
    response.data.get.addTransferAgreement should equal(expectedResponse.data.get.addTransferAgreement)

    checkTransferAgreementExists(response.data.get.addTransferAgreement.transferAgreementId.get)
  }

  "The api" should "throw an error if the consignment id field isn't provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_consignmentid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingconsignmentid", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  private def checkTransferAgreementExists(transferAgreementId: Long): Unit = {
    val sql = s"select * from consignmentapi.TransferAgreement where TransferAgreementId = $transferAgreementId;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ConsignmentId") should equal(transferAgreementId.toString)
  }
}
