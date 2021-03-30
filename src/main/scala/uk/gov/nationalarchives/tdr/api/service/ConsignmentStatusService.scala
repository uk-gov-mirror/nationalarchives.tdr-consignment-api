package uk.gov.nationalarchives.tdr.api.service

import uk.gov
import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.ConsignmentstatusRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentStatusRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.CurrentStatus

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ConsignmentStatusService(consignmentStatusRepository: ConsignmentStatusRepository)
                              (implicit val executionContext: ExecutionContext) {

  def getConsignmentStatus(consignmentId: UUID): Future[CurrentStatus] = {
    for {
      upload <- consignmentStatusRepository.getConsignmentStatus(consignmentId).map(_.map(uploadRow => uploadRow.value).headOption)
    } yield CurrentStatus(upload)
  }
}