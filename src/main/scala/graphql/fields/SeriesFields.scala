package graphql.fields

import graphql.ConsignmentApiContext
import sangria.macros.derive._
import sangria.schema.{Field, ListType, ObjectType, fields}

object SeriesFields {
  case class Series(seriesid: Long, bodyid: Option[Long] = None, name: Option[String] = None, code: Option[String] = None, description: Option[String] = None)
  implicit val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getSeries", ListType(SeriesType), resolve = _.ctx.seriesService.getSeries())
  )

}
