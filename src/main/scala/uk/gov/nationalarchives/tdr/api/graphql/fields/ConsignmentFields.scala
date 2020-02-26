package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object ConsignmentFields {
  case class Consignment(consignmentid: Option[Long] = None, userid: UUID, seriesid: Long)
  case class AddConsignmentInput(seriesid: Long, userid: UUID)

  implicit val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()
  implicit val AddConsignmentInputType: InputObjectType[AddConsignmentInput] = deriveInputObjectType[AddConsignmentInput]()

  val ConsignmentInputArg = Argument("addConsignmentInput", AddConsignmentInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addConsignment", ConsignmentType,
      arguments=ConsignmentInputArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentService.addConsignment(ctx.arg(ConsignmentInputArg)))
  )
}