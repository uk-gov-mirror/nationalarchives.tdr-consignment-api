package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.ValidateUserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes.UuidType
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment

object TransferConfirmationFields {

  case class TransferConfirmation(consignmentId: UUID,
                                  finalOpenRecordsConfirmed: Boolean,
                                  legalOwnershipTransferConfirmed: Boolean
                                 )

  case class AddTransferConfirmationInput(consignmentId: UUID,
                                          finalOpenRecordsConfirmed: Boolean,
                                          legalOwnershipTransferConfirmed: Boolean
                                         ) extends UserOwnsConsignment

  implicit val TransferConfirmationType: ObjectType[Unit, TransferConfirmation] = deriveObjectType[Unit, TransferConfirmation]()
  implicit val AddTransferConfirmationInputType: InputObjectType[AddTransferConfirmationInput] = deriveInputObjectType[AddTransferConfirmationInput]()

  val TransferConfirmationInputArg: Argument[AddTransferConfirmationInput] =
    Argument("addTransferConfirmationInput", AddTransferConfirmationInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addTransferConfirmation", TransferConfirmationType,
      arguments = TransferConfirmationInputArg :: Nil,
      resolve = ctx => ctx.ctx.transferConfirmationService.addTransferConfirmation(ctx.arg(TransferConfirmationInputArg),
        ctx.ctx.accessToken.userId),
      tags = List(ValidateUserOwnsConsignment(TransferConfirmationInputArg))
    ))
}
