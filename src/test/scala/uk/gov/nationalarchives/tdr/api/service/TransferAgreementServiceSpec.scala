package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables._
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val fixedUuidSource = new FixedUUIDSource()
  val fixedTimeSource: FixedTimeSource.type = FixedTimeSource

  "addTransferAgreement" should "add the correct metadata given correct arguments" in {

    val repositoryMock = mock[ConsignmentMetadataRepository]
    val metadataId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    def row(name: String, value: String): ConsignmentmetadataRow =
      ConsignmentmetadataRow(metadataId, consignmentId, name, value, Timestamp.from(FixedTimeSource.now), userId)
    val mockResponse = Future.successful(Seq(
      row("AllEnglishConfirmed", "true"),
      row("CrownCopyrightConfirmed", "true"),
      row("AppraisalSelectionSignOffConfirmed", "true"),
      row("InitialOpenRecordsConfirmed", "true"),
      row("PublicRecordsConfirmed", "true"),
      row("SensitivityReviewSignOffConfirmed", "true")
    ))

    when(repositoryMock.addConsignmentMetadata(any[Seq[ConsignmentmetadataRow]])).thenReturn(mockResponse)

    val service = new TransferAgreementService(repositoryMock, fixedUuidSource, fixedTimeSource)
    val result: TransferAgreement = service.addTransferAgreement(AddTransferAgreementInput(consignmentId,
      initialOpenRecords = true,
      allCrownCopyright = true,
      allEnglish = true,
      allPublicRecords = true,
      appraisalSelectionSignedOff = true,
      sensitivityReviewSignedOff = true), userId).futureValue

    result.consignmentId shouldBe consignmentId
    result.initialOpenRecords shouldBe true
    result.allCrownCopyright shouldBe true
    result.allEnglish shouldBe true
    result.allPublicRecords shouldBe true
    result.appraisalSelectionSignedOff shouldBe true
    result.sensitivityReviewSignedOff shouldBe true
  }
}
