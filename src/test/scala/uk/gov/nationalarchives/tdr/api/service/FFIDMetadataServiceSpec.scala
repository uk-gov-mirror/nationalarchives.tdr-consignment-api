package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{FfidmetadataRow, FfidmetadatamatchesRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FFIDMetadataMatchesRepository, FFIDMetadataRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.{FFIDMetadataInput, FFIDMetadataInputMatches}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class FFIDMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addFFIDMetadata" should "call the repositories with the correct values" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUUIDSource = new FixedUUIDSource
    val fixedFileMetadataId = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val metadataRepository = mock[FFIDMetadataRepository]
    val matchesRepository = mock[FFIDMetadataMatchesRepository]

    val metadataCaptor: ArgumentCaptor[FfidmetadataRow] =  ArgumentCaptor.forClass(classOf[FfidmetadataRow])
    val matchCaptor: ArgumentCaptor[List[FfidmetadatamatchesRow]] =  ArgumentCaptor.forClass(classOf[List[FfidmetadatamatchesRow]])

    val mockMetadataRow: FfidmetadataRow = getMockMetadataRow(fixedFileMetadataId, fixedFileUuid, Timestamp.from(FixedTimeSource.now))
    val mockMetadataMatchesRow = getMatchesRow(mockMetadataRow.ffidmetadataid)

    val mockMetadataResponse = Future(mockMetadataRow)
    val mockMetadataMatchesResponse = Future(List(mockMetadataMatchesRow))

    when(metadataRepository.addFFIDMetadata(metadataCaptor.capture())).thenReturn(mockMetadataResponse)
    when(matchesRepository.addFFIDMetadataMatches(matchCaptor.capture())).thenReturn(mockMetadataMatchesResponse)

    val service = new FFIDMetadataService(metadataRepository, matchesRepository, FixedTimeSource, new FixedUUIDSource())
    service.addFFIDMetadata(getMetadataInput(fixedFileUuid))
    metadataCaptor.getValue should equal(mockMetadataRow)
  }

  "addFFIDMetadata" should "create ffid metadata given the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val fixedUUIDSource = new FixedUUIDSource
    val fixedFileMetadataId = fixedUUIDSource.uuid
    fixedUUIDSource.reset

    val metadataRepository = mock[FFIDMetadataRepository]
    val matchesRepository = mock[FFIDMetadataMatchesRepository]

    val metadataCaptor: ArgumentCaptor[FfidmetadataRow] =  ArgumentCaptor.forClass(classOf[FfidmetadataRow])
    val matchCaptor: ArgumentCaptor[List[FfidmetadatamatchesRow]] =  ArgumentCaptor.forClass(classOf[List[FfidmetadatamatchesRow]])

    val mockMetadataRow: FfidmetadataRow = getMockMetadataRow(fixedFileMetadataId, fixedFileUuid, Timestamp.from(FixedTimeSource.now))
    val mockMetadataMatchesRow = getMatchesRow(mockMetadataRow.ffidmetadataid)

    val mockMetadataResponse = Future(mockMetadataRow)
    val mockMetadataMatchesResponse = Future(List(mockMetadataMatchesRow))

    when(metadataRepository.addFFIDMetadata(metadataCaptor.capture())).thenReturn(mockMetadataResponse)
    when(matchesRepository.addFFIDMetadataMatches(matchCaptor.capture())).thenReturn(mockMetadataMatchesResponse)

    val service = new FFIDMetadataService(metadataRepository, matchesRepository, FixedTimeSource, new FixedUUIDSource())
    val result = service.addFFIDMetadata(getMetadataInput(fixedFileUuid)).futureValue
    result.fileId shouldEqual fixedFileUuid
    result.software shouldEqual "software"
    result.softwareVersion shouldEqual "softwareVersion"
    result.binarySignatureFileVersion shouldEqual "binaryVersion"
    result.containerSignatureFileVersion shouldEqual "containerVersion"
    result.method shouldEqual "method"
    result.datetime shouldEqual Timestamp.from(FixedTimeSource.now).getTime

    result.matches.size should equal(1)
    val matches = result.matches.head
    matches.extension.get shouldEqual "ext"
    matches.identificationBasis shouldEqual "identificationBasis"
    matches.puid.get shouldEqual "puid"
  }

  private def getMetadataInput(fixedFileUuid: UUID) = {
    FFIDMetadataInput(
      fixedFileUuid,
      "software",
      "softwareVersion",
      "binaryVersion",
      "containerVersion",
      "method",
      List(FFIDMetadataInputMatches(Some("ext"), "identificationBasis", Some("puid")))
    )
  }

  private def getMatchesRow(fileMetadataId: UUID) = {
    FfidmetadatamatchesRow(fileMetadataId, Some("ext"), "identificationBasis", Some("puid"))
  }

  private def getMockMetadataRow(ffidMetadataId: UUID, fixedFileUuid: UUID, dummyTimestamp: Timestamp): FfidmetadataRow = {
    FfidmetadataRow(ffidMetadataId, fixedFileUuid, "software", "softwareVersion",dummyTimestamp, "binaryVersion", "containerVersion", "method")
  }
}
