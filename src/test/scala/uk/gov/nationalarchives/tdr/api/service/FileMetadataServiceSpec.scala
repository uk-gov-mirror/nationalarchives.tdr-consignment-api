package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.db.repository.FileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields._
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}
import uk.gov.nationalarchives.Tables.FilemetadataRow

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFileMetadata" should "call the metadata repository with the correct row arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "value",
        Timestamp.from(FixedTimeSource.now), fixedUserId, SHA256ServerSideChecksum)
    )
    val fixedUUIDSource = new FixedUUIDSource()
    val metadataId: UUID = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val addChecksumCaptor: ArgumentCaptor[FilemetadataRow] = ArgumentCaptor.forClass(classOf[FilemetadataRow])
    val getFileMetadataFileCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
    val getFileMetadataPropertyNameCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(addChecksumCaptor.capture(), validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)
    when(metadataRepositoryMock.getFileMetadata(getFileMetadataFileCaptor.capture(), getFileMetadataPropertyNameCaptor.capture()))
      .thenReturn(Future(Seq()))
    val service = new FileMetadataService(metadataRepositoryMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "value"), fixedUserId).futureValue


    val row = addChecksumCaptor.getValue
    row.propertyname should equal(SHA256ServerSideChecksum)
    row.fileid should equal(fixedFileUuid)
    row.userid should equal(fixedUserId)
    row.datetime should equal(Timestamp.from(FixedTimeSource.now))
    row.metadataid.shouldBe(metadataId)
    getFileMetadataFileCaptor.getValue should equal(fixedFileUuid)
    getFileMetadataPropertyNameCaptor.getValue should equal(SHA256ClientSideChecksum)
  }

  "addFileMetadata" should "call the metadata repository with the correct arguments if the checksum matches" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val timestamp = Timestamp.from(FixedTimeSource.now)
    val mockClientChecksumRow = FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "checksum",
      timestamp, fixedUserId, SHA256ClientSideChecksum)
    val mockClientChecksumResponse = Future(Seq(mockClientChecksumRow))
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "value",
        Timestamp.from(FixedTimeSource.now), fixedUserId, SHA256ServerSideChecksum)
    )
    val fixedUUIDSource = new FixedUUIDSource()
    fixedUUIDSource.reset

    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)

    when(metadataRepositoryMock.getFileMetadata(fixedFileUuid, SHA256ClientSideChecksum)).thenReturn(mockClientChecksumResponse)
    val service = new FileMetadataService(metadataRepositoryMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "checksum"), fixedUserId).futureValue
    validationResultCaptor.getValue.get should be(true)
  }

  "addFileMetadata" should "call the metadata repository with the correct arguments if the checksum doesn't match" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "value",
        Timestamp.from(FixedTimeSource.now), fixedUserId, SHA256ServerSideChecksum)
    )
    val timestamp = Timestamp.from(FixedTimeSource.now)
    val mockClientChecksumRow = FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "checksum",
      timestamp, fixedUserId, SHA256ClientSideChecksum)
    val mockClientChecksumResponse = Future(Seq(mockClientChecksumRow))

    val fixedUUIDSource = new FixedUUIDSource()
    fixedUUIDSource.reset

    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)
    when(metadataRepositoryMock.getFileMetadata(fixedFileUuid, SHA256ClientSideChecksum)).thenReturn(mockClientChecksumResponse)

    val service = new FileMetadataService(metadataRepositoryMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "anotherchecksum"), fixedUserId).futureValue
    validationResultCaptor.getValue.get should be(false)
  }

  "addFileMetadata" should "return the correct data for a single update" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val value = "value"
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "value",
        Timestamp.from(FixedTimeSource.now), fixedUserId, SHA256ServerSideChecksum)
    )
    val propertyName = SHA256ServerSideChecksum
    val fixedUUIDSource = new FixedUUIDSource()
    val timestamp = Timestamp.from(FixedTimeSource.now)
    val mockClientChecksumRow = FilemetadataRow(UUID.randomUUID(), fixedFileUuid, "checksum",
      timestamp, fixedUserId, SHA256ClientSideChecksum)
    val mockClientChecksumResponse = Future(Seq(mockClientChecksumRow))


    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], any[Option[Boolean]])).thenReturn(mockMetadataResponse)
    when(metadataRepositoryMock.getFileMetadata(fixedFileUuid, SHA256ClientSideChecksum)).thenReturn(mockClientChecksumResponse)

    val service = new FileMetadataService(metadataRepositoryMock, FixedTimeSource, fixedUUIDSource)
    val result: FileMetadata =
      service.addFileMetadata(AddFileMetadataInput(propertyName, fixedFileUuid, "value"), fixedUserId).futureValue

    result.fileId should equal(fixedFileUuid)
    result.filePropertyName should equal(propertyName)
    result.value should equal(value)
  }

  "addFileMetadata" should "fail if the update is not for a checksum" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileId = UUID.randomUUID()

    val service = new FileMetadataService(fileMetadataRepositoryMock, FixedTimeSource, new FixedUUIDSource())
    val err = service.addFileMetadata(AddFileMetadataInput("SomethingElse", fileId, "checksum"), UUID.randomUUID()).failed.futureValue
    err.getMessage should equal("SomethingElse found. We are only expecting checksum updates for now")
  }

  "getFileMetadata" should "return the correct metadata" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]

    val consignmentId = UUID.randomUUID()
    val fileId = UUID.randomUUID()
    val timestamp = Timestamp.from(FixedTimeSource.now)

    val rows = Seq(
      metadataRow(fileId, "ClientSideFileLastModifiedDate", timestamp.toString),
      metadataRow(fileId, "SHA256ClientSideChecksum", "checksum"),
      metadataRow(fileId, "ClientSideOriginalFilepath", "filePath"),
      metadataRow(fileId, "ClientSideFileSize", "1"),
      metadataRow(fileId, "RightsCopyright", "rightsCopyright"),
      metadataRow(fileId, "LegalStatus", "legalStatus"),
      metadataRow(fileId, "HeldBy", "heldBy"),
      metadataRow(fileId, "Language", "language"),
      metadataRow(fileId, "FoiExemptionCode", "foiExemption")
    )
    when(fileMetadataRepositoryMock.getFileMetadata(consignmentId)).thenReturn(Future(rows))

    val service = new FileMetadataService(fileMetadataRepositoryMock, FixedTimeSource, new FixedUUIDSource())
    val metadataList = service.getFileMetadata(consignmentId).futureValue
    metadataList.length should equal(1)
    val metadata = metadataList.head
    metadata.clientSideFileSize.get should equal(1)
    metadata.clientSideLastModifiedDate.get should equal(timestamp.toLocalDateTime)
    metadata.clientSideOriginalFilePath.get should equal("filePath")
    metadata.foiExemptionCode.get should equal("foiExemption")
    metadata.heldBy.get should equal("heldBy")
    metadata.language.get should equal("language")
    metadata.legalStatus.get should equal("legalStatus")
    metadata.rightsCopyright.get should equal("rightsCopyright")
    metadata.sha256ClientSideChecksum.get should equal("checksum")
  }

  "getFileMetadata" should "return empty fields if the metadata has an unexpected property name" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]

    val consignmentId = UUID.randomUUID()
    val fileId = UUID.randomUUID()

    val rowOne = metadataRow(fileId, "customPropertyNameOne", "customValueOne")
    val rowTwo = metadataRow(fileId, "customPropertyNameTwo", "customValueTwo")
    val rows = Seq(rowOne, rowTwo)
    when(fileMetadataRepositoryMock.getFileMetadata(consignmentId)).thenReturn(Future(rows))

    val service = new FileMetadataService(fileMetadataRepositoryMock, FixedTimeSource, new FixedUUIDSource())
    val metadataList = service.getFileMetadata(consignmentId).futureValue
    metadataList.length should equal(1)
    val metadata = metadataList.head
    val empty = Option.empty
    val expected = FileMetadataValues(fileId, empty, empty, empty, empty, empty, empty, empty, empty, empty)
    metadata should equal(expected)
  }

  private def metadataRow(fileId: UUID, propertyName: String, value: String): FilemetadataRow =
    FilemetadataRow(UUID.randomUUID(), fileId, value, Timestamp.from(Instant.now()), UUID.randomUUID(), propertyName)
}
