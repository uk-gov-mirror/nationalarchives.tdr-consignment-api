package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import org.mockito.ArgumentMatchers._

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createConsignment" should "create a consignment given correct arguments" in {
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    val mockResponse = Future.successful(ConsignmentRow(1L, 1L, Timestamp.from(Instant.now), Some(1)))
    when(consignmentRepositoryMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)
    val consignmentService = new ConsignmentService(consignmentRepositoryMock)
    val result: Consignment = consignmentService.addConsignment(AddConsignmentInput(1 ,1)).await()
    result.seriesid shouldBe 1
    result.userid shouldBe 1
    result.consignmentid shouldBe defined
    result.consignmentid.get shouldBe 1
  }
}
