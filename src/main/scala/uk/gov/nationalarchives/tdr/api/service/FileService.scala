package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.Tables.{ConsignmentstatusRow, FileRow, FilemetadataRow}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._
import uk.gov.nationalarchives.tdr.api.db.repository._
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.{FFIDMetadata, FFIDMetadataMatches}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, Files}

import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class FileService(
                   fileRepository: FileRepository,
                   consignmentRepository: ConsignmentRepository,
                   fileMetadataRepository: FileMetadataRepository,
                   ffidMetadataRepository: FFIDMetadataRepository,
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
    ffidMetadataRepository.getFFIDMetadata(consignmentId).map {
      ffidMetadataAndMatchesRows =>
        val ffidMetadataAndMatches = ffidMetadataAndMatchesRows.toMap

        fileMetadataRepository.getFileMetadata(consignmentId).map {
          rows =>
            rows.groupBy(_.fileid).map(entry => {
              val propertyNameMap: Map[String, String] = entry._2.groupBy(_.propertyname)
                .transform((_, value) => value.head.value)

              val fileMetadataValues = FileMetadataValues(
                propertyNameMap.get(SHA256ClientSideChecksum),
                propertyNameMap.get(ClientSideOriginalFilepath),
                propertyNameMap.get(ClientSideFileLastModifiedDate).map(d => Timestamp.valueOf(d).toLocalDateTime),
                propertyNameMap.get(ClientSideFileSize).map(_.toLong),
                propertyNameMap.get(RightsCopyright.name),
                propertyNameMap.get(LegalStatus.name),
                propertyNameMap.get(HeldBy.name),
                propertyNameMap.get(Language.name),
                propertyNameMap.get(FoiExemptionCode.name)
              )
              val fileId = entry._1
              val ffidMetadata = ffidMetadataAndMatches(fileId)._1
              val ffidMetadataMatches = ffidMetadataAndMatches(fileId)._2

              val fullFfidMetadata: FFIDMetadata = FFIDMetadata(
                ffidMetadata.fileid,
                ffidMetadata.software,
                ffidMetadata.softwareversion,
                ffidMetadata.binarysignaturefileversion,
                ffidMetadata.containersignaturefileversion,
                ffidMetadata.method,
                List(FFIDMetadataMatches(ffidMetadataMatches.extension, ffidMetadataMatches.identificationbasis, ffidMetadataMatches.puid)),
                ffidMetadata.datetime.getTime
              )

              File(fileId, fileMetadataValues, fullFfidMetadata)
            }).toList
        }
    }.flatten
  }
}

case class FileOwnership(fileId: UUID, userId: UUID)
