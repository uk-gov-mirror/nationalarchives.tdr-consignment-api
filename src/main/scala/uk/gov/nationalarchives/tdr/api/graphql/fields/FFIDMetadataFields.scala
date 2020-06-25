package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import sangria.schema.{Argument, Field, InputObjectType, ObjectType, fields}
import sangria.macros.derive._
import FieldTypes._
import sangria.marshalling.circe._
import io.circe.generic.auto._
import uk.gov.nationalarchives.tdr.api.auth.ValidateHasFFIDMetadataAccess
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext

object FFIDMetadataFields {
  case class FFIDMetadata(fileId: UUID, software: String, softwareVersion: String, binarySignatureFileVersion: String, containerSignatureFileVersion: String, method: String, extension: Option[String] = None, identificationBasis: String, puid: Option[String], datetime: Long)
  case class FFIDMetadataInput(fileId: UUID, software: String, softwareVersion: String, binarySignatureFileVersion: String, containerSignatureFileVersion: String, method: String, extension: Option[String] = None, identificationBasis: String, puid: Option[String], datetime: Long)
    implicit val AddFFFIDMetadataInputType: InputObjectType[FFIDMetadataInput] = deriveInputObjectType[FFIDMetadataInput]()
    implicit val FFIDMetadataType: ObjectType[Unit, FFIDMetadata] = deriveObjectType[Unit, FFIDMetadata]()

    val FileMetadataInputArg = Argument("addFFIDMetadataInput", AddFFFIDMetadataInputType)

    val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
      Field("addFFIDMetadata", FFIDMetadataType,
        arguments=FileMetadataInputArg :: Nil,
        resolve = ctx => ctx.ctx.ffidMetadataService.addFFIDMetadata(ctx.arg(FileMetadataInputArg)),
        tags=List(ValidateHasFFIDMetadataAccess)
      ))
}
