package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.{AntivirusMetadataRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.AddAntivirusMetadataInput
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.FileChecks

import scala.concurrent.{ExecutionContext, Future}

class AntivirusMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addAntivirusMetadata" should "create anti-virus metadata given the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val avRepositoryMock = mock[AntivirusMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val mockResponse = Future.successful(AvmetadataRow(
      fixedFileUuid,
      "software",
      Some("value"),
      "software version",
      "database version",
      "result",
      dummyTimestamp
    ))

    when(avRepositoryMock.addAntivirusMetadata(any[AvmetadataRow])).thenReturn(mockResponse)

    val service: AntivirusMetadataService = new AntivirusMetadataService(avRepositoryMock, fileRepositoryMock)
    val result = service.addAntivirusMetadata(AddAntivirusMetadataInput(
      fixedFileUuid,
      "software",
      "software version",
      "database version",
      "result",
      dummyInstant.toEpochMilli
    )).futureValue

    result.fileId shouldBe fixedFileUuid
    result.software shouldBe "software"
    result.softwareVersion shouldBe "software version"
    result.databaseVersion shouldBe "database version"
    result.result shouldBe "result"
    result.datetime shouldBe dummyInstant.toEpochMilli
  }
}
