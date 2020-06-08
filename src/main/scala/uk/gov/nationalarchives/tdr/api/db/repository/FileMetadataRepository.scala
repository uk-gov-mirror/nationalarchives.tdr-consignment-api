package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Filemetadata, FilemetadataRow}

import scala.concurrent.Future

class FileMetadataRepository(db: Database) {
  private val insertQuery = Filemetadata returning Filemetadata.map(_.fileid) into
    ((fileMetadata, fileid) => fileMetadata.copy(fileid = fileid))


  def addFileMetadata(fileMetadataRows: FilemetadataRow): Future[FilemetadataRow] = {
    db.run(insertQuery += fileMetadataRows)
  }
}
