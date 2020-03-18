package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import sangria.ast.StringValue
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

import scala.util.{Failure, Success, Try}

object FieldTypes {
  private case object UuidCoercionViolation extends ValueCoercionViolation("Valid UUID expected")
  private case object BigDecimalCoercionViolation extends ValueCoercionViolation("Valid BigDecimal expected")

  private def parseUuid(s: String): Either[ValueCoercionViolation, UUID] = Try(UUID.fromString(s)) match {
    case Success(uuid) => Right(uuid)
    case Failure(_) => Left(UuidCoercionViolation)
  }

  implicit val UuidType: ScalarType[UUID] = ScalarType[UUID]("UUID",
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

  private def parseBigDecimal(s: String): Either[ValueCoercionViolation, BigDecimal] =
    Try(BigDecimal(s)) match {
      case Success(bigDecimal) => Right(bigDecimal)
      case Failure(_) => Left(BigDecimalCoercionViolation)
    }

  implicit val BigDecimalType: ScalarType[BigDecimal] = ScalarType[BigDecimal]("BigDecimal",
    coerceOutput = (bd, _) => bd.toString,
    coerceUserInput = {
      case s: String => parseBigDecimal(s)
      case _ => Left(BigDecimalCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseBigDecimal(s)
      case _ => Left(BigDecimalCoercionViolation)
    }
  )
}
