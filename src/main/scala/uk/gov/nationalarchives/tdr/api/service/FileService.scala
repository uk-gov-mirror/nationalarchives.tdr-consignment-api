package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.Tables.{ConsignmentstatusRow, FileRow, FilemetadataRow}
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, Files}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._

import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class FileService(
                   fileRepository: FileRepository,
                   consignmentRepository: ConsignmentRepository,
                   fileMetadataRepository: FileMetadataRepository,
                   ffidMetadataRepository: FFIDMetadataRepository,
                   ffidMetadataMatchesRepository: FFIDMetadataMatchesRepository,
                   timeSource: TimeSource,
                   uuidSource: UUIDSource
                 )(implicit val executionContext: ExecutionContext) {

  def addFile(addFilesInput: AddFilesInput, userId: UUID): Future[Files] = {
    val now = Timestamp.from(timeSource.now)

    val rows: Seq[FileRow] = List.fill(addFilesInput.numberOfFiles)(1)
      .map(_ => FileRow(uuidSource.uuid, addFilesInput.consignmentId, userId, now))

    val consignmentStatusRow = ConsignmentstatusRow(uuidSource.uuid, addFilesInput.consignmentId, "Upload", "InProgress", now)

    def fileMetadataRows(fileRows: Seq[FileRow]): Seq[FilemetadataRow] = for {
      staticMetadata <- staticMetadataProperties
      fileId <- fileRows.map(_.fileid)
    } yield FilemetadataRow(uuidSource.uuid, fileId, staticMetadata.value, now, userId, staticMetadata.name)

    for {
      _ <- consignmentRepository.addParentFolder(addFilesInput.consignmentId, addFilesInput.parentFolder)
      files <- fileRepository.addFiles(rows, consignmentStatusRow)
      _ <- fileMetadataRepository.addFileMetadata(fileMetadataRows(files))
    } yield Files(files.map(_.fileid))
  }

  def getOwnersOfFiles(fileIds: Seq[UUID]): Future[Seq[FileOwnership]] = {
    consignmentRepository.getConsignmentsOfFiles(fileIds)
      .map(_.map(consignmentByFile => FileOwnership(consignmentByFile._1, consignmentByFile._2.userid)))
  }

  def fileCount(consignmentId: UUID): Future[Int] = {
    fileRepository.countFilesInConsignment(consignmentId)
  }

  def getFiles(consignmentId: UUID): Future[Files] = {
    fileRepository.getFilesWithPassedAntivirus(consignmentId).map(r => Files(r.map(_.fileid)))
  }

  def getFileMetadata(consignmentId: UUID): Future[List[File]] = {
    val fileMetadataService = new FileMetadataService(fileMetadataRepository, timeSource, uuidSource)
    val ffidMetadataService = new FFIDMetadataService(ffidMetadataRepository, ffidMetadataMatchesRepository, timeSource, uuidSource)
    for {
      fileMetadataList <- fileMetadataService.getFileMetadata(consignmentId)
      ffidMetadataList <- ffidMetadataService.getFFIDMetadata(consignmentId)
    } yield {
      fileMetadataList map {
        case (fileId, fileMetadata) =>
          File(fileId, fileMetadata, ffidMetadataList.find(_.fileId == fileId))
      }
    }.toList
  }
}

case class FileOwnership(fileId: UUID, userId: UUID)
