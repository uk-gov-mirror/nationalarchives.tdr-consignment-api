package uk.gov.nationalarchives.tdr.api.db

import akka.stream.alpakka.slick.javadsl.SlickSession

object DbConnection {
  val db = SlickSession.forConfig("consignmentapi").db
}
