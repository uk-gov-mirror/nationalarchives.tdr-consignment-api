package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateHasExportAccess, ValidateUserOwnsConsignment}
import uk.gov.nationalarchives.tdr.api.consignmentstatevalidation.ValidateNoPreviousUploadForConsignment
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object FileFields {
  case class Files(fileIds: Seq[UUID])

  case class AddFilesInput(consignmentId: UUID, numberOfFiles: Int, parentFolder: Option[String]) extends UserOwnsConsignment
  implicit val AddFilesInputType: InputObjectType[AddFilesInput] = deriveInputObjectType[AddFilesInput]()
  implicit val FileType: ObjectType[Unit, Files]  = deriveObjectType[Unit, Files]()
  private val FileInputArg = Argument("addFilesInput", AddFilesInputType)
  private val ConsignmentIdArg: Argument[UUID] = Argument("consignmentid", UuidType)

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field(
      "getFiles",
      FileType,
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.fileService.getFiles(ctx.arg(ConsignmentIdArg)),
      tags=List(ValidateHasExportAccess)
    )
  )

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field(
      "addFiles",
      FileType,
      arguments = List(FileInputArg),
      resolve = ctx => ctx.ctx.fileService.addFile(ctx.arg(FileInputArg), ctx.ctx.accessToken.userId),
      tags=List(ValidateUserOwnsConsignment(FileInputArg), ValidateNoPreviousUploadForConsignment)
    )
  )
}
