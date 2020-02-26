package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import sangria.marshalling.circe._
import io.circe.generic.auto._
import sangria.ast.StringValue
import sangria.schema.ScalarType._
import sangria.macros.derive._
import sangria.schema.{Argument, Field, InputObjectType, ListType, ObjectType, ScalarType, fields}
import sangria.validation.ValueCoercionViolation
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext

import scala.util.{Failure, Success, Try}

object ConsignmentFields {
  private case object UuidCoercionViolation extends ValueCoercionViolation("Valid UUID expected")

  private def parseUuid(s: String): Either[ValueCoercionViolation, UUID] = Try(UUID.fromString(s)) match {
    case Success(uuid) => Right(uuid)
    case Failure(_) => Left(UuidCoercionViolation)
  }

  implicit private val UuidType: ScalarType[UUID] = ScalarType[UUID]("UUID",
    coerceOutput = (u, _) => u.toString,
    coerceUserInput = {
      case s: String => parseUuid(s)
      case _ => Left(UuidCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseUuid(s)
      case _ => Left(UuidCoercionViolation)
    }
  )

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
