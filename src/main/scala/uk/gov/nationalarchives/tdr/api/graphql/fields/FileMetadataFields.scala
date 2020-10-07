package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.ValidateHasChecksumMetadataAccess
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import FieldTypes._

object FileMetadataFields {

  val SHA256ServerSideChecksum = "SHA256ServerSideChecksum"
  case class FileMetadata(filePropertyName: String, fileId: UUID, value: String)
  case class AddFileMetadataInput(filePropertyName: String, fileId: UUID, value: String)

  implicit val FileMetadataType: ObjectType[Unit, FileMetadata] = deriveObjectType[Unit, FileMetadata]()
  implicit val AddFileMetadataInputType: InputObjectType[AddFileMetadataInput] = deriveInputObjectType[AddFileMetadataInput]()

  val FileMetadataInputArg: Argument[AddFileMetadataInput] = Argument("addFileMetadataInput", AddFileMetadataInputType)

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addFileMetadata", FileMetadataType,
      arguments=FileMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.fileMetadataService.addFileMetadata(ctx.arg(FileMetadataInputArg), ctx.ctx.accessToken.userId.map(id => UUID.fromString(id))),
      tags=List(ValidateHasChecksumMetadataAccess)
    ))
}
