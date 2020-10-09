package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{ConsignmentRow, FileRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, Files}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class FileServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "createFile" should "create a file given correct arguments" in {
    val fixedUuidSource = new FixedUUIDSource()
    val uuid = UUID.randomUUID()
    val fileId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()

    val fileRepositoryMock = mock[FileRepository]
    val consignmentRepositoryMock = mock[ConsignmentRepository]

    val mockFileResponse = Future.successful(List(FileRow(fileId, consignmentId, uuid, Timestamp.from(Instant.now))))
    when(fileRepositoryMock.addFiles(any[List[FileRow]])).thenReturn(mockFileResponse)
    val mockConsignmentResponse = Future.successful(())
    when(consignmentRepositoryMock.addParentFolder(consignmentId, Option("Parent folder name"))).thenReturn(mockConsignmentResponse)

    val fileService = new FileService(fileRepositoryMock, consignmentRepositoryMock, FixedTimeSource, fixedUuidSource)
    val result: Files = fileService.addFile(AddFilesInput(consignmentId, 1, Option("Parent folder name")), Option(uuid)).futureValue

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
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    val fileRowOne = FileRow(fileUuidOne, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val fileRowTwo = FileRow(fileUuidTwo, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val fileRowThree = FileRow(fileUuidThree, consignmentUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val expectedArgs = List(fileRowOne, fileRowTwo, fileRowThree)
    val captor: ArgumentCaptor[List[FileRow]] = ArgumentCaptor.forClass(classOf[List[FileRow]])

    val mockResponse = Future.successful(List(fileRowOne, fileRowTwo, fileRowThree))
    when(fileRepositoryMock.addFiles(captor.capture())).thenReturn(mockResponse)
    val mockConsignmentResponse = Future.successful(())
    when(consignmentRepositoryMock.addParentFolder(consignmentUuid, Option("Parent folder name"))).thenReturn(mockConsignmentResponse)

    val fileService = new FileService(fileRepositoryMock, consignmentRepositoryMock, FixedTimeSource, fixedUuidSource)
    val result: Files = fileService.addFile(AddFilesInput(consignmentUuid, 3, Option("Parent folder name")),Some(userUuid)).futureValue

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
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    fixedUuidSource.reset
    val fileService = new FileService(fileRepositoryMock, consignmentRepositoryMock, FixedTimeSource, fixedUuidSource)

    val expectedRow = List(FileRow(fileUuid, consignmentUuid, userId, Timestamp.from(FixedTimeSource.now)))
    val captor: ArgumentCaptor[List[FileRow]] = ArgumentCaptor.forClass(classOf[List[FileRow]])
    val mockResponse = Future.successful(List(FileRow(fileUuid, consignmentUuid, userId, Timestamp.from(FixedTimeSource.now))))
    when(fileRepositoryMock.addFiles(captor.capture())).thenReturn(mockResponse)
    val mockConsignmentResponse = Future.successful(())
    when(consignmentRepositoryMock.addParentFolder(consignmentUuid, Option("Parent folder name"))).thenReturn(mockConsignmentResponse)

    fileService.addFile(AddFilesInput(consignmentUuid, 1, Option("Parent folder name")),Some(userId)).futureValue

    verify(fileRepositoryMock).addFiles(expectedRow)
    captor.getAllValues.size should equal(1)
    captor.getAllValues.get(0).head.consignmentid should equal(consignmentUuid)
    captor.getAllValues.get(0).head.userid should equal(userId)
  }

  "getOwnersOfFiles" should "find the owners of the given files" in {
    val fixedUuidSource = new FixedUUIDSource()
    val fileId1 = UUID.fromString("bc609dc4-e153-4620-a7ab-20e7fd5a4005")
    val fileId2 = UUID.fromString("67178a08-36ea-41c2-83ee-4b343b6429cb")
    val userId1 = UUID.fromString("e9cac50f-c5eb-42b4-bb5d-355ccf8920cc")
    val userId2 = UUID.fromString("f4ffe1d0-3525-4a7c-ba0c-812f6e054ab1")
    val seriesId1 = UUID.fromString("bb503ea6-7207-42d7-9844-81471aa1b36a")
    val seriesId2 = UUID.fromString("74394d89-aa22-4170-b50e-3f5eefda7062")
    val consignmentId1 = UUID.fromString("0ae52efa-4f01-4b05-84f1-e36626180dad")
    val consignmentId2 = UUID.fromString("2e29cc1c-0a3e-40b2-b39d-f60bfea88abe")

    val fileRepositoryMock = mock[FileRepository]
    val consignmentRepositoryMock = mock[ConsignmentRepository]
    val fileService = new FileService(fileRepositoryMock, consignmentRepositoryMock, FixedTimeSource, fixedUuidSource)
    val consignment1 = ConsignmentRow(consignmentId1, seriesId1, userId1, Timestamp.from(Instant.now))
    val consignment2 = ConsignmentRow(consignmentId2, seriesId2, userId2, Timestamp.from(Instant.now))

    when(consignmentRepositoryMock.getConsignmentsOfFiles(Seq(fileId1)))
      .thenReturn(Future.successful(Seq((fileId1, consignment1), (fileId2, consignment2))))

    val owners = fileService.getOwnersOfFiles(Seq(fileId1)).futureValue

    owners should have size 2

    owners(0).userId should equal(userId1)
    owners(1).userId should equal(userId2)
  }
}
