package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, ConsignmentmetadataRow, SeriesRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentMetadataFields._
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.Series

import scala.concurrent.{ExecutionContext, Future}


class ConsignmentMetadataService(
                                  consignmentMetadataRepository: ConsignmentMetadataRepository,
                                  timeSource: TimeSource,
                                  uuidSource: UUIDSource
                                )(implicit val executionContext: ExecutionContext) {

  def addConsignmentMetadata(consignmentMetadataInputs: Seq[AddConsignmentMetadataInput], userId: UUID): Future[Unit] = {
    val consignmentMetadataRows: Seq[ConsignmentmetadataRow] = consignmentMetadataInputs.map(i => ConsignmentmetadataRow(
      uuidSource.uuid,
      i.consignmentid,
      i.propertyName,
      i.value,
      Timestamp.from(timeSource.now),
      userId
    ))
    consignmentMetadataRepository.addConsignmentMetadata(consignmentMetadataRows)
  }
}
