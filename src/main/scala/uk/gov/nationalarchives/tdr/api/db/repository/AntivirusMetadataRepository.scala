package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{AvmetadataRow, Avmetadata}

import scala.concurrent.Future

class AntivirusMetadataRepository(db: Database) {
  private val insertQuery = Avmetadata returning Avmetadata.map(_.fileid) into
    ((antivirusMetadata, fileid) => antivirusMetadata.copy(fileid = fileid))

  def addAntivirusMetadata(antivirusMetadataRow: AvmetadataRow): Future[AvmetadataRow] = {
    db.run(insertQuery += antivirusMetadataRow)
  }
}
