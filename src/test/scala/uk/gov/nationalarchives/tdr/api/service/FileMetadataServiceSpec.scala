package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.ClientFileMetadata
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.AddFileMetadataInput
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFileMetadata" should "call the metadata repository with the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, "value", Timestamp.from(FixedTimeSource.now), fixedUserId)
    )
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()
    val metadataId: UUID = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val captor: ArgumentCaptor[FilemetadataRow] = ArgumentCaptor.forClass(classOf[FilemetadataRow])
    when(metadataRepositoryMock.addFileMetadata(captor.capture())).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput("PropertyName", fixedFileUuid, "value"), Some(fixedUserId)).futureValue


    val row = captor.getValue
    row.propertyid should equal(fixedPropertyId)
    row.fileid should equal(fixedFileUuid)
    row.userid should equal(fixedUserId)
    row.datetime should equal(Timestamp.from(FixedTimeSource.now))
    row.metadataid.shouldBe(metadataId)
  }

  "addFileMetadata" should "return the correct data for a single update" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val value = "value"
    val dummyTimestamp = Timestamp.from(Instant.now())
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, value, dummyTimestamp, fixedUserId)
    )
    val propertyName = "PropertyName"
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some(propertyName), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()


    when(metadataRepositoryMock.addFileMetadata(any[FilemetadataRow])).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    val result: FileMetadataFields.FileMetadata =
      service.addFileMetadata(AddFileMetadataInput(propertyName, fixedFileUuid, "value"), Some(fixedUserId)).futureValue

    result.fileId should equal(fixedFileUuid)
    result.filePropertyName should equal(propertyName)
    result.value should equal(value)
  }

  "addFileMetadata" should "add the checksum validation result if the update is for a checksum" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val filePropertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val fileId = UUID.randomUUID()
    val propertyId = UUID.randomUUID()
    val mockClientFileMetadata = ClientFileMetadata(fileId, Option.empty, Some("checksum"), Some("Mock"), 1, 1, Option.empty, 1, UUID.randomUUID)
    val mockFilePropertyRow = Some(FilepropertyRow(propertyId, Some("SHA256ServerSideChecksum"), Some("Description"), Some("ShortName")))
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fileId, propertyId, "value", Timestamp.from(FixedTimeSource.now), UUID.randomUUID())
    )

    when(clientFileMetadataServiceMock.getClientFileMetadata(any[UUID])).thenReturn(Future(mockClientFileMetadata))
    when(filePropertyRepositoryMock.getPropertyByName(any[String])).thenReturn(Future(mockFilePropertyRow))
    when(fileMetadataRepositoryMock.addFileMetadata(any[FilemetadataRow])).thenReturn(mockMetadataResponse)

    val service = new FileMetadataService(fileMetadataRepositoryMock, filePropertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource())
    service.addFileMetadata(AddFileMetadataInput("SHA256ServerSideChecksum", fileId, "checksum"), Some(UUID.randomUUID())).futureValue
    verify(fileMetadataRepositoryMock).addChecksumValidationResult(any[UUID], any[Option[Boolean]])
  }

  "addFileMetadata" should "not add the checksum validation result if the update is not for a checksum" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val filePropertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val fileId = UUID.randomUUID()
    val propertyId = UUID.randomUUID()
    val mockClientFileMetadata = ClientFileMetadata(fileId, Option.empty, Some("checksum"), Some("Mock"), 1, 1, Option.empty, 1, UUID.randomUUID)
    val mockFilePropertyRow = Some(FilepropertyRow(propertyId, Some("SomethingElse"), Some("Description"), Some("ShortName")))
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fileId, propertyId, "value", Timestamp.from(FixedTimeSource.now), UUID.randomUUID())
    )

    when(clientFileMetadataServiceMock.getClientFileMetadata(any[UUID])).thenReturn(Future(mockClientFileMetadata))
    when(filePropertyRepositoryMock.getPropertyByName(any[String])).thenReturn(Future(mockFilePropertyRow))
    when(fileMetadataRepositoryMock.addFileMetadata(any[FilemetadataRow])).thenReturn(mockMetadataResponse)

    val service = new FileMetadataService(fileMetadataRepositoryMock, filePropertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource())
    service.addFileMetadata(AddFileMetadataInput("SomethingElse", fileId, "checksum"), Some(UUID.randomUUID())).futureValue
    verify(fileMetadataRepositoryMock, times(0)).addChecksumValidationResult(any[UUID], any[Option[Boolean]])
  }

  "getFileProperty" should "return a file property" in {
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val filePropertyRow = Some(FilepropertyRow(fixedPropertyId, Some("SHA256ServerSideChecksum"), Some("Description"), Some("ShortName")))
    val mockPropertyResponse = Future.successful(filePropertyRow)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)
    val service = new FileMetadataService(mock[FileMetadataRepository], propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource)
    val property = service.getFileProperty("SHA256ServerSideChecksum").futureValue
    property.isDefined should equal(true)
    property.get.propertyid should equal(fixedPropertyId)
    property.get.name.get should equal("SHA256ServerSideChecksum")
    property.get.shortname.get should equal("ShortName")
    property.get.description.get should equal("Description")
  }

  "addChecksumValidationResult" should "update the row with the correct value if the checksums match" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockChecksum = "c3d410191fbc6727f350905e974238f585deb752c9fbff27192109d26685a8df"
    val service = new FileMetadataService(fileMetadataRepositoryMock, mock[FilePropertyRepository], clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource)
    val fileId = UUID.randomUUID()
    val mockClientFileMetadata = ClientFileMetadata(fileId, Option.empty, Some(mockChecksum), Some("Mock"), 1, 1, Option.empty, 1, UUID.randomUUID)

    val fileIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
    val checksumCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])

    when(clientFileMetadataServiceMock.getClientFileMetadata(fileId)).thenReturn(Future(mockClientFileMetadata))
    when(fileMetadataRepositoryMock.addChecksumValidationResult(fileIdCaptor.capture(), checksumCaptor.capture())).thenReturn(Future(1))

    service.addChecksumValidationResult(AddFileMetadataInput("", fileId, mockChecksum)).futureValue

    fileIdCaptor.getValue should equal(fileId)
    checksumCaptor.getValue should equal(Some(true))
  }

  "addChecksumValidationResult" should "update the row with the correct value if the checksums don't match" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockChecksum = "70933a42ff5990c08e73bf1e97149a5df9825449cca0f55de15862463357493b"
    val mockMismatchedChecksum = "c3d410191fbc6727f350905e974238f585deb752c9fbff27192109d26685a8df"
    val service = new FileMetadataService(fileMetadataRepositoryMock, mock[FilePropertyRepository], clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource)
    val fileId = UUID.randomUUID()
    val mockClientFileMetadata = ClientFileMetadata(fileId, Option.empty, Some(mockMismatchedChecksum), Some("Mock"), 1, 1, Option.empty, 1, UUID.randomUUID)

    val fileIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
    val checksumCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])

    when(clientFileMetadataServiceMock.getClientFileMetadata(fileId)).thenReturn(Future(mockClientFileMetadata))
    when(fileMetadataRepositoryMock.addChecksumValidationResult(fileIdCaptor.capture(), checksumCaptor.capture())).thenReturn(Future(1))

    service.addChecksumValidationResult(AddFileMetadataInput("", fileId, mockChecksum)).futureValue

    fileIdCaptor.getValue should equal(fileId)
    checksumCaptor.getValue should equal(Some(false))
  }
}
