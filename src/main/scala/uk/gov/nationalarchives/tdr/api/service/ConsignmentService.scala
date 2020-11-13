package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, SeriesRow}
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
    val consignment: Future[Seq[SeriesRow]] = consignmentRepository.getSeriesOfConsignment(consignmentId)
    consignment.map(rows => rows.headOption.map(
      series => Series(series.seriesid, series.bodyid, series.name, series.code, series.description)))
  }

  def getTransferringBodyOfConsignment(consignmentId: UUID): Future[Option[TransferringBody]] = {
    val consignment: Future[Seq[BodyRow]] = consignmentRepository.getTransferringBodyOfConsignment(consignmentId)
    consignment.map(rows => rows.headOption.map(
      transferringBody => TransferringBody(transferringBody.name)))
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    consignmentRepository.consignmentHasFiles(consignmentId)
  }

  def getConsignmentFileProgress(consignmentId: UUID): Future[FileChecks] = {
    for {
      avMetadataCount <- fileRepository.countProcessedAvMetadataInConsignment(consignmentId)
      checksumCount <- fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId)
      fileFormatIdCount <- ffidMetadataRepository.countProcessedFfidMetadata(consignmentId)
    } yield FileChecks(AntivirusProgress(avMetadataCount), ChecksumProgress(checksumCount), FFIDProgress(fileFormatIdCount))
  }

  def getConsignmentParentFolder(consignmentId: UUID): Future[Option[String]] = {
    consignmentRepository.getParentFolder(consignmentId)
  }
}
