package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.marshalling.FromInput
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ListInputType, ObjectType, fields}
import sangria.util.tag.@@
import uk.gov.nationalarchives.tdr.api.auth.ValidateUserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object TransferConfirmationFields {

  case class TransferConfirmation(consignmentid: UUID,
                                  finalOpenRecordsConfirmed: Boolean,
                                  legalOwnershipTransferConfirmed: Boolean
                                 )

  case class AddTransferConfirmationInput(consignmentid: UUID,
                                          finalOpenRecordsConfirmed: Boolean,
                                          legalOwnershipTransferConfirmed: Boolean
                                         )

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
