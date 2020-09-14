package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Ffidmetadata, FfidmetadataRow, File}

import scala.concurrent.Future

class FFIDMetadataRepository(db: Database) {

  private val insertQuery = Ffidmetadata returning Ffidmetadata.map(_.fileid) into
    ((ffidMetadata, fileid) => ffidMetadata.copy(fileid = fileid))

  def addFFIDMetadata(row: FfidmetadataRow): Future[FfidmetadataRow] = {
    db.run(insertQuery += row)
  }

  def countProcessedFfidMetadata(consignmentId: UUID): Future[Int] = {
    val query = Ffidmetadata.join(File)
      .on(_.fileid === _.fileid)
      .filter(_._2.consignmentid === consignmentId)
      .groupBy(_._1.fileid)
      .map(_._1)
      .length
    db.run(query.result)
  }
}
