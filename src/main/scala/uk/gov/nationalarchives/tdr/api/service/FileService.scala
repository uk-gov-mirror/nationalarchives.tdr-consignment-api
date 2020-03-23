package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables.FileRow
import uk.gov.nationalarchives.tdr.api.db.repository.FileRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.{AddFilesInput, File}

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileRepository: FileRepository, timeSource: TimeSource)(implicit val executionContext: ExecutionContext) {

  def addFile(addFilesInput: AddFilesInput, userId: Option[UUID]): Future[File] = {
    val rows: Seq[nationalarchives.Tables.FileRow] = List.fill(addFilesInput.numberOfFiles.getOrElse(1))(1)
      .map(_ => FileRow(addFilesInput.consignmentId, userId.get.toString, Timestamp.from(timeSource.now)))

    fileRepository.addFiles(rows).map(_.map(_.fileid.get)).map(fileids => File(fileids))
  }
}
