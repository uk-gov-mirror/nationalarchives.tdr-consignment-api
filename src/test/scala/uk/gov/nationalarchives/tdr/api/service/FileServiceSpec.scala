package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.FileRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, Files}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class FileServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createFile" should "create a file given correct arguments" in {
    val fixedUuidSource = new FixedUUIDSource()
    val uuid = UUID.randomUUID()
    val fileId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(List(FileRow(fileId, consignmentId, uuid, Timestamp.from(Instant.now))))
    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource, fixedUuidSource)
    val result: Files = fileService.addFile(AddFilesInput(consignmentId, 1),Some(uuid)).await()

    result.fileIds shouldBe List(fileId)
  }

  "createFile" should "create three files given correct arguments" in {
    val fixedUuidSource = new FixedUUIDSource()
    val fileUuidOne = fixedUuidSource.uuid
    val fileUuidTwo = fixedUuidSource.uuid
    val fileUuidThree = fixedUuidSource.uuid
    fixedUuidSource.reset
    val consignmentUuid = UUID.randomUUID()
    val userUuid = UUID.randomUUID()

    val fileRepositoryMock = mock[FileRepository]
    val fileRowOne = FileRow(fileUuidOne, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val fileRowTwo = FileRow(fileUuidTwo, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val fileRowThree = FileRow(fileUuidThree, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val expectedArgs = List(fileRowOne, fileRowTwo, fileRowThree)
    val captor: ArgumentCaptor[List[FileRow]] = ArgumentCaptor.forClass(classOf[List[FileRow]])
    val mockResponse = Future.successful(List(fileRowOne, fileRowTwo, fileRowThree))

    when(fileRepositoryMock.addFiles(captor.capture())).thenReturn(mockResponse)
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource, fixedUuidSource)
    val result: Files = fileService.addFile(AddFilesInput(consignmentUuid, 3),Some(userUuid)).await()

    captor.getAllValues.size should equal(1)
    captor.getAllValues.get(0).length should equal(3)
    captor.getAllValues.get(0).forall(expectedArgs.contains) should be(true)

    result.fileIds shouldBe List(fileUuidOne, fileUuidTwo, fileUuidThree)
  }

  "createFile" should "link a file to the correct user and consignment" in {
    val fixedUuidSource = new FixedUUIDSource()
    val userId = UUID.randomUUID()
    val fileUuid = fixedUuidSource.uuid
    val consignmentUuid = UUID.randomUUID()
    val fileRepositoryMock = mock[FileRepository]
    fixedUuidSource.reset
    val fileService = new FileService(fileRepositoryMock, FixedTimeSource, fixedUuidSource)

    val expectedRow = List(FileRow(fileUuid, consignmentUuid, userId, Timestamp.from(FixedTimeSource.now)))
    val captor: ArgumentCaptor[List[FileRow]] = ArgumentCaptor.forClass(classOf[List[FileRow]])
    val mockResponse = Future.successful(List(FileRow(fileUuid, consignmentUuid, userId, Timestamp.from(FixedTimeSource.now))))
    when(fileRepositoryMock.addFiles(captor.capture())).thenReturn(mockResponse)

    fileService.addFile(AddFilesInput(consignmentUuid, 1),Some(userId)).await()

    verify(fileRepositoryMock).addFiles(expectedRow)
    captor.getAllValues.size should equal(1)
    captor.getAllValues.get(0).head.consignmentid should equal(consignmentUuid)
    captor.getAllValues.get(0).head.userid should equal(userId)
  }
}
