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
    val seriesId = UUID.fromString("1436ad43-73a2-4489-a774-85fa95daff32")

    TestUtils.createConsignment(consignmentId, seriesId, userId)

    consignmentRepository.addParentFolder(consignmentId, Option("TEST ADD PARENT FOLDER NAME")).futureValue

    val parentFolderName = consignmentRepository.getConsignment(consignmentId).futureValue.map(consignment => consignment.parentfolder)

    parentFolderName should contain only Some("TEST ADD PARENT FOLDER NAME")
  }

  "getParentFolder" should "get parent folder name for a consignment" in {
    val db = DbConnection.db
    val consignmentRepository = new ConsignmentRepository(db)
    val consignmentId = UUID.fromString("b6da7577-3800-4ebc-821b-9d33e52def9e")
    val seriesId = UUID.fromString("1436ad43-73a2-4489-a774-85fa95daff32")

    TestUtils.createConsignment(consignmentId, seriesId, userId)
    consignmentRepository.addParentFolder(consignmentId, Option("TEST GET PARENT FOLDER NAME"))

    val parentFolderName = consignmentRepository.getParentFolder(consignmentId).futureValue

    parentFolderName should be(Some("TEST GET PARENT FOLDER NAME"))
  }

  "getParentFolder" should "return nothing if no parent folder exists" in {
    val db = DbConnection.db
    val consignmentRepository = new ConsignmentRepository(db)
    val consignmentId = UUID.fromString("8233b9a4-5c2d-4c2d-9355-e6ec5751fea5")
    val seriesId = UUID.fromString("1436ad43-73a2-4489-a774-85fa95daff32")

    TestUtils.createConsignment(consignmentId, seriesId, userId)

    val parentFolderName = consignmentRepository.getParentFolder(consignmentId).futureValue

    parentFolderName should be(None)
  }

  "getSeriesAndBodyOfConsignment" should "get the series name and transferring body name for a consignment" in {
    val db = DbConnection.db
    val consignmentRepository = new ConsignmentRepository(db)
    val consignmentId = UUID.fromString("b59a8bfd-5709-46c7-a5e9-71bae146e2f1")
    val seriesId = UUID.fromString("1436ad43-73a2-4489-a774-85fa95daff32")
    val bodyId = UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")
    val seriesName = "Mock series"
    val bodyName = "Body"

    TestUtils.addSeries(seriesId, bodyId, seriesName)
    TestUtils.createConsignment(consignmentId, seriesId, userId)

    val consignmentAndSeriesAndBody = consignmentRepository.getSeriesAndBodyOfConsignment(consignmentId).futureValue.head

    consignmentAndSeriesAndBody.series.name.get should be(seriesName)
    consignmentAndSeriesAndBody.body.name.get should be(bodyName)
  }
}
