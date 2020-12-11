package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ListInputType, ListType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateAddFileMetadataAccess, ValidateHasChecksumMetadataAccess}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import FieldTypes._
import sangria.marshalling.FromInput
import sangria.marshalling.FromInput.InputObjectResult
import sangria.util.tag.@@

object FileMetadataFields {

  val SHA256ServerSideChecksum = "SHA256ServerSideChecksum"
  val SHA256ClientSideChecksum = "SHA256ClientSideChecksum"
  val ClientSideOriginalFilepath = "ClientSideOriginalFilepath"
  val ClientSideFileLastModifiedDate = "ClientSideFileLastModifiedDate"
  val ClientSideFileSize = "ClientSideFileSize"

  case class FileMetadata(filePropertyName: String, fileId: UUID, value: String)
  case class AddFileMetadataInput(filePropertyName: String, fileId: UUID, value: String)

  implicit val FileMetadataType: ObjectType[Unit, FileMetadata] = deriveObjectType[Unit, FileMetadata]()
  implicit val AddFileMetadataInputType: InputObjectType[AddFileMetadataInput] = deriveInputObjectType[AddFileMetadataInput]()

  val FileMetadataMultipleInputArg: Argument[Seq[AddFileMetadataInput @@ InputObjectResult]] = Argument("addFileMetadataInputs", ListInputType(AddFileMetadataInputType))
  val FileMetadataInputArg: Argument[AddFileMetadataInput] = Argument("addFileMetadataInput", AddFileMetadataInputType)


  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addFileMetadata", FileMetadataType,
      arguments=FileMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.fileMetadataService.addFileMetadata(ctx.arg(FileMetadataInputArg), ctx.ctx.accessToken.userId),
      tags=List(ValidateHasChecksumMetadataAccess),
      deprecationReason = Option("Use addFileMetadata(a: [AddFileMetadataInput]!) instead to reduce front end network requests")
    ),
    Field("addFileMetadata", ListType(FileMetadataType),
      arguments=FileMetadataMultipleInputArg :: Nil,
      resolve = ctx => ctx.ctx.fileMetadataService.addFileMetadata(ctx.arg(FileMetadataMultipleInputArg), ctx.ctx.accessToken.userId),
      tags = ValidateAddFileMetadataAccess :: Nil
    )
  )
}
