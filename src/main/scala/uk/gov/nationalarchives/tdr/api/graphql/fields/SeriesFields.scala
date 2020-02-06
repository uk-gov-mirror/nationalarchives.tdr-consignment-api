package uk.gov.nationalarchives.tdr.api.graphql.fields

import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.Arguments.BodyArg
import sangria.macros.derive._
import sangria.schema.{Field, ListType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.graphql.Tags.ValidateBody

object SeriesFields {
  case class Series(seriesid: Long, bodyid: Option[Long] = None, name: Option[String] = None, code: Option[String] = None, description: Option[String] = None)
  implicit val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getSeries", ListType(SeriesType),
      arguments=BodyArg :: Nil,
      resolve = ctx => ctx.ctx.seriesService.getSeries(ctx.arg(BodyArg)),
      tags=List(ValidateBody()))
  )
}
