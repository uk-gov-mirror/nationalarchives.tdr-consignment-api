package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Filemetadata, _}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataRepository(db: Database)(implicit val executionContext: ExecutionContext) {

  private val insertQuery = Filemetadata returning Filemetadata.map(_.metadataid) into
    ((filemetadata, metadataid) => filemetadata.copy(metadataid = metadataid))

  def addFileMetadata(rows: Seq[FilemetadataRow]): Future[Seq[FilemetadataRow]] = {
    db.run(insertQuery ++= rows)
  }

  def addChecksumMetadata(row: FilemetadataRow, validationResult: Option[Boolean]): Future[FilemetadataRow] = {
    val checksumValidationUpdate = (for {
      file <- File if file.fileid === row.fileid
    } yield file.checksummatches).update(validationResult)
    val allUpdates = DBIO.seq(insertQuery += row, checksumValidationUpdate).transactionally
    db.run(allUpdates).map(_ => row)
  }

  def getFileMetadata(fileId: UUID, propertyName: String*): Future[Seq[FilemetadataRow]] = {
    val query = Filemetadata
      .filter(_.fileid === fileId)
      .filter(_.propertyname inSet propertyName.toSet)
    db.run(query.result)
  }

  def getFileMetadata(consignmentId: UUID): Future[Seq[FilemetadataRow]] = {
    val query = Filemetadata.join(File)
      .on(_.fileid === _.fileid)
      .filter(_._2.consignmentid === consignmentId)
      .map(_._1)
    db.run(query.result)
  }

  def countProcessedChecksumInConsignment(consignmentId: UUID): Future[Int] = {
    val query = Filemetadata.join(File)
      .on(_.fileid === _.fileid).join(Fileproperty)
      .on(_._1.propertyname === _.name)
      .filter(_._1._2.consignmentid === consignmentId)
      .filter(_._2.name === SHA256ServerSideChecksum)
      .groupBy(_._1._2.fileid)
      .map(_._1)
      .length
    db.run(query.result)
  }
}
