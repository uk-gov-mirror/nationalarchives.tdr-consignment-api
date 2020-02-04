package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.MySQLProfile.api._
import uk.gov.nationalarchives.Tables._

import scala.concurrent.Future

class SeriesRepository(db: Database) {

  def getSeries(): Future[Seq[SeriesRow]] = {
    db.run(Series.result)
  }
}
