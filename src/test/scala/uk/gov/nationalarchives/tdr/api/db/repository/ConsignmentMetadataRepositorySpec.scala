package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository.ConsignmentMetadataRowWithName
import uk.gov.nationalarchives.tdr.api.utils.TestUtils
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.ExecutionContext

class ConsignmentMetadataRepositorySpec extends AnyFlatSpec with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addConsignmentMetadata" should "add consignment metadata with the correct values" in {
    val db = DbConnection.db
    val consignmentMetadataRepository = new ConsignmentMetadataRepository(db)
    val propertyId = UUID.randomUUID()
    val consignmentId = UUID.fromString("d4c053c5-f83a-4547-aefe-878d496bc5d2")
    addConsignmentProperty(propertyId.toString, "ConsignmentProperty")
    createConsignment(consignmentId, userId)
    val input = Seq(ConsignmentMetadataRowWithName(
      "ConsignmentProperty", UUID.randomUUID(), Some(consignmentId), Some("value"), Timestamp.from(Instant.now()), UUID.randomUUID()))
    val result = consignmentMetadataRepository.addConsignmentMetadata(input).futureValue.head
    result.propertyName should equal("ConsignmentProperty")
    result.value should equal(Some("value"))
  }

  "getConsignmentMetadata" should "return the correct metadata" in {
    val db = DbConnection.db
    val consignmentMetadataRepository = new ConsignmentMetadataRepository(db)
    val propertyId = UUID.randomUUID()
    val consignmentId = UUID.fromString("d511ecee-89ac-4643-b62d-76a41984a92b")
    addConsignmentProperty(propertyId.toString, "ConsignmentProperty")
    addConsignmentMetadata(UUID.randomUUID().toString, consignmentId.toString, propertyId.toString)
    createConsignment(consignmentId, userId)
    val response = consignmentMetadataRepository.getConsignmentMetadata(consignmentId, "ConsignmentProperty").futureValue.head
    response.value should equal(Some("Result of ConsignmentMetadata processing"))
    response.propertyName should equal("ConsignmentProperty")
    response.consignmentid should equal(Some(consignmentId))
  }
}
