package uk.gov.nationalarchives.tdr.api.db.repository

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.{TestDatabase, TestUtils}

import java.util.UUID
import scala.concurrent.ExecutionContext

class ConsignmentStatusRepositorySpec extends AnyFlatSpec with TestDatabase with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "getConsignmentStatus" should "return all relevant consignment status rows" in {
    val db = DbConnection.db
    val consignmentStatusRepository = new ConsignmentStatusRepository(db)
    val consignmentId = UUID.fromString("b8271ba9-9ef4-4584-b074-5a48b2a34cec")
    val userId = UUID.fromString("aee2d1a9-e1db-43a0-9fd6-a6c342bb187b")

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createConsignmentUploadStatus(consignmentId)

    val consignmentUploadStatus = consignmentStatusRepository.getConsignmentStatus(consignmentId).futureValue.head

    consignmentUploadStatus.statustype should be("Upload")
    consignmentUploadStatus.value should be("InProgress")
  }

  "getConsignmentStatus" should "return an error should no consignment status exist for given consignment id" in {
    val db = DbConnection.db
    val consignmentStatusRepository = new ConsignmentStatusRepository(db)
    val consignmentId = UUID.fromString("b8271ba9-9ef4-4584-b074-5a48b2a34cec")
    val userId = UUID.fromString("aee2d1a9-e1db-43a0-9fd6-a6c342bb187b")

    TestUtils.createConsignment(consignmentId, userId)

    val consignmentUploadStatus = consignmentStatusRepository.getConsignmentStatus(consignmentId).futureValue

    consignmentUploadStatus should be(empty)
  }
}
