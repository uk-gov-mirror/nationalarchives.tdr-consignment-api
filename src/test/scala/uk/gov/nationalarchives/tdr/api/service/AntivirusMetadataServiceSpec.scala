package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.AntivirusMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.AddAntivirusMetadataInput
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class AntivirusMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addAntivirusMetadata" should "create anti-virus metadata given the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val repositoryMock = mock[AntivirusMetadataRepository]
    val mockResponse = Future.successful(AvmetadataRow(
      fixedFileUuid,
      Some("software"),
      Some("value"),
      Some("software version"),
      Some("database version"),
      Some("result"),
      dummyTimestamp
    ))

    when(repositoryMock.addAntivirusMetadata(any[AvmetadataRow])).thenReturn(mockResponse)

    val service: AntivirusMetadataService = new AntivirusMetadataService(repositoryMock)
    val result = service.addAntivirusMetadata(AddAntivirusMetadataInput(
      fixedFileUuid,
      Some("software"),
      Some("value"),
      Some("software version"),
      Some("database version"),
      Some("result"),
      dummyInstant.toEpochMilli
    )).await()

    result.fileId shouldBe fixedFileUuid
    result.software.get shouldBe "software"
    result.value.get shouldBe "value"
    result.softwareVersion.get shouldBe "software version"
    result.databaseVersion.get shouldBe "database version"
    result.result.get shouldBe "result"
    result.datetime shouldBe dummyInstant.toEpochMilli
  }
}
