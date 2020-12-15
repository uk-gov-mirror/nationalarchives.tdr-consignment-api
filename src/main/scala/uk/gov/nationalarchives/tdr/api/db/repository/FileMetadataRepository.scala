package uk.gov.nationalarchives.tdr.api.db.repository

import java.sql.Timestamp
import java.util.UUID

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Filemetadata, _}
import uk.gov.nationalarchives.tdr.api.db.repository.FileMetadataRepository.FileMetadataRowWithName
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataRepository(db: Database)(implicit val executionContext: ExecutionContext) {
  def sqlRowToFileMetadata(propertyName: String, row: FilemetadataRow): FileMetadataRowWithName =
    FileMetadataRowWithName(propertyName, row.metadataid, row.fileid, row.value, row.datetime, row.userid)

  val insertQuery: PostgresProfile.IntoInsertActionComposer[FilemetadataRow, FileMetadataRowWithName] =
    Filemetadata returning Filemetadata.join(Fileproperty).on(_.propertyid === _.propertyid).map(_._2) into
      ((fileMetadata, fileproperty) => sqlRowToFileMetadata(fileproperty.name.get, fileMetadata))

  def addFileMetadata(rows: Seq[FileMetadataRowWithName]): Future[Seq[FileMetadataRowWithName]] = {
    db.run(DBIO.seq(rows.map(fileMetadataRowAction): _*).transactionally).map(_ => rows)
  }

  def addChecksumMetadata(row: FileMetadataRowWithName, validationResult: Option[Boolean]): Future[FileMetadataRowWithName] = {
    val checksumMetadataInsert = fileMetadataRowAction(row)
    val checksumValidationUpdate = (for {
      file <- File if file.fileid === row.fileid
    } yield file.checksummatches).update(validationResult)

    val allUpdates = DBIO.seq(checksumMetadataInsert, checksumValidationUpdate).transactionally
    val result: Future[Unit] = db.run(allUpdates)
    result.map(_ => row)
  }

  private def fileMetadataRowAction(row: FileMetadataRowWithName) = {
    for {
      property <- Fileproperty.filter(_.name === row.propertyName).result.head
      r <- Filemetadata += FilemetadataRow(row.metadataid, row.fileid, property.propertyid, row.value, row.datetime, row.userid)
    } yield row
  }

  def getFileMetadata(fileId: UUID, propertyName: String*): Future[Seq[FileMetadataRowWithName]] = {
    val query = Filemetadata.join(Fileproperty)
      .on(_.propertyid === _.propertyid)
      .filter(_._1.fileid === fileId)
      .filter(_._2.name inSet propertyName.toSet)
      .map(f => f._2.name.get -> f._1)
    db.run(query.result).map(_.map(f => sqlRowToFileMetadata(f._1, f._2)))
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

object FileMetadataRepository {

  case class FileMetadataRowWithName(propertyName: String, metadataid: UUID, fileid: UUID, value: String, datetime: Timestamp, userid: UUID)

}
