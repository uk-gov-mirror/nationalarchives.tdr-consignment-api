package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.DbConnection
import uk.gov.nationalarchives.tdr.api.utils.TestUtils
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.ExecutionContext

class ConsignmentRepositorySpec extends AnyFlatSpec with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addParentFolder" should "add parent folder name to an existing consignment row" in {
    val db = DbConnection.db
    val consignmentRepository = new ConsignmentRepository(db)
    val consignmentId = UUID.fromString("0292019d-d112-465b-b31e-72dfb4d1254d")

    TestUtils.createConsignment(consignmentId, userId)
    TestUtils.createFile(UUID.fromString("813231b3-2fbd-4869-abdf-0c42624c4d57"), consignmentId)
    TestUtils.createFile(UUID.fromString("a235f269-026f-4fb2-a9de-966b49b2dba6"), consignmentId)
    TestUtils.createFile(UUID.fromString("dac49dad-6e7e-4876-a9a6-0e151e13a136"), consignmentId)

    consignmentRepository.addParentFolder(consignmentId, Option("TEST PARENT FOLDER NAME")).futureValue

    val parentFolderName = consignmentRepository.getConsignment(consignmentId).futureValue.map(consignment => consignment.parentfolder)

    parentFolderName should contain only Some("TEST PARENT FOLDER NAME")
  }
}
