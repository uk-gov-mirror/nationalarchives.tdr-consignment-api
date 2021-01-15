package uk.gov.nationalarchives.tdr.api.model.consignment

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class ConsignmentReferenceSpec extends AnyFlatSpec with ScalaFutures with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val testCases = List(ReferenceTestCase(2020, 5000, "TDR-2020-LBB"))
  testCases.foreach(testCase => {
    "createConsignmentReference" should s"generated consignment reference ${testCase.expectedReference} from " +
      s"year ${testCase.year} and sequence number ${testCase.sequenceNum}" in {

      val result: String = ConsignmentReference.createConsignmentReference(testCase.year, testCase.sequenceNum)

      result should equal (testCase.expectedReference)
    }
  })
}

case class ReferenceTestCase(year: Int, sequenceNum: Long, expectedReference: String)