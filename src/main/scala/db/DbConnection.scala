package db

import slick.jdbc.MySQLProfile.api._

object DbConnection {
  val db = Database.forConfig("consignmentapi")
}
