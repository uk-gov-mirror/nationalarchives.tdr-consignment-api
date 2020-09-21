package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{File, Filemetadata, FilemetadataRow, Fileproperty}

import scala.concurrent.Future

class FileMetadataRepository(db: Database) {
  private val insertQuery = Filemetadata returning Filemetadata.map(_.fileid) into
    ((fileMetadata, fileid) => fileMetadata.copy(fileid = fileid))

  def addFileMetadata(fileMetadataRows: FilemetadataRow): Future[FilemetadataRow] = {
    db.run(insertQuery += fileMetadataRows)
  }

  def countProcessedChecksumInConsignment(consignmentId: UUID): Future[Int] = {
    val query = Filemetadata.join(File)
      .on(_.fileid === _.fileid).join(Fileproperty)
      .on(_._1.propertyid === _.propertyid)
      .filter(_._1._2.consignmentid === consignmentId)
      .filter(_._2.name === "SHA256ServerSideChecksum")
      .groupBy(_._1._2.fileid)
      .map(_._1)
      .length
    db.run(query.result)
  }

  def addChecksumValidationResult(fileId: UUID, validationResult: Option[Boolean]): Future[Int] = {
    val query = for {
      file <- File if file.fileid === fileId
    } yield file.checksummatches
    val update = query.update(validationResult)
    db.run(update)
  }
}
