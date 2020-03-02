package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentService(consignmentRepository: ConsignmentRepository)(implicit val executionContext: ExecutionContext) {
    def addConsignment(addConsignmentInput: AddConsignmentInput): Future[Consignment] = {
      val consignmentRow = ConsignmentRow(addConsignmentInput.seriesid, addConsignmentInput.userid.toString, Timestamp.from(Instant.now()))
      consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(row.consignmentid, UUID.fromString(row.userid), row.seriesid))
    }

  def getConsignment(consignmentId: Long): Future[Option[Consignment]] = {
    val consignments = consignmentRepository.getConsignment(consignmentId)

    consignments.map(rows => rows.headOption.map(row => Consignment(row.consignmentid, UUID.fromString(row.userid), row.seriesid)))
  }
}
