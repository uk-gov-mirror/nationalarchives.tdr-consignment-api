package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.Tags.ValidateUserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment

object FileFields {
  case class File(fileIds: Seq[Long])

  case class AddFileInput(consignmentId: Long, numberOfFiles: Option[Int]) extends UserOwnsConsignment
  implicit val AddFileInputType: InputObjectType[AddFileInput] = deriveInputObjectType[AddFileInput]()
  implicit val FileType: ObjectType[Unit, File]  = deriveObjectType[Unit, File]()
  private val FileInputArg = Argument("addFileInput", AddFileInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field(
      "addFiles",
      FileType,
      arguments = List(FileInputArg),
      resolve = ctx => ctx.ctx.fileService.addFile(ctx.arg(FileInputArg), ctx.ctx.accessToken.userId.map(id => UUID.fromString(id))),
      tags=List(ValidateUserOwnsConsignment(FileInputArg))
    )
  )
}
