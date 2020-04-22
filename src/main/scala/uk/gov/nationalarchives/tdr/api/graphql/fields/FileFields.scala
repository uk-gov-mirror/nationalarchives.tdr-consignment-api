package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateNoPreviousUploadForConsignment, ValidateUserOwnsConsignment}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object FileFields {
  case class Files(fileIds: Seq[UUID])

  case class AddFilesInput(consignmentId: UUID, numberOfFiles: Int) extends UserOwnsConsignment
  implicit val AddFilesInputType: InputObjectType[AddFilesInput] = deriveInputObjectType[AddFilesInput]()
  implicit val FileType: ObjectType[Unit, Files]  = deriveObjectType[Unit, Files]()
  private val FileInputArg = Argument("addFilesInput", AddFilesInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field(
      "addFiles",
      FileType,
      arguments = List(FileInputArg),
      resolve = ctx => ctx.ctx.fileService.addFile(ctx.arg(FileInputArg), ctx.ctx.accessToken.userId.map(id => UUID.fromString(id))),
      tags=List(ValidateUserOwnsConsignment(FileInputArg), ValidateNoPreviousUploadForConsignment(FileInputArg))
    )
  )
}
