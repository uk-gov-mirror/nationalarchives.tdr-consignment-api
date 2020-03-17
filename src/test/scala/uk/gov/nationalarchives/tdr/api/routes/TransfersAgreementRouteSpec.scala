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

class TransfersAgreementRouteSpec extends AnyFlatSpec with Matchers with TestRequest with BeforeAndAfterEach  {

  private val addTransferAgreementJsonFilePrefix: String = "json/addtransferagreement_"
  private val getTransferAgreementJsonFilePrefix: String = "json/gettransferagreement_"

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  case class GraphqlMutationData(data: Option[AddTransferAgreement], errors: List[GraphqlError] = Nil)
  case class GraphqlQueryData(transferAgreement: Option[TransferAgreement], errors: List[GraphqlError] = Nil)
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
    resetDatabase()
  }

  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](addTransferAgreementJsonFilePrefix)
  val runTestQuery: (String, OAuth2BearerToken) => GraphqlQueryData =
    runTestRequest[GraphqlQueryData](getTransferAgreementJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](addTransferAgreementJsonFilePrefix)
  val expectedQueryResponse: String => GraphqlQueryData =
    getDataFromFile[GraphqlQueryData](getTransferAgreementJsonFilePrefix)

  "The api" should "return all requested fields from inserted Transfer Agreement object" in {
    val sql = "insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'4ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_all")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.data.get.addTransferAgreement should equal(expectedResponse.data.get.addTransferAgreement)

    checkTransferAgreementExists(response.data.get.addTransferAgreement.transferAgreementId.get)
  }

  "The api" should "return the expected data from inserted Transfer Agreement object" in {
    val sql = "insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'4ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_some")
    val response: GraphqlMutationData = runTestMutation("mutation_somedata", validUserToken())
    response.data.get.addTransferAgreement should equal(expectedResponse.data.get.addTransferAgreement)

    checkTransferAgreementExists(response.data.get.addTransferAgreement.transferAgreementId.get)
  }

  "The api" should "throw an error if the consignment id field is not provided" in {
    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_consignmentid_missing")
    val response: GraphqlMutationData = runTestMutation("mutation_missingconsignmentid", validUserToken())
    response.errors.head.message should equal (expectedResponse.errors.head.message)
  }

  "The api" should "return an error if a user does not own the transfer agreement's consignment id" in {
    val sql = "insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'5ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_error_not_owner")
    val response: GraphqlMutationData = runTestMutation("mutation_alldata", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)
  }

  "The api" should "return an error if an invalid consignment id is provided" in {
    val sql = "insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'4ab14990-ed63-4615-8336-56fbb9960300')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()

    val expectedResponse: GraphqlMutationData = expectedMutationResponse("data_error_invalid_consignmentid")
    val response: GraphqlMutationData = runTestMutation("mutation_invalid_consignmentid", validUserToken())
    response.errors.head.message should equal(expectedResponse.errors.head.message)
  }

  "The api" should "return an existing transfer agreement for a user owned consignment" in {
    val consignentSql = s"insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val sql = "INSERT INTO consignmentapi.TransferAgreement (ConsignmentId, AllPublicRecords, AllCrownCopyright, " +
      "AllEnglish, AllDigital, AppraisalSelectionSignedOff, SensitivityReviewSignedOff, TransferAgreementId) " +
      "VALUES (1, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE);"
    val psConsignemnt: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(consignentSql)
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.executeUpdate()
    psConsignemnt.executeUpdate()

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_all")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())
    response.transferAgreement should equal(expectedResponse)
  }

  "The api" should "return no transfer agreement if it doesn't exist" in {
    val consignentSql = s"insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(consignentSql)
    ps.executeUpdate()
    ps.executeUpdate()

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_none")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())
    response.transferAgreement should equal(expectedResponse)
  }

  "The api" should "return an error if the consignment id isn't provided" in {
    val consignentSql = s"insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(consignentSql)
    ps.executeUpdate()
    ps.executeUpdate()

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_consignmentidmissing")
    val response: GraphqlQueryData = runTestQuery("query_missingconsignmentid", validUserToken())
    response.transferAgreement should equal(expectedResponse)
  }

  "The api" should "return an error if the user doesn't own the consignment" in {
    val consignentSql = s"insert into consignmentapi.Consignment (SeriesId, UserId) VALUES (1,'$userId')"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(consignentSql)
    ps.executeUpdate()
    ps.executeUpdate()

    val expectedResponse: GraphqlQueryData = expectedQueryResponse("data_invalidconsignment")
    val response: GraphqlQueryData = runTestQuery("query_alldata", validUserToken())
    response.transferAgreement should equal(expectedResponse)
  }

  private def checkTransferAgreementExists(transferAgreementId: Long): Unit = {
    val sql = s"select * from consignmentapi.TransferAgreement where TransferAgreementId = $transferAgreementId;"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("TransferAgreementId") should equal(transferAgreementId.toString)
  }

  private def resetDatabase(): Unit = {
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.TransferAgreement").executeUpdate()
    DbConnection.db.source.createConnection().prepareStatement("delete from consignmentapi.Consignment").executeUpdate()

    val resetTransferAgreementIdCount = "alter table consignmentapi.TransferAgreement alter column TransferAgreementId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetTransferAgreementIdCount).executeUpdate()

    val resetConsignmentIdCount = "alter table consignmentapi.Consignment alter column ConsignmentId restart with 1"
    DbConnection.db.source.createConnection().prepareStatement(resetConsignmentIdCount).executeUpdate()
  }
}
