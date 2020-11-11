package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, SeriesRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment, FileChecks}
import uk.gov.nationalarchives.tdr.api.graphql.fields.{ConsignmentFields, SeriesFields}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  private val fixedConsignmentUUID = UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")

  "createConsignment" should "create a consignment given correct arguments" in {
    val fixedUuidSource = new FixedUUIDSource()
    val userUuid = UUID.randomUUID()
    val seriesUuid = UUID.randomUUID()
    val consignmentUuid = fixedUuidSource.uuid
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val mockResponse = Future.successful(ConsignmentRow(consignmentUuid, seriesUuid, userUuid, Timestamp.from(Instant.now)))
    when(consignmentRepoMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)
    val consignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)
    val result: Consignment = consignmentService.addConsignment(AddConsignmentInput(seriesUuid), Some(userUuid)).futureValue
    result.consignmentid shouldBe consignmentUuid
    result.seriesid shouldBe seriesUuid
    result.userid shouldBe userUuid
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val fixedUuidSource = new FixedUUIDSource()
    val userUuid = UUID.randomUUID()
    val seriesUuid = UUID.randomUUID()
    val consignmentId = UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val consignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)

    val expectedRow = ConsignmentRow(consignmentId, seriesUuid, userUuid, Timestamp.from(FixedTimeSource.now))
    val mockResponse = Future.successful(ConsignmentRow(consignmentId, seriesUuid, userUuid, Timestamp.from(Instant.now)))
    when(consignmentRepoMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)

    consignmentService.addConsignment(AddConsignmentInput(seriesUuid), Some(userUuid)).futureValue

    verify(consignmentRepoMock).addConsignment(expectedRow)
  }

  "getConsignment" should "return the specific Consignment for the requested consignment id" in {
    val fixedUuidSource = new FixedUUIDSource()
    val userUuid = UUID.randomUUID()
    val seriesUuid = UUID.randomUUID()
    val consignmentUuid = UUID.randomUUID()
    val consignmentRow = ConsignmentRow(consignmentUuid, seriesUuid, userUuid, Timestamp.from(Instant.now))
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq(consignmentRow))
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    when(consignmentRepoMock.getConsignment(any[UUID])).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)
    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(consignmentUuid).futureValue

    verify(consignmentRepoMock, times(1)).getConsignment(any[UUID])
    val consignment: ConsignmentFields.Consignment = response.get
    consignment.consignmentid should equal(consignmentUuid)
    consignment.seriesid should equal(seriesUuid)
    consignment.userid should equal(userUuid)
  }

  "getConsignment" should "return none when consignment id does not exist" in {
    val fixedUuidSource = new FixedUUIDSource()
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq())
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    when(consignmentRepoMock.getConsignment(any[UUID])).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)
    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(UUID.randomUUID()).futureValue
    verify(consignmentRepoMock, times(1)).getConsignment(any[UUID])

    response should be(None)
  }

  "consignmentHasFiles" should "return true when files already associated with provided consignment id" in {
    val fixedUuidSource = new FixedUUIDSource()
    val mockResponse: Future[Boolean] = Future.successful(true)
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    when(consignmentRepoMock.consignmentHasFiles(fixedConsignmentUUID)).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)
    val response: Boolean = consignmentService.consignmentHasFiles(fixedConsignmentUUID).futureValue

    response should be(true)
  }

  "consignmentHasFiles" should "return false when no files associated with provided consignment id" in {
    val fixedUuidSource = new FixedUUIDSource()
    val mockResponse: Future[Boolean] = Future.successful(false)
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    when(consignmentRepoMock.consignmentHasFiles(fixedConsignmentUUID)).thenReturn(mockResponse)

    val consignmentService: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)
    val response: Boolean = consignmentService.consignmentHasFiles(fixedConsignmentUUID).futureValue

    response should be(false)
  }

  "getConsignmentFileProgress" should "return total processed files" in {
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val fixedUuidSource = new FixedUUIDSource()

    val service: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)

    val consignmentId = UUID.fromString("3c8da55a-bca0-4cd8-8efb-fb2d316e88ee")
    val filesProcessed = 78

    when(fileRepositoryMock.countProcessedAvMetadataInConsignment(consignmentId)).thenReturn(Future.successful(filesProcessed))
    when(fileMetadataRepositoryMock.countProcessedChecksumInConsignment(consignmentId)).thenReturn(Future.successful(filesProcessed))
    when(ffidMetadataRepositoryMock.countProcessedFfidMetadata(consignmentId)).thenReturn(Future.successful(filesProcessed))

    val progress: FileChecks = service.getConsignmentFileProgress(consignmentId).futureValue

    progress.antivirusProgress.filesProcessed shouldBe filesProcessed
    progress.checksumProgress.filesProcessed shouldBe filesProcessed
    progress.ffidProgress.filesProcessed shouldBe filesProcessed
  }

  "getConsignmentParentFolder" should "return the parent folder name for a given consignment" in {
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val fixedUuidSource = new FixedUUIDSource()

    val service: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)

    val consignmentId = UUID.fromString("d8383f9f-c277-49dc-b082-f6e266a39618")
    val parentFolder: Option[String] = Option("CONSIGNMENT SERVICE PARENT FOLDER TEST")

    when(consignmentRepoMock.getParentFolder(consignmentId)).thenReturn(Future.successful(parentFolder))

    val parentFolderResult: Option[String] = service.getConsignmentParentFolder(consignmentId).futureValue

    parentFolderResult shouldBe parentFolder
  }

  "getSeriesOfConsignment" should "return the series for a given consignment" in {
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val fixedUuidSource = new FixedUUIDSource()

    val service: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)

    val consignmentId = UUID.fromString("d8383f9f-c277-49dc-b082-f6e266a39618")
    val seriesId = UUID.fromString("bf55455e-2017-11eb-adc1-0242ac120002")
    val bodyId = UUID.fromString("8eae8ed8-201c-11eb-adc1-0242ac120002")
    val seriesName = Option("Mock series")
    val seriesCode = Option("Mock series")
    val seriesDescription = Option("Series description")

    val mockSeries = Seq(SeriesRow(seriesId, bodyId, seriesName, seriesCode, seriesDescription))

    when(consignmentRepoMock.getSeriesOfConsignment(consignmentId)).thenReturn(Future.successful(mockSeries))

    val expectedSeries: SeriesFields.Series = service.getSeriesOfConsignment(consignmentId).futureValue.get

    expectedSeries.seriesid shouldBe mockSeries.head.seriesid
    expectedSeries.bodyid shouldBe mockSeries.head.bodyid
    expectedSeries.name shouldBe mockSeries.head.name
    expectedSeries.code shouldBe mockSeries.head.code
    expectedSeries.description shouldBe mockSeries.head.description
  }

  "getTransferringBodyOfConsignment" should "return the transferring body for a given consignment" in {
    val consignmentRepoMock = mock[ConsignmentRepository]
    val fileMetadataRepositoryMock = mock[FileMetadataRepository]
    val fileRepositoryMock = mock[FileRepository]
    val ffidMetadataRepositoryMock = mock[FFIDMetadataRepository]
    val fixedUuidSource = new FixedUUIDSource()

    val service: ConsignmentService = new ConsignmentService(consignmentRepoMock,
      fileMetadataRepositoryMock,
      fileRepositoryMock,
      ffidMetadataRepositoryMock,
      FixedTimeSource,
      fixedUuidSource)

    val consignmentId = UUID.fromString("d8383f9f-c277-49dc-b082-f6e266a39618")
    val bodyId = UUID.fromString("8eae8ed8-201c-11eb-adc1-0242ac120002")
    val bodyName = Option("Mock department")
    val bodyCode = Option("Mock department")
    val bodyDescription = Option("Body description")

    val mockBody = Seq(BodyRow(bodyId, bodyName, bodyCode, bodyDescription))

    when(consignmentRepoMock.getTransferringBodyOfConsignment(consignmentId)).thenReturn(Future.successful(mockBody))

    val expectedBody: ConsignmentFields.TransferringBody = service.getTransferringBodyOfConsignment(consignmentId).futureValue.get

    expectedBody.name shouldBe mockBody.head.name
  }
}
