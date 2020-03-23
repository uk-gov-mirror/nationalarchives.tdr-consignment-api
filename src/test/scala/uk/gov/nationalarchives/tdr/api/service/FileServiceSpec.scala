package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.FileRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, File}
import uk.gov.nationalarchives.tdr.api.utils.FixedTimeSource
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class FileServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createFile" should "create a file given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(List(FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1))))
    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(AddFilesInput(consignmentId, Some(1)),Some(uuid)).await()

    result.fileIds shouldBe List(1)
  }

  "createFile" should "create one file with an empty number of files" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(List(FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1))))
    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(AddFilesInput(consignmentId, Option.empty),Some(uuid)).await()

    result.fileIds shouldBe List(1)
  }

  "createFile" should "create three files given correct arguments" in {
    val uuid = UUID.randomUUID()
    val consignmentId = 1
    val fileRepositoryMock = mock[FileRepository]
    val fileRowOne = FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(1))
    val fileRowTwo = FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(2))
    val fileRowThree = FileRow(consignmentId, uuid.toString, Timestamp.from(Instant.now), Some(3))
    val mockResponse = Future.successful(List(fileRowOne, fileRowTwo, fileRowThree))

    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)
    val result: File = fileService.addFile(AddFilesInput(consignmentId, Some(3)),Some(uuid)).await()

    result.fileIds shouldBe List(1,2,3)
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val userId = UUID.randomUUID()
    val consignmentId = 123
    val fileRepositoryMock = mock[FileRepository]
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource)

    val expectedRow = List(FileRow(consignmentId, userId.toString, Timestamp.from(FixedTimeSource.now), None))
    val mockResponse = Future.successful(List(FileRow(consignmentId, userId.toString, Timestamp.from(Instant.now), Some(1))))
    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockResponse)

    fileService.addFile(AddFilesInput(consignmentId, Some(1)),Some(userId)).await()

    verify(fileRepositoryMock).addFiles(expectedRow)

  }
}
