package uk.gov.nationalarchives.tdr.api.service

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.TransferagreementRow
import uk.gov.nationalarchives.tdr.api.db.repository.TransferAgreementRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addTransferAgreement" should "create a transfer agreement given correct arguments" in {
    val repositoryMock = mock[TransferAgreementRepository]
    val mockResponse = Future.successful(TransferagreementRow(1L,
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(1L)))

    when(repositoryMock.addTransferAgreement(any[TransferagreementRow])).thenReturn(mockResponse)
    val service = new TransferAgreementService(repositoryMock)
    val result: TransferAgreement = service.addTransferAgreement(AddTransferAgreementInput(1L,
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(true),
      Some(true))).await()

    result.consignmentId shouldBe 1
    result.allCrownCopyright.get shouldBe true
    result.allDigital.get shouldBe true
    result.allEnglish.get shouldBe true
    result.allPublicRecords.get shouldBe true
    result.appraisalSelectionSignedOff.get shouldBe true
    result.sensitivityReviewSignedOff.get shouldBe true
  }
}
