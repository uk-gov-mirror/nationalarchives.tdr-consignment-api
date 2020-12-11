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
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, SHA256ClientSideChecksum, SHA256ServerSideChecksum}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService.ChecksumMetadata

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFileMetadata" should "call the metadata repository with the correct row arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, "value", Timestamp.from(FixedTimeSource.now), fixedUserId)
    )
    val propertyRow = Some(FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("Description"), Some("ShortName")))
    val mockPropertyResponse = Future.successful(propertyRow)
    val mockClientFileMetadata = ClientFileMetadata(fixedFileUuid, Option.empty, Some("checksum"), Some("Mock"), 1, Option.empty, 1, UUID.randomUUID)
    val fixedUUIDSource = new FixedUUIDSource()
    val metadataId: UUID = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val captor: ArgumentCaptor[FilemetadataRow] = ArgumentCaptor.forClass(classOf[FilemetadataRow])
    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(captor.capture(), validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)
    when(clientFileMetadataServiceMock.getClientFileMetadata(fixedFileUuid)).thenReturn(Future(mockClientFileMetadata))
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "value"), fixedUserId).futureValue


    val row = captor.getValue
    row.propertyid should equal(fixedPropertyId)
    row.fileid should equal(fixedFileUuid)
    row.userid should equal(fixedUserId)
    row.datetime should equal(Timestamp.from(FixedTimeSource.now))
    row.metadataid.shouldBe(metadataId)
  }

  "addFileMetadata" should "call the metadata repository with the correct arguments if the checksum matches" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, "value", Timestamp.from(FixedTimeSource.now), fixedUserId)
    )
    val propertyRow = Some(FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("Description"), Some("ShortName")))
    val mockPropertyResponse = Future.successful(propertyRow)
    val mockClientFileMetadata = ClientFileMetadata(fixedFileUuid, Option.empty, Some("checksum"), Some("Mock"), 1, Option.empty, 1, UUID.randomUUID)
    val fixedUUIDSource = new FixedUUIDSource()
    fixedUUIDSource.reset

    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)
    when(clientFileMetadataServiceMock.getClientFileMetadata(fixedFileUuid)).thenReturn(Future(mockClientFileMetadata))
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "checksum"), fixedUserId).futureValue
    validationResultCaptor.getValue.get should be(true)
  }

  "addFileMetadata" should "call the metadata repository with the correct arguments if the checksum doesn't match" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val mockMetadataResponse = Future.successful(
      FilemetadataRow(UUID.randomUUID(), fixedFileUuid, fixedPropertyId, "value", Timestamp.from(FixedTimeSource.now), fixedUserId)
    )
    val propertyRow = Some(FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("Description"), Some("ShortName")))
    val mockPropertyResponse = Future.successful(propertyRow)
    val mockClientFileMetadata = ClientFileMetadata(fixedFileUuid, Option.empty, Some("checksum"), Some("Mock"), 1, Option.empty, 1, UUID.randomUUID)
    val fixedUUIDSource = new FixedUUIDSource()
    fixedUUIDSource.reset

    val validationResultCaptor: ArgumentCaptor[Option[Boolean]] = ArgumentCaptor.forClass(classOf[Option[Boolean]])
    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], validationResultCaptor.capture()))
      .thenReturn(mockMetadataResponse)
    when(clientFileMetadataServiceMock.getClientFileMetadata(fixedFileUuid)).thenReturn(Future(mockClientFileMetadata))
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuid, "anotherchecksum"), fixedUserId).futureValue
    validationResultCaptor.getValue.get should be(false)
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
    val mockClientFileMetadata = ClientFileMetadata(fixedFileUuid, Option.empty, Some("checksum"), Some("Mock"), 1, Option.empty, 1, UUID.randomUUID)
    val propertyName = SHA256ServerSideChecksum
    val mockPropertyResponse = Future.successful(Some(FilepropertyRow(fixedPropertyId, Some(propertyName), Some("Description"), Some("ShortName"))))
    val fixedUUIDSource = new FixedUUIDSource()


    when(metadataRepositoryMock.addChecksumMetadata(any[FilemetadataRow], any[Option[Boolean]])).thenReturn(mockMetadataResponse)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)
    when(clientFileMetadataServiceMock.getClientFileMetadata(any[UUID])).thenReturn(Future(mockClientFileMetadata))

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    val result: FileMetadataFields.FileMetadata =
      service.addFileMetadata(AddFileMetadataInput(propertyName, fixedFileUuid, "value"), fixedUserId).futureValue

    result.fileId should equal(fixedFileUuid)
    result.filePropertyName should equal(propertyName)
    result.value should equal(value)
  }

  "addFileMetadata" should "fail if the update is not for a checksum" in {
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val filePropertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val fileId = UUID.randomUUID()

    val service = new FileMetadataService(fileMetadataRepositoryMock, filePropertyRepositoryMock,
      clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource())
    val err = service.addFileMetadata(AddFileMetadataInput("SomethingElse", fileId, "checksum"), UUID.randomUUID()).failed.futureValue
    err.getMessage should equal("SomethingElse found. We are only expecting checksum updates for now")
  }

  "addFileMetadata" should "call the metadata repository with the correct arguments for multiple updates" in {
    val fixedFileUuidOne = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedFileUuidTwo = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val valueOne = "valueOne"
    val valueTwo = "valueTwo"
    val mockMetadataResponse = Future.successful(
      Seq(
        FilemetadataRow(UUID.randomUUID(), fixedFileUuidOne, fixedPropertyId, valueOne, Timestamp.from(FixedTimeSource.now), fixedUserId),
        FilemetadataRow(UUID.randomUUID(), fixedFileUuidTwo, fixedPropertyId, valueTwo, Timestamp.from(FixedTimeSource.now), fixedUserId)
      )
    )

    val propertyRow = FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("Description"), Some("ShortName"))
    val mockPropertyResponse = Future.successful(Seq(propertyRow))
    val fixedUUIDSource = new FixedUUIDSource()

    val captor: ArgumentCaptor[Seq[ChecksumMetadata]] = ArgumentCaptor.forClass(classOf[Seq[ChecksumMetadata]])
    when(metadataRepositoryMock.addChecksumMetadata(captor.capture()))
      .thenReturn(mockMetadataResponse)

    when(metadataRepositoryMock.getFileMetadata(any[UUID], any[String])).thenReturn(Future(Option.empty))
    when(metadataRepositoryMock.addMetadata(any[Seq[FilemetadataRow]])).thenReturn(Future(Seq()))
    when(propertyRepositoryMock.getPropertiesByName(any[List[String]])).thenReturn(mockPropertyResponse)

    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    service.addFileMetadata(Seq(
      AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuidOne, valueOne),
      AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuidTwo, valueTwo),
    ), fixedUserId).futureValue
    fixedUUIDSource.reset

    val rowOne = captor.getValue.toList.head.fileMetadataRow
    rowOne.propertyid should equal(fixedPropertyId)
    rowOne.value should equal(valueOne)
    rowOne.fileid should equal(fixedFileUuidOne)
    rowOne.userid should equal(fixedUserId)
    rowOne.datetime should equal(Timestamp.from(FixedTimeSource.now))
    rowOne.metadataid.shouldBe(fixedUUIDSource.uuid)

    val rowTwo = captor.getValue.toList.last.fileMetadataRow
    rowTwo.propertyid should equal(fixedPropertyId)
    rowTwo.value should equal(valueTwo)
    rowTwo.fileid should equal(fixedFileUuidTwo)
    rowTwo.userid should equal(fixedUserId)
    rowTwo.datetime should equal(Timestamp.from(FixedTimeSource.now))
    rowTwo.metadataid.shouldBe(fixedUUIDSource.uuid)
  }

  "addFileMetadata" should "return the correct data for multiple updates" in {
    val fixedFileUuidOne = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val fixedFileUuidTwo = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUserId = UUID.fromString("61b49923-daf7-4140-98f1-58ba6cbed61f")
    val metadataRepositoryMock = mock[FileMetadataRepository]
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val valueOne = "valueOne"
    val valueTwo = "valueTwo"
    val mockMetadataResponse = Future.successful(
      Seq(
        FilemetadataRow(UUID.randomUUID(), fixedFileUuidOne, fixedPropertyId, valueOne, Timestamp.from(FixedTimeSource.now), fixedUserId),
        FilemetadataRow(UUID.randomUUID(), fixedFileUuidTwo, fixedPropertyId, valueTwo, Timestamp.from(FixedTimeSource.now), fixedUserId)
      )
    )

    val propertyRowOne = FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("ServerDescription"), Some("ShortName"))
    val propertyRowTwo = FilepropertyRow(fixedPropertyId, Some(SHA256ClientSideChecksum), Some("ClientDescription"), Some("ShortName"))
    val mockPropertyResponse = Future.successful(Seq(propertyRowOne, propertyRowTwo))
    val fixedUUIDSource = new FixedUUIDSource()

    when(metadataRepositoryMock.addChecksumMetadata(any[Seq[ChecksumMetadata]]))
      .thenReturn(mockMetadataResponse)

    when(metadataRepositoryMock.getFileMetadata(any[UUID], any[String])).thenReturn(Future(Option.empty))
    when(metadataRepositoryMock.addMetadata(any[Seq[FilemetadataRow]])).thenReturn(Future(Seq()))
    when(propertyRepositoryMock.getPropertiesByName(any[List[String]])).thenReturn(mockPropertyResponse)


    val service = new FileMetadataService(metadataRepositoryMock, propertyRepositoryMock, clientFileMetadataServiceMock, FixedTimeSource, fixedUUIDSource)
    val result = service.addFileMetadata(Seq(
      AddFileMetadataInput(SHA256ServerSideChecksum, fixedFileUuidOne, valueOne),
      AddFileMetadataInput(SHA256ClientSideChecksum, fixedFileUuidTwo, valueTwo),
    ), fixedUserId).futureValue


  }

  "addFileMetadata" should "update the checksum validation if one update is for server side checksum and the checksum matches" in {

  }

  "addFileMetadata" should "update the checksum validation if one update is for server side checksum and the checksum doesn't match" in {

  }

  "addFileMetadata" should "not update the checksum validation if no updates are for server side checksum" in {

  }

  "getFileProperty" should "return a file property" in {
    val fixedPropertyId = UUID.fromString("6929ca2a-c920-41d3-bf8d-25a7da5d8ae2")
    val propertyRepositoryMock = mock[FilePropertyRepository]
    val clientFileMetadataServiceMock = mock[ClientFileMetadataService]
    val filePropertyRow = Some(FilepropertyRow(fixedPropertyId, Some(SHA256ServerSideChecksum), Some("Description"), Some("ShortName")))
    val mockPropertyResponse = Future.successful(filePropertyRow)
    when(propertyRepositoryMock.getPropertyByName(any[String])).thenReturn(mockPropertyResponse)
    val service = new FileMetadataService(mock[FileMetadataRepository], propertyRepositoryMock,
      clientFileMetadataServiceMock, FixedTimeSource, new FixedUUIDSource)
    val property = service.getFileProperty(SHA256ServerSideChecksum).futureValue
    property.isDefined should equal(true)
    property.get.propertyid should equal(fixedPropertyId)
    property.get.name.get should equal(SHA256ServerSideChecksum)
    property.get.shortname.get should equal("ShortName")
    property.get.description.get should equal("Description")
  }

}
