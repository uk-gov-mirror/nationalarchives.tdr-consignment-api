package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.mockito.ArgumentMatchers._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadataValues}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers  {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFileMetadata" should "call the metadata repository with the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val mockMetadataResponse = Future.successful(Seq(FilemetadataRow(UUID.randomUUID(), Some(fixedFileUuid), Some(fixedPropertyId), Some("value"), Timestamp.from(FixedTimeSource.now), fixedUserId)))
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()
    val metadataId: UUID = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val captor: ArgumentCaptor[Seq[FilemetadataRow]] = ArgumentCaptor.forClass(classOf[Seq[FilemetadataRow]])
    when(metadataRepositoryMock.addFileMetadata(captor.capture())).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput("PropertyName", List(FileMetadataValues(fixedFileUuid, "value"))), Some(fixedUserId)).await()


    captor.getValue.nonEmpty shouldBe(true)
    val row = captor.getValue.head
    row.propertyid.get should equal(fixedPropertyId)
    row.fileid.get should equal(fixedFileUuid)
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
    val mockMetadataResponse = Future.successful(Seq(FilemetadataRow(UUID.randomUUID(), Some(fixedFileUuid), Some(fixedPropertyId), Some(value), dummyTimestamp, fixedUserId)))
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()
    val propertyName = "PropertyName"

    when(metadataRepositoryMock.addFileMetadata(any[Seq[FilemetadataRow]])).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, FixedTimeSource, fixedUUIDSource)
    val result: Seq[FileMetadataFields.FileMetadata] = service.addFileMetadata(AddFileMetadataInput(propertyName, List(FileMetadataValues(fixedFileUuid, "value"))), Some(fixedUserId)).await()

    result.length should equal(1)
    val response = result(0)
    response.fileId should equal(fixedFileUuid)
    response.filePropertyName should equal(propertyName)
    response.value should equal(value)
  }

  "addFileMetadata" should "return the correct data for multiple updates" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val secondFixedFileUuid = UUID.fromString("de3b388d-e1ec-41cf-a729-a1bf29113075")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val value = "value"
    val anotherValue = "anotherValue"
    val dummyTimestamp = Timestamp.from(Instant.now())
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val mockMetadataResponse = Future.successful(Seq(
      FilemetadataRow(UUID.randomUUID(), Some(fixedFileUuid), Some(fixedPropertyId), Some(value), dummyTimestamp, fixedUserId),
      FilemetadataRow(UUID.randomUUID(), Some(secondFixedFileUuid), Some(fixedPropertyId), Some(anotherValue), dummyTimestamp, fixedUserId)
    ))

    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()
    val propertyName = "PropertyName"

    val captor: ArgumentCaptor[Seq[FilemetadataRow]] = ArgumentCaptor.forClass(classOf[Seq[FilemetadataRow]])
    when(metadataRepositoryMock.addFileMetadata(captor.capture())).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, FixedTimeSource, fixedUUIDSource)
    val result: Seq[FileMetadataFields.FileMetadata] = service.addFileMetadata(AddFileMetadataInput(propertyName, List(FileMetadataValues(fixedFileUuid, value), FileMetadataValues(secondFixedFileUuid, anotherValue))), Some(fixedUserId)).await()

    result.length should equal(2)
    val firstResponse = result(0)
    val secondResponse = result(1)

    firstResponse.fileId should equal(fixedFileUuid)
    secondResponse.fileId should equal(secondFixedFileUuid)

    firstResponse.filePropertyName should equal(propertyName)
    secondResponse.filePropertyName should equal(propertyName)

    firstResponse.value should equal(value)
    secondResponse.value should equal(anotherValue)

    captor.getValue.length should equal(2)
  }

  "getFileProperty" should "return a file property" in {
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)
    val service = new FileMetadataService(mock[FileMetadataRepository], propertyRepositoryMock, FixedTimeSource, new FixedUUIDSource)
    val property = service.getFileProperty("Name").await()
    property.isDefined should equal(true)
    property.get.propertyid should equal(fixedPropertyId)
    property.get.name.get should equal("Name")
    property.get.shortname.get should equal("ShortName")
    property.get.description.get should equal("Description")
  }


}
