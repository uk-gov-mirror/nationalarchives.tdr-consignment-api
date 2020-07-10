package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, LongType, ObjectType, OptionType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateSeries, ValidateUserOwnsConsignment}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object ConsignmentFields {
  case class Consignment(consignmentid: Option[UUID] = None, userid: UUID, seriesid: UUID, totalFiles: Int)
  case class AddConsignmentInput(seriesid: UUID)
  case class ConsignmentFileMetadataProgress(processedFiles: Int, totalFiles: Int)

  implicit val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()
  implicit val ConsignmentFileMetadataProgressType: ObjectType[Unit, ConsignmentFileMetadataProgress] =
    deriveObjectType[Unit, ConsignmentFileMetadataProgress]()
  implicit val AddConsignmentInputType: InputObjectType[AddConsignmentInput] = deriveInputObjectType[AddConsignmentInput]()

  val ConsignmentInputArg = Argument("addConsignmentInput", AddConsignmentInputType)
  val ConsignmentIdArg = Argument("consignmentid", UuidType)

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getConsignment", OptionType(ConsignmentType),
      arguments=ConsignmentIdArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentService.getConsignment(ctx.arg(ConsignmentIdArg)),
      tags=List(ValidateUserOwnsConsignment(ConsignmentIdArg))
     ),
    Field("getConsignmentFileMetadataProgress", OptionType(ConsignmentFileMetadataProgressType),
      arguments=ConsignmentIdArg :: Nil,
      resolve = ctx => ctx.ctx.antivirusMetadataService.getFileMetadataProgress(ctx.arg(ConsignmentIdArg)),
      tags=List(ValidateUserOwnsConsignment(ConsignmentIdArg))
    )
  )

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addConsignment", ConsignmentType,
      arguments=ConsignmentInputArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentService.addConsignment(
        ctx.arg(ConsignmentInputArg),
        ctx.ctx.accessToken.userId.map(id => UUID.fromString(id))
      ),
      tags=List(ValidateSeries)
    )
  )
}
