package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{Avmetadata, Consignment, File, FileRow}

import scala.concurrent.{ExecutionContext, Future}

class FileRepository(db: Database) {
  private val insertQuery = File  returning File.map(_.fileid)into ((file, fileid) => file.copy(fileid = fileid))

  def addFiles(fileRows: Seq[FileRow]): Future[Seq[Tables.FileRow]] = {
    db.run(insertQuery ++= fileRows)
  }

  def countFilesInConsignment(consignmentId: UUID): Future[Int] = {
    val query = File.filter(_.consignmentid === consignmentId).length
    db.run(query.result)
  }

  def countProcessedFilesInConsignment(consignmentId: UUID): Future[Int] = {
    val query = (for {
      (avmetadata, file) <- Avmetadata.join(File).on(_.fileid === _.fileid).filter(_._2.consignmentid === consignmentId)
    } yield (avmetadata, file )).groupBy(_._1.fileid).map{ case (fileid, _) => (fileid) }.length
    db.run(query.result)
  }
}
