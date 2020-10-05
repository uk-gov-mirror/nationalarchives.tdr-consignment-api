package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
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

    consignmentRepository.addParentFolder(consignmentId, Option("TEST PARENT FOLDER NAME")).futureValue

    val parentFolderName = consignmentRepository.getConsignment(consignmentId).futureValue.map(consignment => consignment.parentfolder)

    parentFolderName should contain only Some("TEST PARENT FOLDER NAME")
  }
}
