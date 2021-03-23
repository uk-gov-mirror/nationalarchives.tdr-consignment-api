package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.Tables.ConsignmentstatusRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentStatusRepository

import java.util.UUID
import scala.concurrent.Future

class ConsignmentStatusService(consignmentStatusRepository: ConsignmentStatusRepository) {

  def getConsignmentStatus(consignmentId: UUID): Future[Seq[ConsignmentstatusRow]] = {
    consignmentStatusRepository.getConsignmentStatus(consignmentId)
  }
}
