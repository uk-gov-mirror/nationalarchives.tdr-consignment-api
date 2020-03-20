package uk.gov.nationalarchives.tdr.api.db.repository

import uk.gov.nationalarchives.Tables.FileRow

import scala.concurrent.Future

class FileRepository {
  def addFile(fileRow: FileRow): Future[FileRow] = ???
}
