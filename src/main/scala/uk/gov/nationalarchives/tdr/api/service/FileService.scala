package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, Files}

import scala.concurrent.{ExecutionContext, Future}

class FileService(
                   fileRepository: FileRepository,
                   consignmentRepository: ConsignmentRepository,
                   timeSource: TimeSource,
                   uuidSource: UUIDSource
                 )(implicit val executionContext: ExecutionContext) {

  def addFile(addFilesInput: AddFilesInput, userId: Option[UUID]): Future[Files] = {
    val rows: Seq[nationalarchives.Tables.FileRow] = List.fill(addFilesInput.numberOfFiles)(1)
      .map(_ => FileRow(uuidSource.uuid, addFilesInput.consignmentId, userId.get, Timestamp.from(timeSource.now)))

    fileRepository.addFiles(rows).map(_.map(_.fileid)).map(fileids => Files(fileids))
  }

  def getOwnersOfFiles(fileIds: Seq[UUID]): Future[Seq[FileOwnership]] = {
    consignmentRepository.getConsignmentsOfFiles(fileIds)
      .map(_.map(consignmentByFile => FileOwnership(consignmentByFile._1, consignmentByFile._2.userid)))
  }

  def findNonExistentFiles(fileIds: Seq[UUID]) = {
    fileRepository.filesExist(fileIds).map(result => {
      fileIds.filter(id => {
        !result.map(_.fileid).contains(id)
      })
    })
  }
}

case class FileOwnership(fileId: UUID, userId: UUID)
