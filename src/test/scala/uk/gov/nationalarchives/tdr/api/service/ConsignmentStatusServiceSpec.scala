package uk.gov.nationalarchives.tdr.api.service

import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ConsignmentstatusRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentStatusRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.CurrentStatus
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import java.sql.Timestamp
import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ConsignmentStatusServiceSpec extends AnyFlatSpec with MockitoSugar with ResetMocksAfterEachTest with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val consignmentStatusRepositoryMock = mock[ConsignmentStatusRepository]
  val consignmentService = new ConsignmentStatusService(consignmentStatusRepositoryMock)

  "getConsignmentStatus" should "turn repository response into current status object" in {
    val fixedUUIDSource = new FixedUUIDSource()
    val consignmentStatusId = fixedUUIDSource.uuid
    val consignmentId = fixedUUIDSource.uuid
    val consignmentStatusRow = ConsignmentstatusRow(
      consignmentStatusId,
      consignmentId,
      "Upload",
      "InProgress",
      Timestamp.from(FixedTimeSource.now)
    )

    val mockRepoResponse: Future[Seq[ConsignmentstatusRow]] = Future.successful(Seq(consignmentStatusRow))
    when(consignmentStatusRepositoryMock.getConsignmentStatus(consignmentId)).thenReturn(mockRepoResponse)

    val response: CurrentStatus = consignmentService.getConsignmentStatus(consignmentId).futureValue

    response.upload should be(Some("InProgress"))
  }

  "getConsignmentStatus" should "return an empty CurrentStatus object if a consignment status doesn't exist for given consignment" in {
    val fixedUUIDSource = new FixedUUIDSource()
    val consignmentId = fixedUUIDSource.uuid
    val mockRepoResponse = Future.successful(Seq())
    when(consignmentStatusRepositoryMock.getConsignmentStatus(consignmentId)).thenReturn(mockRepoResponse)

    val response = consignmentService.getConsignmentStatus(consignmentId).futureValue

    response should be(CurrentStatus(None))
  }

  "getConsignmentStatus" should "return the newest status when more than one is available" in {
    val consignmentId = UUID.randomUUID()
    val newTime = Timestamp.from(FixedTimeSource.now)
    val olderTime = Timestamp.from(FixedTimeSource.now.minus(Duration.ofDays(1)))
    val oldestTime = Timestamp.from(FixedTimeSource.now.minus(Duration.ofDays(2)))
    val mockedResponse = Future {
      Seq(
        ConsignmentstatusRow(UUID.randomUUID(), consignmentId, "Upload", "InProgressOlder", olderTime),
        ConsignmentstatusRow(UUID.randomUUID(), consignmentId, "Upload", "InProgressNew", newTime),
        ConsignmentstatusRow(UUID.randomUUID(), consignmentId, "Upload", "InProgressOldest", oldestTime)
      )
    }

    when(consignmentStatusRepositoryMock.getConsignmentStatus(consignmentId)).thenReturn(mockedResponse)

    val response = consignmentService.getConsignmentStatus(consignmentId).futureValue

    response.upload.get should equal("InProgressNew")
  }
}
