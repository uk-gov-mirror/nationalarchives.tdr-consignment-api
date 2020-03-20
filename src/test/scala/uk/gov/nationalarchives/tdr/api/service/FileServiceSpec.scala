package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.File
import uk.gov.nationalarchives.tdr.api.utils.FixedTimeSource

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

class FileServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createFile" should "create a file given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1)))
    when(fileRepositoryMock.addFile(any[FileRow])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(consignmentId, 1).await()

    result.fileid shouldBe 1
    result.consignmentid shouldBe 1
  }

  "createFile" should "create three files given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1)))
    when(fileRepositoryMock.addFile(any[FileRow])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(consignmentId, 3).await()

    result.fileid shouldBe 1
    result.consignmentid shouldBe 1
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val userId = UUID.randomUUID()
    val consignmentId = 123
    val fileRepositoryMock = mock[FileRepository]
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)

    val expectedRow = FileRow(consignmentId, userId.toString, Timestamp.from(FixedTimeSource.now), None)
    val mockResponse = Future.successful(FileRow(consignmentId, userId.toString, Timestamp.from(Instant.now), Some(1)))
    when(fileRepositoryMock.addFile(any[FileRow])).thenReturn(mockResponse)

    fileService.addFile(consignmentId: Long, 1).await()

    verify(fileRepositoryMock).addFile(expectedRow)

  }
}
