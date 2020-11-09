package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields._
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.Series

import scala.concurrent.{ExecutionContext, Future}


class ConsignmentService(
                          consignmentRepository: ConsignmentRepository,
                          fileMetadataRepository: FileMetadataRepository,
                          fileRepository: FileRepository,
                          ffidMetadataRepository: FFIDMetadataRepository,
                          timeSource: TimeSource,
                          uuidSource: UUIDSource
                        )(implicit val executionContext: ExecutionContext) {

  def addConsignment(addConsignmentInput: AddConsignmentInput, userId: Option[UUID]): Future[Consignment] = {
      val consignmentRow = ConsignmentRow(uuidSource.uuid, addConsignmentInput.seriesid, userId.get, Timestamp.from(timeSource.now))
      consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(row.consignmentid, row.userid, row.seriesid))
    }

  def getConsignment(consignmentId: UUID): Future[Option[Consignment]] = {
    val consignments = consignmentRepository.getConsignment(consignmentId)
    consignments.map(rows => rows.headOption.map(row => Consignment(row.consignmentid, row.userid, row.seriesid)))
  }

  def getSeriesOfConsignment(consignmentId: UUID): Future[Option[Series]] = {
    val consignment: Future[Seq[ConsignmentResult]] = consignmentRepository.getSeriesAndBodyOfConsignment(consignmentId)
    consignment.map(rows => rows.headOption.map(
      row => Series(row.series.seriesid, row.series.bodyid, row.series.name, row.series.code, row.series.description)))
  }

  def getTbOfConsignment(consignmentId: UUID): Future[Option[TransferringBody]] = {
    val consignment: Future[Seq[ConsignmentResult]] = consignmentRepository.getSeriesAndBodyOfConsignment(consignmentId)
    consignment.map(rows => rows.headOption.map(
      row => TransferringBody(row.body.bodyid, row.body.name, row.body.code, row.body.description)))
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    consignmentRepository.consignmentHasFiles(consignmentId)
  }

  def getConsignmentFileProgress(consignmentId: UUID): Future[FileChecks] = {
    for {
      processed <- fileRepository.countProcessedAvMetadataInConsignment(consignmentId)
      checksum <- fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId)
      fileFormatId <- ffidMetadataRepository.countProcessedFfidMetadata(consignmentId)
    } yield FileChecks(AntivirusProgress(processed), ChecksumProgress(checksum), FFIDProgress(fileFormatId))
  }

  def getConsignmentParentFolder(consignmentId: UUID): Future[Option[String]] = {
    consignmentRepository.getParentFolder(consignmentId)
  }
}
