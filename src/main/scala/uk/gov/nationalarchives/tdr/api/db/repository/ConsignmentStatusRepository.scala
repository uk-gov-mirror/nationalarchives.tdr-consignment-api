package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Consignmentstatus, ConsignmentstatusRow}

import java.util.UUID
import scala.concurrent.Future

class ConsignmentStatusRepository(db: Database) {

  def getConsignmentStatus(consignmentId: UUID): Future[Seq[ConsignmentstatusRow]] = {
    val query = Consignmentstatus.filter(_.consignmentid === consignmentId)
    db.run(query.result)
  }
}
