package uk.gov.nationalarchives.tdr.api.graphql.fields

object ConsignmentFields {

  case class Consignment(consignmentid: Option[Long] = None, userid: Long, seriesid: Long)

}
