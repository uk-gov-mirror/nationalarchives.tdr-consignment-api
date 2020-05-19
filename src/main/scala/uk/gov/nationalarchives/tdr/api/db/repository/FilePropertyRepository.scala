package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Fileproperty, FilepropertyRow}

import scala.concurrent.Future

class FilePropertyRepository(db: Database) {

  def getPropertyByName(propertyName: String): Future[Seq[FilepropertyRow]] = {
    val query = Fileproperty.filter(_.name === propertyName)
    db.run(query.result)
  }
}
