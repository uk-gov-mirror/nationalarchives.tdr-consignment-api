package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.TransferagreementRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository.ConsignmentMetadataRowWithName
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val fixedUuidSource = new FixedUUIDSource()
  val fixedTimeSource = FixedTimeSource


  "addTransferAgreement" should "add the correct metadata given correct arguments" in {

    val repositoryMock = mock[ConsignmentMetadataRepository]
    val metadataId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    def row(name: String, value: String): ConsignmentMetadataRowWithName =
      ConsignmentMetadataRowWithName(name, metadataId, Some(consignmentId),  Some(value), Timestamp.from(FixedTimeSource.now), userId)
    val mockResponse = Future.successful(Seq(
      row("AllEnglishConfirmed", "true"),
      row("AppraisalSelectionSignOffConfirmed", "true"),
      row("InitialOpenRecordsConfirmed", "true"),
      row("CrownCopyrightConfirmed", "true"),
      row("SensitivityReviewSignOffConfirmed", "true")
    ))

    when(repositoryMock.addConsignmentMetadata(any[Seq[ConsignmentMetadataRowWithName]])).thenReturn(mockResponse)

    val service = new TransferAgreementService(repositoryMock, fixedUuidSource, fixedTimeSource)
    val result: TransferAgreement = service.addTransferAgreement(AddTransferAgreementInput(consignmentId,
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(true)), userId).futureValue

    result.consignmentId shouldBe consignmentId
    result.allCrownCopyright.get shouldBe true
    result.allEnglish.get shouldBe true
    result.allPublicRecords.get shouldBe true
    result.appraisalSelectionSignedOff.get shouldBe true
    result.sensitivityReviewSignedOff.get shouldBe true
  }
}
