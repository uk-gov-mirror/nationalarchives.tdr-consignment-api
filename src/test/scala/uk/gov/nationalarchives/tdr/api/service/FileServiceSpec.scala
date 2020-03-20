package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}
import uk.gov.nationalarchives.tdr.api.utils.FixedTimeSource

import scala.concurrent.Future

class FileServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  "createFile" should "create a file given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1)))
    when(fileRepositoryMock.addFile(any[FileRow])).thenReturn(mockResponse)
    val fileService = new ConsignmentService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(consignmentId).await()
    result.fileid shouldBe 1
    result.userid shouldBe uuid
    result.consignmentid.get shouldBe 1
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val userId = UUID.randomUUID()
    val consignmentId = 123
    val fileRepositoryMock = mock[FileRepository]
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)

    val expectedRow = FileRow(consignmentId, userId.toString, Timestamp.from(FixedTimeSource.now), None)
    val mockResponse = Future.successful(FileRow(consignmentId, userId.toString, Timestamp.from(Instant.now), Some(1)))
    when(fileRepositoryMock.addFile(any[FileRow])).thenReturn(mockResponse)

    fileService.addFile(consignmentId: Long, Some(userId)).await()

    verify(fileRepositoryMock).addFile(expectedRow)

  }
}
