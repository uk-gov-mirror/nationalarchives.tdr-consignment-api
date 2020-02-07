package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.MySQLProfile.api._
import uk.gov.nationalarchives.Tables._

import scala.concurrent.Future

class SeriesRepository(db: Database) {

  def getSeries(): Future[Seq[SeriesRow]] = {
    val query = for {
      (series, _) <- Series.join(Body).on(_.bodyid === _.bodyid )
    } yield series
    db.run(query.result)
  }

  def getSeries(bodyName: String): Future[Seq[SeriesRow]] = {
    val query = for {
      (series, _) <- Series.join(Body).on(_.bodyid === _.bodyid).filter(_._2.name === bodyName)
    } yield series
    print(query.result.statements)
    db.run(query.result)

  }
}
