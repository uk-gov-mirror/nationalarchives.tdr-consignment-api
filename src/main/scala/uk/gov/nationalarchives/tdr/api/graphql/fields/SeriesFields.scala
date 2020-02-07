package uk.gov.nationalarchives.tdr.api.graphql.fields

import cats.syntax.either._
import io.circe.Decoder
import io.circe.generic.auto._
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.validation.ValueCoercionViolation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import sangria.macros.derive._
import sangria.schema.{Argument, Field, InputObjectType, ListType, ObjectType, fields}
import sangria.marshalling.circe._

object SeriesFields {
  case class Series(bodyid: Option[Long] = None, name: Option[String] = None,
                    code: Option[String] = None, description: Option[String] = None, seriesid: Option[Long] = None)
  case class AddSeriesInput(bodyid: Option[Long] = None,
                            name: Option[String] = None, code: Option[String] = None, description: Option[String] = None)

  implicit val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()
  implicit val AddSeriesInputType: InputObjectType[AddSeriesInput] = deriveInputObjectType[AddSeriesInput]()

  private val SeriesInputArg = Argument("addSeriesInput", AddSeriesInputType)

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getSeries", ListType(SeriesType), resolve = _.ctx.seriesService.getSeries())
  )

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field(
      "addSeries",
      SeriesType,
      arguments = List(SeriesInputArg),
      resolve = ctx => ctx.ctx.seriesService.addSeries(ctx.arg(SeriesInputArg)))
    )
}
