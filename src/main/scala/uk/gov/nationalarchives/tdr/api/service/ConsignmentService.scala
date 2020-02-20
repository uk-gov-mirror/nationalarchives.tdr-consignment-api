package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.Consignment

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentService(consignmentRepository: ConsignmentRepository)(implicit val executionContext: ExecutionContext) {
    def addConsignment(consignment: Consignment): Future[Consignment] = {
      val consignmentRow = ConsignmentRow(consignment.seriesid, consignment.userid, Timestamp.from(Instant.now()))
      consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(row.consignmentid, row.seriesid, row.userid))
    }
}
