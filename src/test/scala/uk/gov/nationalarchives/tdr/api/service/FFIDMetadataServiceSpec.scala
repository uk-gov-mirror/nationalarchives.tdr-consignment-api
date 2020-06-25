package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov
import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.FfidmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.FFIDMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.FFIDMetadataInput

import scala.concurrent.{ExecutionContext, Future}

class FFIDMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFFIDMetadata" should "call the repository with the correct values" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val repositoryMock = mock[FFIDMetadataRepository]
    val dummyInstant = Instant.now().getLong(ChronoField.MICRO_OF_SECOND)
    val dummyTimestamp = Timestamp.from(Instant.ofEpochMilli(dummyInstant))
    val captor: ArgumentCaptor[FfidmetadataRow] =  ArgumentCaptor.forClass(classOf[FfidmetadataRow])
    val mockRow: nationalarchives.Tables.FfidmetadataRow = getMockRow(fixedFileUuid, dummyTimestamp)
    val mockResponse = Future(mockRow)
    when(repositoryMock.addFFIDMetadata(captor.capture())).thenReturn(mockResponse)
    val service = new FFIDMetadataService(repositoryMock)
    service.addFFIDMetadata(getMetadataInput(fixedFileUuid, dummyTimestamp))
    captor.getValue should equal(mockRow)
  }

  "addFFIDMetadata" should "create ffid metadata given the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val repositoryMock = mock[FFIDMetadataRepository]
    val dummyInstant = Instant.now().getLong(ChronoField.MICRO_OF_SECOND)
    val dummyTimestamp = Timestamp.from(Instant.ofEpochMilli(dummyInstant))
    val mockRow: Tables.FfidmetadataRow = getMockRow(fixedFileUuid, dummyTimestamp)
    val mockResponse = Future(mockRow)
    when(repositoryMock.addFFIDMetadata(any[FfidmetadataRow])).thenReturn(mockResponse)
    val service = new FFIDMetadataService(repositoryMock)
    val result = service.addFFIDMetadata(getMetadataInput(fixedFileUuid, dummyTimestamp)).futureValue
    result.fileId shouldEqual fixedFileUuid
    result.software shouldEqual "software"
    result.softwareVersion shouldEqual "softwareVersion"
    result.binarySignatureFileVersion shouldEqual "binaryVersion"
    result.containerSignatureFileVersion shouldEqual "containerVersion"
    result.method shouldEqual "method"
    result.extension.get shouldEqual "ext"
    result.identificationBasis shouldEqual "identificationBasis"
    result.puid.get shouldEqual "puid"
    result.datetime shouldEqual dummyInstant
  }

  private def getMetadataInput(fixedFileUuid: UUID, dummyTimestamp: Timestamp) = {
    FFIDMetadataInput(
      fixedFileUuid,
      "software",
      "softwareVersion",
      "binaryVersion",
      "containerVersion",
      "method",
      Some("ext"),
      "identificationBasis",
      Some("puid"),
      dummyTimestamp.getTime
    )
  }

  private def getMockRow(fixedFileUuid: UUID, dummyTimestamp: Timestamp): gov.nationalarchives.Tables.FfidmetadataRow = {
    FfidmetadataRow(
      fixedFileUuid,
      "software",
      "softwareVersion",
      "binaryVersion",
      "containerVersion",
      "method",
      Some("ext"),
      "identificationBasis",
      Some("puid"),
      dummyTimestamp
    )
  }
}
