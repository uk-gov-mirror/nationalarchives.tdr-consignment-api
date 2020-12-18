package uk.gov.nationalarchives.tdr.api.graphql.fields
import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.marshalling.FromInput
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ListInputType, ObjectType, fields}
import sangria.util.tag.@@
import uk.gov.nationalarchives.tdr.api.auth.ValidateHasChecksumMetadataAccess
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext

object ConsignmentMetadataFields {
  case class ConsignmentMetadata(consignmentid: UUID,
                                 propertyName: UUID,
                                 value: String,
                                 datetime: Long,
                                 metadataId: UUID)

  case class AddConsignmentMetadataInput(consignmentid: UUID,
                                         propertyName: UUID,
                                         value: String)

  implicit val ConsignmentMetadataType: ObjectType[Unit, ConsignmentMetadata] = deriveObjectType[Unit, ConsignmentMetadata]()
  implicit val AddConsignmentMetadataInputType: InputObjectType[AddConsignmentMetadataInput] = deriveInputObjectType[AddConsignmentMetadataInput]()

  val ConsignmentMetadataInputArg: Argument[Seq[AddConsignmentMetadataInput @@ FromInput.InputObjectResult]] =
    Argument("addConsignmentMetadataInput", ListInputType(AddConsignmentMetadataInputType))

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addConsignmentMetadata", ConsignmentMetadataType,
      arguments=ConsignmentMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentMetadataService.addConsignmentMetadata(ctx.arg(ConsignmentMetadataInputArg),
        ctx.ctx.accessToken.userId),
      tags=List(ValidateHasChecksumMetadataAccess)
    ))
}
