package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Consignmentmetadata, ConsignmentmetadataRow}

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentMetadataRepository(db: Database) {
  def addConsignmentMetadata(consignmentMetadataRows: Seq[ConsignmentmetadataRow])(implicit executionContext: ExecutionContext): Future[Unit] = {
    db.run(Consignmentmetadata ++= consignmentMetadataRows).map(_ => ())
  }
}
