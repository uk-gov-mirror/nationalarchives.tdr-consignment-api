package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction
import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{File, Filemetadata, FilemetadataRow, Fileproperty}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService.{ChecksumMetadata, MetadataInput, OtherMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataRepository(db: Database)(implicit val executionContext: ExecutionContext) {
  private val insertQuery = Filemetadata returning Filemetadata.map(_.fileid) into
    ((fileMetadata, fileid) => fileMetadata.copy(fileid = fileid))

  def addChecksumMetadata(fileMetadataRow: FilemetadataRow, validationResult: Option[Boolean]): Future[FilemetadataRow] = {
    val checksumMetadataInsert = insertQuery += fileMetadataRow
    val checksumValidationUpdate = (for {
      file <- File if file.fileid === fileMetadataRow.fileid
    } yield file.checksummatches).update(validationResult)

    val allUpdates = DBIO.seq(checksumMetadataInsert, checksumValidationUpdate).transactionally
    val result: Future[Unit] = db.run(allUpdates)
    result.map(_ => fileMetadataRow)
  }

  def addMetadata(rows: Seq[MetadataInput]): Future[Seq[FilemetadataRow]] = {
    val metadataRows = insertQuery ++= rows.map(_.fileMetadataRow()).toList
    val queries = rows.toList map {
      case ChecksumMetadata(fileMetadataRow, validationResult) =>
        val checksumValidationUpdate = for {
          file <- File if file.fileid === fileMetadataRow.fileid
        } yield file.checksummatches
        checksumValidationUpdate.update(validationResult)
    }

    val result = db.run(DBIO.seq(metadataRows :: queries:_*).transactionally)
    result.map(_ => rows.map(_.fileMetadataRow()))
  }

  def getFileMetadata(fileId: UUID, propertyName: String): Future[Option[_root_.uk.gov.nationalarchives.Tables.FilemetadataRow]] = {
    val query = Filemetadata.join(Fileproperty)
      .on(_.propertyid === _.propertyid)
      .filter(_._1.fileid === fileId)
      .filter(_._2.name === propertyName)
      .map(_._1)
    db.run(query.result.headOption)
  }

  def getFileMetadata(fileId: UUID, propertyId: UUID): Future[Option[_root_.uk.gov.nationalarchives.Tables.FilemetadataRow]] = {
    val query = Filemetadata
      .filter(_.fileid === fileId)
      .filter(_.propertyid === propertyId)
    db.run(query.result.headOption)
  }

  def countProcessedChecksumInConsignment(consignmentId: UUID): Future[Int] = {
    val query = Filemetadata.join(File)
      .on(_.fileid === _.fileid).join(Fileproperty)
      .on(_._1.propertyid === _.propertyid)
      .filter(_._1._2.consignmentid === consignmentId)
      .filter(_._2.name === SHA256ServerSideChecksum)
      .groupBy(_._1._2.fileid)
      .map(_._1)
      .length
    db.run(query.result)
  }
}
