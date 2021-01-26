package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.time.LocalDateTime
import java.util.UUID

import sangria.ast.StringValue
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

import scala.util.{Failure, Success, Try}

object FieldTypes {
  private case object UuidCoercionViolation extends ValueCoercionViolation("Valid UUID expected")
  private case object LocalDateTimeCoercionViolation extends ValueCoercionViolation("Valid Local Date Time expected")

  private def parseUuid(s: String): Either[ValueCoercionViolation, UUID] = Try(UUID.fromString(s)) match {
    case Success(uuid) => Right(uuid)
    case Failure(_) => Left(UuidCoercionViolation)
  }

  private def parseDate(s: String): Either[ValueCoercionViolation, LocalDateTime] = Try(LocalDateTime.parse(s)) match {
    case Success(date) => Right(date)
    case Failure(_) => Left(LocalDateTimeCoercionViolation)
  }

  implicit val LocalDateTimeType: ScalarType[LocalDateTime] = ScalarType[LocalDateTime]("LocalDateTime",
    coerceOutput = (d, _) => d.toString,
    coerceUserInput = {
      case s: String => parseDate(s)
      case _ => Left(LocalDateTimeCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseDate(s)
      case _ => Left(LocalDateTimeCoercionViolation)
    }
  )

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
}
