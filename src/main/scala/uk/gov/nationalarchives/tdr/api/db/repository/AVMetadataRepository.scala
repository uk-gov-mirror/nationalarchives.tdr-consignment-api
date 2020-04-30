package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{AvmetadataRow, Avmetadata}

import scala.concurrent.Future

class AVMetadataRepository(db: Database) {
  private val insertQuery = Avmetadata returning Avmetadata.map(_.fileid) into
    ((avMetadata, fileid) => avMetadata.copy(fileid = fileid))

  def addAVMetadata(avMetadataRows: Seq[AvmetadataRow]): Future[Seq[AvmetadataRow]] = {
    db.run(insertQuery ++= avMetadataRows)
  }
}
