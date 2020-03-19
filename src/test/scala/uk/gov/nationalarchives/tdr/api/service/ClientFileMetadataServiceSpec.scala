package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.ClientfilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.ClientFileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.AddClientFileMetadataInput
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class ClientFileMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addClientFileMetadata" should "create client file metadata given correct arguments" in {

    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val dummyFileSize: BigDecimal = 1000.01
    val repositoryMock = mock[ClientFileMetadataRepository]
    val mockResponse = Future.successful(ClientfilemetadataRow(
      1L,
      Some("dummy/original/path"),
      Some("dummyCheckSum"),
      Some("checksumType"),
      dummyTimestamp,
      dummyTimestamp,
      Some(dummyFileSize),
      dummyTimestamp,
      Some(1L))
    )

    when(repositoryMock.addClientFileMetadata(any[ClientfilemetadataRow])).thenReturn(mockResponse)
    val service = new ClientFileMetadataService(repositoryMock)
    val result = service.addClientFileMetadata(AddClientFileMetadataInput(
      1L,
      Some("dummy/original/path"),
      Some("dummyCheckSum"),
      Some("checksumType"),
      dummyInstant.toEpochMilli,
      dummyInstant.toEpochMilli,
      Some(dummyFileSize),
      dummyInstant.toEpochMilli)).await()

    result.fileId shouldBe 1
    result.originalPath.get shouldBe "dummy/original/path"
    result.checksum.get shouldBe "dummyCheckSum"
    result.checksumType.get shouldBe "checksumType"
    result.lastModified shouldBe dummyInstant.toEpochMilli
    result.createdDate shouldBe dummyInstant.toEpochMilli
    result.fileSize.get shouldBe dummyFileSize
    result.datetime shouldBe dummyInstant.toEpochMilli
    result.clientFileMetadataId shouldBe 1
  }
}
