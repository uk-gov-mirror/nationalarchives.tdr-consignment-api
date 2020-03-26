package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}
import uk.gov.nationalarchives.tdr.api.utils.FixedTimeSource
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createConsignment" should "create a consignment given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    val mockResponse = Future.successful(ConsignmentRow(1L, uuid.toString, Timestamp.from(Instant.now), Some(1)))
    when(consignmentRepositoryMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)
    val consignmentService = new ConsignmentService(consignmentRepositoryMock, FixedTimeSource)
    val result: Consignment = consignmentService.addConsignment(AddConsignmentInput(1), Some(uuid)).await()
    result.seriesid shouldBe 1
    result.userid shouldBe uuid
    result.consignmentid shouldBe defined
    result.consignmentid.get shouldBe 1
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val userId = UUID.randomUUID()
    val seriesId = 123
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    val consignmentService = new ConsignmentService(consignmentRepositoryMock, FixedTimeSource)

    val expectedRow = ConsignmentRow(seriesId, userId.toString, Timestamp.from(FixedTimeSource.now), None)
    val mockResponse = Future.successful(ConsignmentRow(seriesId, userId.toString, Timestamp.from(Instant.now), Some(1)))
    when(consignmentRepositoryMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)

    consignmentService.addConsignment(AddConsignmentInput(seriesId), Some(userId)).await()

    verify(consignmentRepositoryMock).addConsignment(expectedRow)
  }

  "getConsignment" should "return the specfic Consignment for the requested consignment id" in {
    val userUuid = UUID.randomUUID()
    val consignmentRow = ConsignmentRow(1L, userUuid.toString, Timestamp.from(Instant.now), Some(1))
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq(consignmentRow))
    val consignmentRepoMock = mock[ConsignmentRepository]
    when(consignmentRepoMock.getConsignment(anyLong())).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock, FixedTimeSource)
    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(1).await()

    verify(consignmentRepoMock, times(1)).getConsignment(anyLong())
    val consignment: ConsignmentFields.Consignment = response.get
    consignment.consignmentid should equal(Some(1))
    consignment.seriesid should equal(1L)
    consignment.userid should equal(userUuid)
  }

  "getConsignment" should "return none when consignment id does not exist" in {
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq())
    val consignmentRepoMock = mock[ConsignmentRepository]
    when(consignmentRepoMock.getConsignment(anyLong())).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock, FixedTimeSource)
    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(1).await()
    verify(consignmentRepoMock, times(1)).getConsignment(anyLong())

    response should be(None)
  }
}
