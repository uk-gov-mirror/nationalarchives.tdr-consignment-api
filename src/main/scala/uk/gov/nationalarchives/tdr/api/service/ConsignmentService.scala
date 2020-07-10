package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{AddConsignmentInput, Consignment}

import scala.concurrent.{ExecutionContext, Future}


class ConsignmentService(
                          consignmentRepository: ConsignmentRepository,
                          fileRepository: FileRepository,
                          timeSource: TimeSource,
                          uuidSource: UUIDSource
                        )(implicit val executionContext: ExecutionContext) {

  def addConsignment(addConsignmentInput: AddConsignmentInput, userId: Option[UUID]): Future[Consignment] = {
      val consignmentRow = ConsignmentRow(uuidSource.uuid, addConsignmentInput.seriesid, userId.get, Timestamp.from(timeSource.now))
      consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(Some(row.consignmentid), row.userid, row.seriesid, 0))
    }

  def getConsignment(consignmentId: UUID): Future[Option[Consignment]] = {
    val consignments = consignmentRepository.getConsignment(consignmentId)
    consignments.flatMap(rows => {
      val filesResult = fileRepository.countFilesInConsignment(consignmentId)
      filesResult.map(totalFiles => {
        rows.headOption.map(row => Consignment(Some(row.consignmentid), row.userid, row.seriesid, totalFiles))
      })
    })
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    consignmentRepository.consignmentHasFiles(consignmentId)
  }
}
