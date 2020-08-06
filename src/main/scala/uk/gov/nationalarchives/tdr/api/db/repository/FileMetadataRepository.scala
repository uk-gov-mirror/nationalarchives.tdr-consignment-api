package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Clientfilemetadata, Consignment, File, Filemetadata, FilemetadataRow}

import scala.concurrent.Future

class FileMetadataRepository(db: Database) {
  private val insertQuery = Filemetadata returning Filemetadata.map(_.fileid) into
    ((fileMetadata, fileid) => fileMetadata.copy(fileid = fileid))


  def addFileMetadata(fileMetadataRows: FilemetadataRow): Future[FilemetadataRow] = {
    db.run(insertQuery += fileMetadataRows)
  }

  def countProcessedChecksumInConsignment(consignmentId: UUID): Future[Int] = {
    val propertyId = UUID.fromString("7a1b272c-e2f7-4b8f-8291-5e9dc312edb7")
    val query = Filemetadata.join(File)
      .on(_.fileid === _.fileid)
      .filter(_._2.consignmentid === consignmentId)
      .filter(_._1.propertyid === propertyId)
      .groupBy(_._1.fileid)
      .map(_._1)
      .length
    db.run(query.result)
  }
}
