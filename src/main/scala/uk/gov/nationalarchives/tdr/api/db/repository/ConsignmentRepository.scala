package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.MySQLProfile.api._
import uk.gov.nationalarchives.Tables.{ConsignmentRow, Consignment}

import scala.concurrent.Future

class ConsignmentRepository(db: Database) {
  private val insertQuery = Consignment returning Consignment.map(_.consignmentid) into
    ((consignment, consignmentid) => consignment.copy(consignmentid = Some(consignmentid)))

  def addConsignment(consignmentRow: ConsignmentRow): Future[ConsignmentRow] = {
    db.run(insertQuery += consignmentRow)
  }
}
