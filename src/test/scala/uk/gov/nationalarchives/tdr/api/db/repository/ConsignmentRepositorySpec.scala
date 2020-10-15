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

    consignmentRepository.addParentFolder(consignmentId, Option("TEST ADD PARENT FOLDER NAME")).futureValue

    val parentFolderName = consignmentRepository.getConsignment(consignmentId).futureValue.map(consignment => consignment.parentfolder)

    parentFolderName should contain only Some("TEST ADD PARENT FOLDER NAME")
  }

  "getParentFolder" should "get parent folder name for a consignment" in {
    val db = DbConnection.db
    val consignmentRepository = new ConsignmentRepository(db)
    val consignmentId = UUID.fromString("b6da7577-3800-4ebc-821b-9d33e52def9e")

    TestUtils.createConsignment(consignmentId, userId)
    consignmentRepository.addParentFolder(consignmentId, Option("TEST GET PARENT FOLDER NAME"))

    val parentFolderName = consignmentRepository.getParentFolder(consignmentId).futureValue

    parentFolderName should be (Some("TEST GET PARENT FOLDER NAME"))
  }
}
