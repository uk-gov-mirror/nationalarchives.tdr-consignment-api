package uk.gov.nationalarchives.tdr.api.graphql.fields

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.Tags.{ValidateIsAdmin, ValidateUserOwnsConsignment}

object TransferAgreementFields {
  case class TransferAgreement(consignmentId: Long,
                               allPublicRecords: Option[Boolean] = None,
                               allCrownCopyright: Option[Boolean] = None,
                               allEnglish: Option[Boolean] = None,
                               allDigital: Option[Boolean] = None,
                               appraisalSelectionSignedOff: Option[Boolean] = None,
                               sensitivityReviewSignedOff: Option[Boolean] = None,
                               transferAgreementId: Option[Long] = None)

  case class AddTransferAgreementInput(consignmentId: Long,
                                       allPublicRecords: Option[Boolean] = None,
                                       allCrownCopyright: Option[Boolean] = None,
                                       allEnglish: Option[Boolean] = None,
                                       allDigital: Option[Boolean] = None,
                                       appraisalSelectionSignedOff: Option[Boolean] = None,
                                       sensitivityReviewSignedOff: Option[Boolean] = None)

  implicit val TransferAgreementType: ObjectType[Unit, TransferAgreement] = deriveObjectType[Unit, TransferAgreement]()
  implicit val AddTransferAgreementInputType: InputObjectType[AddTransferAgreementInput] = deriveInputObjectType[AddTransferAgreementInput]()

  val TransferAgreementInputArg = Argument("addTransferAgreementInput", AddTransferAgreementInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addTransferAgreement", TransferAgreementType,
      arguments=TransferAgreementInputArg :: Nil,
      resolve = ctx => ctx.ctx.transferAgreementService.addTransferAgreement(ctx.arg(TransferAgreementInputArg)),
      tags=List(ValidateUserOwnsConsignment())
    ))
}
