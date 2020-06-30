package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ClientfilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.ClientFileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.{AddClientFileMetadataInput, ClientFileMetadata}
import uk.gov.nationalarchives.tdr.api.utils.FixedUUIDSource
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class ClientFileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addClientFileMetadata" should "create client file metadata given correct arguments" in {
    val fixedUuidSource = new FixedUUIDSource()
    val clientMetadataUuid = fixedUuidSource.uuid
    val fileUuid = UUID.randomUUID()
    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val dummyFileSize: Long = 1000
    val repositoryMock = mock[ClientFileMetadataRepository]
    val mockResponse = Future.successful(Seq(ClientfilemetadataRow(
      clientMetadataUuid,
      fileUuid,
      Some("dummy/original/path"),
      Some("dummyCheckSum"),
      Some("checksumType"),
      dummyTimestamp,
      dummyTimestamp,
      Some(dummyFileSize),
      dummyTimestamp
    ))
    )

    when(repositoryMock.addClientFileMetadata(any[Seq[ClientfilemetadataRow]])).thenReturn(mockResponse)
    val service = new ClientFileMetadataService(repositoryMock, fixedUuidSource)
    val result = service.addClientFileMetadata(Seq(AddClientFileMetadataInput(
      fileUuid,
      Some("dummy/original/path"),
      Some("dummyCheckSum"),
      Some("checksumType"),
      dummyInstant.toEpochMilli,
      dummyInstant.toEpochMilli,
      Some(dummyFileSize),
      dummyInstant.toEpochMilli))).futureValue

    result.length shouldBe 1
    val r: ClientFileMetadata = result.head
    r.fileId shouldBe fileUuid
    r.originalPath.get shouldBe "dummy/original/path"
    r.checksum.get shouldBe "dummyCheckSum"
    r.checksumType.get shouldBe "checksumType"
    r.lastModified shouldBe dummyInstant.toEpochMilli
    r.createdDate shouldBe dummyInstant.toEpochMilli
    r.fileSize.get shouldBe dummyFileSize
    r.datetime shouldBe dummyInstant.toEpochMilli
    r.clientFileMetadataId shouldBe clientMetadataUuid
  }

  "getClientFileMetaddata" should "return client file metadata given an existing file id" in {
    val fixedUuidSource = new FixedUUIDSource()
    val clientMetadataUuid = fixedUuidSource.uuid
    val fileUuid = UUID.randomUUID()
    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val dummyFileSize: Long = 1000
    val repositoryMock = mock[ClientFileMetadataRepository]
    val mockResponse = Future.successful(Seq(ClientfilemetadataRow(
      clientMetadataUuid,
      fileUuid,
      Some("dummy/original/path"),
      Some("dummyCheckSum"),
      Some("checksumType"),
      dummyTimestamp,
      dummyTimestamp,
      Some(dummyFileSize),
      dummyTimestamp
    )))
    when(repositoryMock.getClientFileMetadata(fileUuid)).thenReturn(mockResponse)

    val service = new ClientFileMetadataService(repositoryMock, fixedUuidSource)
    val result = service.getClientFileMetadata(fileUuid).futureValue

    result.fileId shouldBe fileUuid
    result.originalPath.get shouldBe "dummy/original/path"
    result.checksum.get shouldBe "dummyCheckSum"
    result.checksumType.get shouldBe "checksumType"
    result.lastModified shouldBe dummyInstant.toEpochMilli
    result.createdDate shouldBe dummyInstant.toEpochMilli
    result.fileSize.get shouldBe dummyFileSize
    result.datetime shouldBe dummyInstant.toEpochMilli
    result.clientFileMetadataId shouldBe clientMetadataUuid
  }

  "getClientFileMetaddata" should "return an empty list for a non existent id" in {
    val fixedUuidSource = new FixedUUIDSource()
    val fileUuid = UUID.randomUUID()
    val repositoryMock = mock[ClientFileMetadataRepository]
    when(repositoryMock.getClientFileMetadata(fileUuid)).thenReturn(Future(Seq()))
    val service = new ClientFileMetadataService(repositoryMock, fixedUuidSource)
    val caught: Throwable = service.getClientFileMetadata(fileUuid).failed.futureValue
    caught.getMessage should equal(s"Could not find metadata for file $fileUuid")
  }
}
