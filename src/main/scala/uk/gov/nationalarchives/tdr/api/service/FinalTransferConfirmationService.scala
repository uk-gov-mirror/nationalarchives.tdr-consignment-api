package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.ConsignmentmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.FinalTransferConfirmationFields._
import uk.gov.nationalarchives.tdr.api.service.FinalTransferConfirmationService.{FinalOpenRecordsConfirmed, LegalOwnershipTransferConfirmed}

import scala.concurrent.{ExecutionContext, Future}


class FinalTransferConfirmationService(consignmentMetadataRepository: ConsignmentMetadataRepository,
                                       uuidSource: UUIDSource,
                                       timeSource: TimeSource
                                      )(implicit val executionContext: ExecutionContext) {

  def addFinalTransferConfirmation(consignmentMetadataInputs: AddFinalTransferConfirmationInput, userId: UUID): Future[FinalTransferConfirmation] = {
    consignmentMetadataRepository.addConsignmentMetadata(convertInputToPropertyRows(consignmentMetadataInputs, userId)).map {
      rows => convertDbRowsToFinalTransferConfirmation(consignmentMetadataInputs.consignmentId, rows)
    }
  }

  private def convertInputToPropertyRows(inputs: AddFinalTransferConfirmationInput, userId: UUID): Seq[ConsignmentmetadataRow] = {
    val time = Timestamp.from(timeSource.now)
    Seq(
      ConsignmentmetadataRow(
        uuidSource.uuid, inputs.consignmentId, FinalOpenRecordsConfirmed, inputs.finalOpenRecordsConfirmed.toString, time, userId),
      ConsignmentmetadataRow(
        uuidSource.uuid, inputs.consignmentId, LegalOwnershipTransferConfirmed, inputs.legalOwnershipTransferConfirmed.toString, time, userId)
    )
  }

  private def convertDbRowsToFinalTransferConfirmation(consignmentId: UUID, rows: Seq[ConsignmentmetadataRow]): FinalTransferConfirmation = {
    val propertyNameToValue = rows.map(row => row.propertyname -> row.value.toBoolean).toMap
    FinalTransferConfirmation(consignmentId,
      propertyNameToValue(FinalOpenRecordsConfirmed),
      propertyNameToValue(LegalOwnershipTransferConfirmed)
    )
  }

}

object FinalTransferConfirmationService {
  val FinalOpenRecordsConfirmed = "FinalOpenRecordsConfirmed"
  val LegalOwnershipTransferConfirmed = "LegalOwnershipTransferConfirmed"

  val finalTransferConfirmationProperties = List(
    FinalOpenRecordsConfirmed,
    LegalOwnershipTransferConfirmed
  )
}
