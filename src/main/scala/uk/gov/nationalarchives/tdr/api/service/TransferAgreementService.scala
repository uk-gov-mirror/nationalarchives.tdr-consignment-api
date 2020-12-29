package uk.gov.nationalarchives.tdr.api.service

import java.util.UUID
import java.sql.{SQLException, Timestamp}

import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException

import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentMetadataRepository.ConsignmentMetadataRowWithName
import uk.gov.nationalarchives.tdr.api.service.TransferAgreementService._

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementService(consignmentMetadataRepository: ConsignmentMetadataRepository, uuidSource: UUIDSource, timeSource: TimeSource)
                              (implicit val executionContext: ExecutionContext) {

  implicit class StringUtils(value: Option[String]) {
    def toBoolean: Boolean = value.get.toBoolean
  }

  def addTransferAgreement(input: AddTransferAgreementInput, userId: UUID): Future[TransferAgreement] = {
    consignmentMetadataRepository.addConsignmentMetadata(convertInputToPropertyRows(input, userId)).map(
      rows => convertDbRowsToTransferAgreement(input.consignmentId, rows)
    )
  }

  private def convertInputToPropertyRows(input: AddTransferAgreementInput, userId: UUID): Seq[ConsignmentMetadataRowWithName] = {
    val time = Timestamp.from(timeSource.now)
    val consignmentId = input.consignmentId
    Seq(
      ConsignmentMetadataRowWithName(
        PublicRecordsConfirmed, uuidSource.uuid, Some(consignmentId), Some(input.allPublicRecords.get.toString), time, userId),
      ConsignmentMetadataRowWithName(
        AllEnglishConfirmed, uuidSource.uuid, Some(consignmentId), Some(input.allEnglish.get.toString), time, userId),
      ConsignmentMetadataRowWithName(
        AppraisalSelectionSignOffConfirmed, uuidSource.uuid, Some(consignmentId), Some(input.appraisalSelectionSignedOff.get.toString), time, userId),
      ConsignmentMetadataRowWithName(
        CrownCopyrightConfirmed, uuidSource.uuid, Some(consignmentId), Some(input.allCrownCopyright.get.toString), time, userId),
      ConsignmentMetadataRowWithName(
        //initialOpenRecordsConfirmed field to be added to frontend
        //temporarily default to true until value passed from frontend input
        InitialOpenRecordsConfirmed, uuidSource.uuid, Some(consignmentId), Some("true"), time, userId),
      ConsignmentMetadataRowWithName(
        SensitivityReviewSignOffConfirmed, uuidSource.uuid, Some(consignmentId), Some(input.sensitivityReviewSignedOff.get.toString), time, userId)
    )
  }

  private def convertDbRowsToTransferAgreement(consignmentId: UUID, rows: Seq[ConsignmentMetadataRowWithName]): TransferAgreement = {
    val propertyNameToValue = rows.map(row => row.propertyName -> row.value.map(_.toBoolean)).toMap
    TransferAgreement(consignmentId,
      propertyNameToValue(PublicRecordsConfirmed),
      propertyNameToValue(CrownCopyrightConfirmed),
      propertyNameToValue(AllEnglishConfirmed),
      propertyNameToValue(AppraisalSelectionSignOffConfirmed),
      propertyNameToValue(InitialOpenRecordsConfirmed),
      propertyNameToValue(SensitivityReviewSignOffConfirmed),
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

  private def agreed(field: Option[Boolean]): Boolean = field.isDefined && field.get

  private def isAgreementComplete(propertyNameToValue: Map[String, Option[Boolean]]): Boolean = {
    transferAgreementProperties.map(p => {
      propertyNameToValue(p)
    }).forall(agreed)
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
