package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.Tables._
import uk.gov.nationalarchives.tdr.api.service.TransferAgreementService.transferAgreementProperties

import scala.concurrent.ExecutionContext

class ConsignmentMetadataRepositorySpec extends AnyFlatSpec with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private val consignmentMetadataProperty = "AllEnglishConfirmed"

  "addConsignmentMetadata" should "add consignment metadata with the correct values" in {
    val db = DbConnection.db
    val consignmentMetadataRepository = new ConsignmentMetadataRepository(db)
    val consignmentId = UUID.fromString("d4c053c5-f83a-4547-aefe-878d496bc5d2")
    createConsignment(consignmentId, userId)
    val input = Seq(ConsignmentmetadataRow(
      UUID.randomUUID(), consignmentId, consignmentMetadataProperty, "value", Timestamp.from(Instant.now()), UUID.randomUUID()))
    val result = consignmentMetadataRepository.addConsignmentMetadata(input).futureValue.head
    result.propertyname should equal(consignmentMetadataProperty)
    result.value should equal("value")
    checkMetadataAddedExists(consignmentId)
  }

  "getConsignmentMetadata" should "return the correct metadata" in {
    val db = DbConnection.db
    val consignmentMetadataRepository = new ConsignmentMetadataRepository(db)
    val consignmentId = UUID.fromString("d511ecee-89ac-4643-b62d-76a41984a92b")
    addConsignmentMetadata(UUID.randomUUID().toString, consignmentId.toString, consignmentMetadataProperty)
    createConsignment(consignmentId, userId)
    val response = consignmentMetadataRepository.getConsignmentMetadata(consignmentId, consignmentMetadataProperty).futureValue.head
    response.value should equal("Result of ConsignmentMetadata processing")
    response.propertyname should equal(consignmentMetadataProperty)
    response.consignmentid should equal(consignmentId)
  }

  private def checkMetadataAddedExists(consignmentId: UUID): Unit = {
    val sql = "SELECT * FROM ConsignmentMetadata WHERE ConsignmentId = ?"
    val ps: PreparedStatement = DbConnection.db.source.createConnection().prepareStatement(sql)
    ps.setString(1, consignmentId.toString)
    val rs: ResultSet = ps.executeQuery()
    rs.next()
    rs.getString("ConsignmentId") should equal(consignmentId.toString)
    rs.getString("PropertyName") should equal (consignmentMetadataProperty)
    rs.getString("Value") should equal ("value")
    rs.next() should equal (false)
  }
}
