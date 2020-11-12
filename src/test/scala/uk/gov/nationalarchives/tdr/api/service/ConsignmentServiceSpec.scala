package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, SeriesRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment, FileChecks}
import uk.gov.nationalarchives.tdr.api.graphql.fields.{ConsignmentFields, SeriesFields}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentServiceSpec extends AnyFlatSpec with MockitoSugar with ResetMocksAfterEachTest with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val fixedUuidSource: FixedUUIDSource = mock[FixedUUIDSource]
  val bodyId: UUID = UUID.fromString("8eae8ed8-201c-11eb-adc1-0242ac120002")
  val userId: UUID = UUID.randomUUID()
  val seriesId: UUID = UUID.randomUUID()
  val consignmentId: UUID = UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")
  val seriesName: Option[String] = Option("Mock series")
  val seriesCode: Option[String] = Option("Mock series")
  val seriesDescription: Option[String] = Option("Series description")
  val bodyName: Option[String] = Option("Mock department")
  val bodyCode: Option[String] = Option("Mock department")
  val bodyDescription: Option[String] = Option("Body description")
  val mockConsignment: ConsignmentRow = ConsignmentRow(consignmentId, seriesId, userId, Timestamp.from(FixedTimeSource.now))

  val consignmentRepoMock: ConsignmentRepository = mock[ConsignmentRepository]
  val fileMetadataRepositoryMock: FileMetadataRepository = mock[FileMetadataRepository]
  val fileRepositoryMock: FileRepository = mock[FileRepository]
  val ffidMetadataRepositoryMock: FFIDMetadataRepository = mock[FFIDMetadataRepository]
  val mockResponse: Future[ConsignmentRow] = Future.successful(mockConsignment)
  val consignmentService = new ConsignmentService(consignmentRepoMock,
    fileMetadataRepositoryMock,
    fileRepositoryMock,
    ffidMetadataRepositoryMock,
    FixedTimeSource,
    fixedUuidSource)

  "createConsignment" should "create a consignment given correct arguments" in {
    when(consignmentRepoMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)
    val result: Consignment = consignmentService.addConsignment(AddConsignmentInput(seriesId), Some(userId)).futureValue
    result.consignmentid shouldBe consignmentId
    result.seriesid shouldBe seriesId
    result.userid shouldBe userId
  }

  "createConsignment" should "link a consignment to the user's ID" in {
    val expectedRow = mockConsignment
    val mockResponse = Future.successful(mockConsignment)
    when(consignmentRepoMock.addConsignment(any[ConsignmentRow])).thenReturn(mockResponse)
    when(fixedUuidSource.uuid).thenReturn(consignmentId)
    consignmentService.addConsignment(AddConsignmentInput(seriesId), Some(userId)).futureValue

    verify(consignmentRepoMock).addConsignment(expectedRow)
  }

  "getConsignment" should "return the specific Consignment for the requested consignment id" in {
    val consignmentRow = mockConsignment
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq(consignmentRow))
    when(consignmentRepoMock.getConsignment(any[UUID])).thenReturn(mockResponse)

    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(consignmentId).futureValue

    verify(consignmentRepoMock, times(1)).getConsignment(any[UUID])
    val consignment: ConsignmentFields.Consignment = response.get
    consignment.consignmentid should equal(consignmentId)
    consignment.seriesid should equal(seriesId)
    consignment.userid should equal(userId)
  }

  "getConsignment" should "return none when consignment id does not exist" in {
    val mockResponse: Future[Seq[ConsignmentRow]] = Future.successful(Seq())
    when(consignmentRepoMock.getConsignment(any[UUID])).thenReturn(mockResponse)

    val response: Option[ConsignmentFields.Consignment] = consignmentService.getConsignment(UUID.randomUUID()).futureValue
    verify(consignmentRepoMock, times(1)).getConsignment(any[UUID])

    response should be(None)
  }

  "consignmentHasFiles" should "return true when files already associated with provided consignment id" in {
    val mockResponse: Future[Boolean] = Future.successful(true)
    when(consignmentRepoMock.consignmentHasFiles(consignmentId)).thenReturn(mockResponse)

    val response: Boolean = consignmentService.consignmentHasFiles(consignmentId).futureValue
    response should be(true)
  }

  "consignmentHasFiles" should "return false when no files associated with provided consignment id" in {
    val mockResponse: Future[Boolean] = Future.successful(false)
    when(consignmentRepoMock.consignmentHasFiles(consignmentId)).thenReturn(mockResponse)

    val response: Boolean = consignmentService.consignmentHasFiles(consignmentId).futureValue
    response should be(false)
  }

  "getConsignmentFileProgress" should "return total processed files" in {
    val filesProcessed = 78
    when(fileRepositoryMock.countProcessedAvMetadataInConsignment(consignmentId)).thenReturn(Future.successful(filesProcessed))
    when(fileMetadataRepositoryMock.countProcessedChecksumInConsignment(consignmentId)).thenReturn(Future.successful(filesProcessed))
    when(ffidMetadataRepositoryMock.countProcessedFfidMetadata(consignmentId)).thenReturn(Future.successful(filesProcessed))

    val progress: FileChecks = consignmentService.getConsignmentFileProgress(consignmentId).futureValue
    progress.antivirusProgress.filesProcessed shouldBe filesProcessed
    progress.checksumProgress.filesProcessed shouldBe filesProcessed
    progress.ffidProgress.filesProcessed shouldBe filesProcessed
  }

  "getConsignmentParentFolder" should "return the parent folder name for a given consignment" in {
    val parentFolder: Option[String] = Option("CONSIGNMENT SERVICE PARENT FOLDER TEST")
    when(consignmentRepoMock.getParentFolder(consignmentId)).thenReturn(Future.successful(parentFolder))

    val parentFolderResult: Option[String] = consignmentService.getConsignmentParentFolder(consignmentId).futureValue
    parentFolderResult shouldBe parentFolder
  }

  "getSeriesOfConsignment" should "return the series for a given consignment" in {
    val mockSeries = Seq(SeriesRow(seriesId, bodyId, seriesName, seriesCode, seriesDescription))
    when(consignmentRepoMock.getSeriesOfConsignment(consignmentId)).thenReturn(Future.successful(mockSeries))

    val expectedSeries: SeriesFields.Series = consignmentService.getSeriesOfConsignment(consignmentId).futureValue.get
    expectedSeries.seriesid shouldBe mockSeries.head.seriesid
    expectedSeries.bodyid shouldBe mockSeries.head.bodyid
    expectedSeries.name shouldBe mockSeries.head.name
    expectedSeries.code shouldBe mockSeries.head.code
    expectedSeries.description shouldBe mockSeries.head.description
  }

  "getTransferringBodyOfConsignment" should "return the transferring body for a given consignment" in {
    val mockBody = Seq(BodyRow(bodyId, bodyName, bodyCode, bodyDescription))
    when(consignmentRepoMock.getTransferringBodyOfConsignment(consignmentId)).thenReturn(Future.successful(mockBody))

    val expectedBody: ConsignmentFields.TransferringBody = consignmentService.getTransferringBodyOfConsignment(consignmentId).futureValue.get
    expectedBody.name shouldBe mockBody.head.name
  }
}
