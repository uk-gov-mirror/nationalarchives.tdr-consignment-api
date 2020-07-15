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

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, FixedTimeSource, fixedUUIDSource)
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
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, value, dummyTimestamp, fixedUserId)
    )
    val propertyName = "PropertyName"
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some(propertyName), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()


    when(metadataRepositoryMock.addFileMetadata(any[FilemetadataRow])).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, FixedTimeSource, fixedUUIDSource)
    val result: FileMetadataFields.FileMetadata =
      service.addFileMetadata(AddFileMetadataInput(propertyName, fixedFileUuid, "value"), Some(fixedUserId)).futureValue

    result.fileId should equal(fixedFileUuid)
    result.filePropertyName should equal(propertyName)
    result.value should equal(value)
  }

  "getFileProperty" should "return a file property" in {
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some("Name"), Some("Description"), Some("ShortName"))))
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)
    val service = new FileMetadataService(mock[FileMetadataRepository], propertyRepositoryMock, FixedTimeSource, new FixedUUIDSource)
    val property = service.getFileProperty("Name").futureValue
    property.isDefined should equal(true)
    property.get.propertyid should equal(fixedPropertyId)
    property.get.name.get should equal("Name")
    property.get.shortname.get should equal("ShortName")
    property.get.description.get should equal("Description")
  }
}
