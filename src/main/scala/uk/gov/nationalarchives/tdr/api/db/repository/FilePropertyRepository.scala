package uk.gov.nationalarchives.tdr.api.db.repository

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.Tables.{Fileproperty, FilepropertyRow}

import scala.concurrent.{ExecutionContext, Future}

class FilePropertyRepository(db: Database)(implicit val executionContext: ExecutionContext) {

  def getPropertiesByName(propertyNames: List[String]) = {
    val query = Fileproperty.filter(_.name inSet propertyNames)
    db.run(query.result)
  }

  def getPropertyByName(propertyName: String): Future[Option[FilepropertyRow]] = {
    val query = Fileproperty.filter(_.name === propertyName)
    db.run(query.result).map(_.headOption)
  }
}
