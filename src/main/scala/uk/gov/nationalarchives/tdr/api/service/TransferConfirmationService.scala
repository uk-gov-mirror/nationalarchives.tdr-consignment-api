package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, ConsignmentmetadataRow, SeriesRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferConfirmationFields._
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.Series
import uk.gov.nationalarchives.tdr.api.service.TransferConfirmationService.{FinalOpenRecordsConfirmed, LegalOwnershipTransferConfirmed}

import scala.concurrent.{ExecutionContext, Future}


class TransferConfirmationService(consignmentMetadataRepository: ConsignmentMetadataRepository,
                                  uuidSource: UUIDSource,
                                  timeSource: TimeSource,
                                 )(implicit val executionContext: ExecutionContext) {

  def addTransferConfirmation(consignmentMetadataInputs: AddTransferConfirmationInput, userId: UUID): Future[TransferConfirmation] = {
    consignmentMetadataRepository.addConsignmentMetadata(convertInputToPropertyRows(consignmentMetadataInputs, userId)).map {
      rows => convertDbRowsToTransferConfirmation(consignmentMetadataInputs.consignmentid, rows)
    }
  }

  private def convertInputToPropertyRows(inputs: AddTransferConfirmationInput, userId: UUID): Seq[ConsignmentmetadataRow] = {
    val time = Timestamp.from(timeSource.now)
    Seq(
      ConsignmentmetadataRow(
        uuidSource.uuid, inputs.consignmentid, FinalOpenRecordsConfirmed, inputs.finalOpenRecordsConfirmed.toString, time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, inputs.consignmentid, LegalOwnershipTransferConfirmed, inputs.legalOwnershipTransferConfirmed.toString, time, userId)
    )
  }

  private def convertDbRowsToTransferConfirmation(consignmentId: UUID, rows: Seq[ConsignmentmetadataRow]): TransferConfirmation = {
    val propertyNameToValue = rows.map(row => row.propertyname -> row.value.toBoolean).toMap
    TransferConfirmation(consignmentId,
      propertyNameToValue(FinalOpenRecordsConfirmed),
      propertyNameToValue(LegalOwnershipTransferConfirmed)
    )
  }

}

object TransferConfirmationService {
  val FinalOpenRecordsConfirmed = "FinalOpenRecordsConfirmed"
  val LegalOwnershipTransferConfirmed = "LegalOwnershipTransferConfirmed"

  val TransferConfirmationProperties = List(
    FinalOpenRecordsConfirmed,
    LegalOwnershipTransferConfirmed
  )
}