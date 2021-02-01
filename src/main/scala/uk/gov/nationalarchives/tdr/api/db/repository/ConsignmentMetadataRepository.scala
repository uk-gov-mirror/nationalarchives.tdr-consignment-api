package uk.gov.nationalarchives.tdr.api.db.repository

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Consignmentmetadata, _}

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentMetadataRepository(db: Database)(implicit val executionContext: ExecutionContext) {
  def addConsignmentMetadata(consignmentMetadataRows: Seq[ConsignmentmetadataRow])(implicit executionContext: ExecutionContext): Future[Unit] = {
    db.run(Consignmentmetadata ++= consignmentMetadataRows).map(_ => ())
  }

  def getConsignmentMetadata(consignmentId: UUID, propertyName: String*): Future[Seq[ConsignmentmetadataRow]] = {
    val query = Consignmentmetadata
      .filter(_.consignmentid === consignmentId)
      .filter(_.propertyname inSet propertyName.toSet)
    db.run(query.result)
  }
}
