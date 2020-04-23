package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.ConsignmentRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}

import scala.concurrent.{ExecutionContext, Future}


class ConsignmentService(consignmentRepository: ConsignmentRepository, timeSource: TimeSource, uuidSource: UUIDSource)
                        (implicit val executionContext: ExecutionContext) {

  def addConsignment(addConsignmentInput: AddConsignmentInput, userId: Option[UUID]): Future[Consignment] = {
      val consignmentRow = ConsignmentRow(uuidSource.uuid, addConsignmentInput.seriesid, userId.get, Timestamp.from(timeSource.now))
      consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(Some(row.consignmentid), row.userid, row.seriesid))
    }

  def getConsignment(consignmentId: UUID): Future[Option[Consignment]] = {
    val consignments = consignmentRepository.getConsignment(consignmentId)

    consignments.map(rows => rows.headOption.map(row => Consignment(Some(row.consignmentid), row.userid, row.seriesid)))
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    consignmentRepository.consignmentHasFiles(consignmentId)
  }
}
