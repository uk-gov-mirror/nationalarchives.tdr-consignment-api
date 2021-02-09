package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.time.ZonedDateTime
import java.util.UUID

import sangria.ast.StringValue
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation
import uk.gov.nationalarchives.tdr.api.utils.TimeUtils.ZonedDateTimeUtils

import scala.util.{Failure, Success, Try}

object FieldTypes {
  private case object UuidCoercionViolation extends ValueCoercionViolation("Valid UUID expected")
  private case object ZonedDateTimeCoercionViolation extends ValueCoercionViolation("Valid Zoned Date Time expected")

  private def parseUuid(s: String): Either[ValueCoercionViolation, UUID] = Try(UUID.fromString(s)) match {
    case Success(uuid) => Right(uuid)
    case Failure(_) => Left(UuidCoercionViolation)
  }

  private def parseZonedDatetime(s: String): Either[ValueCoercionViolation, ZonedDateTime] = Try(ZonedDateTime.parse(s)) match {
    case Success(zonedDateTime) => Right(zonedDateTime)
    case Failure(_) => Left(ZonedDateTimeCoercionViolation)
  }

  implicit val ZonedDateTimeType: ScalarType[ZonedDateTime] = ScalarType[ZonedDateTime]("ZonedDateTime",
    coerceOutput = (zdt, _) => zdt.toSecondsPrecisionString,
    coerceUserInput = {
      case s: String => parseZonedDatetime(s)
      case _ => Left(ZonedDateTimeCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseZonedDatetime(s)
      case _ => Left(ZonedDateTimeCoercionViolation)
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
