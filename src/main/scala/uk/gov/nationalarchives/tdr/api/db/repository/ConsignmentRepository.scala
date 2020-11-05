package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.CompiledFunction
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.Tables.{Body, BodyRow, Consignment, ConsignmentRow, File, Series, SeriesRow}

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

  def getSeriesAndBodyOfConsignment(consignmentId: UUID)(implicit executionContext: ExecutionContext): Future[Seq[ConsignmentResult]] = {
    val query = Consignment.join(Series)
      .on(_.seriesid === _.seriesid).join(Body)
      .on(_._2.bodyid === _.bodyid)
      .filter(_._1._1.consignmentid === consignmentId)
      .map(rows => (rows._1._1, rows._1._2, rows._2))
    db.run(query.result).map(rows => rows.map(result => ConsignmentResult(result._1, result._2, result._3)))
  }

  def consignmentHasFiles(consignmentId: UUID): Future[Boolean] = {
    val query = File.filter(_.consignmentid === consignmentId).exists
    db.run(query.result)
  }

  def getConsignmentsOfFiles(fileIds: Seq[UUID])
                            (implicit executionContext: ExecutionContext): Future[Seq[(UUID, ConsignmentRow)]] = {
    val query = for {
      (file, consignment) <- File.join(Consignment).on(_.consignmentid === _.consignmentid).filter(_._1.fileid.inSet(fileIds))
    } yield (file.fileid, consignment)
    db.run(query.result)
  }

  def addParentFolder(consignmentId: UUID, parentFolder: Option[String])(implicit executionContext: ExecutionContext): Future[Unit] = {
    val updateAction = Consignment.filter(_.consignmentid === consignmentId).map(c => c.parentfolder).update(parentFolder)
    db.run(updateAction).map(_ => ())
  }

  def getParentFolder(consignmentId: UUID)(implicit executionContext: ExecutionContext): Future[Option[String]] = {
    val query = Consignment.filter(_.consignmentid === consignmentId).map(_.parentfolder)
    db.run(query.result).map(_.headOption.flatten)
  }
}

case class ConsignmentResult(consignment: ConsignmentRow, series: SeriesRow, body: BodyRow)
