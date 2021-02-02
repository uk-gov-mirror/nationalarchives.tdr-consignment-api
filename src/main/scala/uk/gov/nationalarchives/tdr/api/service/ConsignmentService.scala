package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}
import java.util.{Calendar, UUID}

import uk.gov.nationalarchives.Tables.{BodyRow, ConsignmentRow, SeriesRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields._
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields.Series
import uk.gov.nationalarchives.tdr.api.model.consignment.ConsignmentReference

import scala.concurrent.{ExecutionContext, Future}


class ConsignmentService(
                          consignmentRepository: ConsignmentRepository,
                          fileMetadataRepository: FileMetadataRepository,
                          fileRepository: FileRepository,
                          ffidMetadataRepository: FFIDMetadataRepository,
                          timeSource: TimeSource,
                          uuidSource: UUIDSource
                        )(implicit val executionContext: ExecutionContext) {

  val calendar: Calendar = Calendar.getInstance()

  def updateTransferInitiated(consignmentId: UUID, userId: UUID): Future[Int] = {
    consignmentRepository.updateTransferInitiated(consignmentId, userId, Timestamp.from(timeSource.now))
  }

  def updateExportLocation(exportLocationInput: UpdateExportLocationInput): Future[Int] = {
    consignmentRepository.updateExportLocation(exportLocationInput, Timestamp.from(timeSource.now))
  }

  def addConsignment(addConsignmentInput: AddConsignmentInput, userId: UUID): Future[Consignment] = {
    val now = timeSource.now
    val yearNow = LocalDate.from(now.atOffset(ZoneOffset.UTC)).getYear
     consignmentRepository.getNextConsignmentSequence.flatMap(sequence => {
       val consignmentRef = ConsignmentReference.createConsignmentReference(yearNow, sequence)
       val consignmentRow = ConsignmentRow(
         uuidSource.uuid,
         addConsignmentInput.seriesid,
         userId,
         Timestamp.from(now),
         consignmentsequence = Option(sequence))
       consignmentRepository.addConsignment(consignmentRow).map(row => Consignment(row.consignmentid, row.userid, row.seriesid, Option(consignmentRef)))
     })
  }

  def getConsignment(consignmentId: UUID): Future[Option[Consignment]] = {
    val consignments = consignmentRepository.getConsignment(consignmentId)
    consignments.map(rows => rows.headOption.map(row => Consignment(row.consignmentid, row.userid, row.seriesid, row.consignmentreference)))
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
      checksumCount <- Future(2)
      fileFormatIdCount <- Future(3)
    } yield FileChecks(AntivirusProgress(avMetadataCount), ChecksumProgress(checksumCount), FFIDProgress(fileFormatIdCount))
  }

  def getConsignmentParentFolder(consignmentId: UUID): Future[Option[String]] = {
    consignmentRepository.getParentFolder(consignmentId)
  }
}
