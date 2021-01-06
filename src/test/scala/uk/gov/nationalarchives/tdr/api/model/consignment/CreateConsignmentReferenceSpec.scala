package uk.gov.nationalarchives.tdr.api.model.consignment

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class CreateConsignmentReferenceSpec extends AnyFlatSpec with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createConsignmentReference" should "create a full consignment reference when given a numerical ID" in {
    val consignmentReferenceClass = new CreateConsignmentReference
    val createdYear = 2020
    val consignmentSequence = 5000

    val result: String = consignmentReferenceClass.createConsignmentReference(createdYear, consignmentSequence)

    result should include ("LBB")
    result should equal ("TDR-2020-LBB")
  }
}
