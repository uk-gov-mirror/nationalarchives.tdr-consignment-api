package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{File, FileRow}

import scala.concurrent.Future

class FileRepository(db: Database) {
  private val insertQuery = File  returning File.map(_.fileid)into ((file, fileid) => file.copy(fileid = fileid))

  def addFiles(fileRows: Seq[FileRow]): Future[Seq[Tables.FileRow]] = {
    db.run(insertQuery ++= fileRows)
  }
}
