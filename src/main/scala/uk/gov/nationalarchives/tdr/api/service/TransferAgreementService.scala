package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables._
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.tdr.api.service.TransferAgreementService._

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementService(consignmentMetadataRepository: ConsignmentMetadataRepository, uuidSource: UUIDSource, timeSource: TimeSource)
                              (implicit val executionContext: ExecutionContext) {

  def addTransferAgreement(input: AddTransferAgreementInput, userId: UUID): Future[TransferAgreement] = {
    consignmentMetadataRepository.addConsignmentMetadata(convertInputToPropertyRows(input, userId)).map(
      rows => convertDbRowsToTransferAgreement(input.consignmentId, rows)
    )
  }

  private def convertInputToPropertyRows(input: AddTransferAgreementInput, userId: UUID): Seq[ConsignmentmetadataRow] = {
    val time = Timestamp.from(timeSource.now)
    val consignmentId = input.consignmentId
    Seq(
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(PublicRecordsConfirmed), Some(input.allPublicRecords.toString), time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(AllEnglishConfirmed), Some(input.allEnglish.toString), time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(AppraisalSelectionSignOffConfirmed), Some(input.appraisalSelectionSignedOff.toString), time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(CrownCopyrightConfirmed), Some(input.allCrownCopyright.toString), time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(InitialOpenRecordsConfirmed), Some(input.initialOpenRecords.toString), time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, Some(consignmentId), Some(SensitivityReviewSignOffConfirmed), Some(input.sensitivityReviewSignedOff.toString), time, userId)
    )
  }

  private def convertDbRowsToTransferAgreement(consignmentId: UUID, rows: Seq[ConsignmentmetadataRow]): TransferAgreement = {
    val propertyNameToValue = rows.map(row => row.propertyname.get -> row.value.map(_.toBoolean)).toMap
    TransferAgreement(consignmentId,
      propertyNameToValue.getOrElse(PublicRecordsConfirmed, Some(false)),
      propertyNameToValue.getOrElse(CrownCopyrightConfirmed, Some(false)),
      propertyNameToValue.getOrElse(AllEnglishConfirmed, Some(false)),
      propertyNameToValue.getOrElse(AppraisalSelectionSignOffConfirmed, Some(false)),
      propertyNameToValue.getOrElse(InitialOpenRecordsConfirmed, Some(false)),
      propertyNameToValue.getOrElse(SensitivityReviewSignOffConfirmed, Some(false)),
      isAgreementComplete(propertyNameToValue)
    )
  }

  def getTransferAgreement(consignmentId: UUID): Future[TransferAgreement] = {
    consignmentMetadataRepository.getConsignmentMetadata(
      consignmentId, transferAgreementProperties: _*).map(rows => convertDbRowsToTransferAgreement(consignmentId, rows))
      .recover {
      case nse: NoSuchElementException => throw InputDataException(s"Could not find metadata for consignment $consignmentId", Some(nse))
      case e: SQLException => throw InputDataException(e.getLocalizedMessage, Some(e))
    }
  }

  private def isAgreementComplete(propertyNameToValue: Map[String, Option[Boolean]]): Boolean = {
    transferAgreementProperties.map(p => {
      propertyNameToValue(p).getOrElse(false)
    }) forall (_ == true)
  }
}

object TransferAgreementService {
  val AllEnglishConfirmed = "AllEnglishConfirmed"
  val PublicRecordsConfirmed = "PublicRecordsConfirmed"
  val AppraisalSelectionSignOffConfirmed = "AppraisalSelectionSignOffConfirmed"
  val CrownCopyrightConfirmed = "CrownCopyrightConfirmed"
  val InitialOpenRecordsConfirmed = "InitialOpenRecordsConfirmed"
  val SensitivityReviewSignOffConfirmed = "SensitivityReviewSignOffConfirmed"

  val transferAgreementProperties = List(
    AllEnglishConfirmed,
    PublicRecordsConfirmed,
    AppraisalSelectionSignOffConfirmed,
    InitialOpenRecordsConfirmed,
    CrownCopyrightConfirmed,
    SensitivityReviewSignOffConfirmed
  )
}
