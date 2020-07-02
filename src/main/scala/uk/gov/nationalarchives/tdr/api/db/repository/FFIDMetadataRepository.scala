package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Ffidmetadata, FfidmetadataRow}

import scala.concurrent.Future

class FFIDMetadataRepository(db: Database) {

  private val insertQuery = Ffidmetadata returning Ffidmetadata.map(_.fileid) into
    ((ffidMetadata, fileid) => ffidMetadata.copy(fileid = fileid))

  def addFFIDMetadata(row: FfidmetadataRow): Future[FfidmetadataRow] = {
    db.run(insertQuery += row)
  }
}
