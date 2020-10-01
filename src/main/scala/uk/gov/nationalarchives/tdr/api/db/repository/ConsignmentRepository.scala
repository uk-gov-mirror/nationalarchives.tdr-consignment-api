package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.CompiledFunction
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{Consignment, ConsignmentRow, File}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentRepository(db: Database) {

  private val insertQuery = Consignment returning Consignment.map(_.consignmentid) into
    ((consignment, consignmentid) => consignment.copy(consignmentid = consignmentid))

  def addConsignment(consignmentRow: ConsignmentRow): Future[ConsignmentRow] = {
    db.run(insertQuery += consignmentRow)
  }

  def getConsignment(consignmentId: UUID): Future[Seq[ConsignmentRow]] = {
    val query = Consignment.filter(_.consignmentid === consignmentId)
    db.run(query.result)
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    val query = File.filter(_.consignmentid === consignmentId).exists
    db.run(query.result)
  }

  def getConsignmentsOfFiles(fileIds: Seq[UUID])
                            (implicit executionContext: ExecutionContext):Future[Seq[(UUID, ConsignmentRow)]] = {
    val query = for {
      (file, consignment) <- File.join(Consignment).on(_.consignmentid === _.consignmentid).filter(_._1.fileid.inSet(fileIds))
    } yield (file.fileid, consignment)
    db.run(query.result)
  }

  def addParentFolder(consignmentId: UUID, parentFolder: Option[String])(implicit executionContext: ExecutionContext): Future[Unit] = {
    val updateAction = Consignment.filter(_.consignmentid === consignmentId).map(c => c.parentfolder).update(parentFolder)
    db.run(updateAction).map(_ => ())
  }

}