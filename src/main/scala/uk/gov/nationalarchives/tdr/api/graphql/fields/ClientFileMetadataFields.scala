package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ListInputType, ListType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateHasFFIDMetadataAccess, ValidateUserOwnsFiles}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._

object ClientFileMetadataFields {
  case class ClientFileMetadata(fileId: UUID,
                                originalPath: Option[String] = None,
                                checksum: Option[String] = None,
                                checksumType: Option[String] = None,
                                lastModified: Long,
                                createdDate: Long,
                                fileSize: Option[Long] = None,
                                datetime: Long,
                                clientFileMetadataId: UUID)

  case class AddClientFileMetadataInput(fileId: UUID,
                                        originalPath: Option[String] = None,
                                        checksum: Option[String] = None,
                                        checksumType: Option[String] = None,
                                        lastModified: Long,
                                        createdDate: Long,
                                        fileSize: Option[Long] = None,
                                        datetime: Long)

  implicit val ClientFileMetadataType: ObjectType[Unit, ClientFileMetadata] = deriveObjectType[Unit, ClientFileMetadata]()
  implicit val AddClientFileMetadataInputType: InputObjectType[AddClientFileMetadataInput] = deriveInputObjectType[AddClientFileMetadataInput]()

  val ClientFileMetadataInputArg = Argument("addClientFileMetadataInput", ListInputType(AddClientFileMetadataInputType))
  val FileIdArg = Argument("fileId", UuidType)

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getClientFileMetadata", ClientFileMetadataType,
      arguments=FileIdArg :: Nil,
      resolve = ctx => ctx.ctx.clientFileMetadataService.getClientFileMetadata(ctx.arg(FileIdArg)),
      tags=ValidateHasFFIDMetadataAccess :: Nil
      //We're only using this for the file metadata api update lambda for now. This check can be changed if we use it anywhere else
    ))

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addClientFileMetadata", ListType(ClientFileMetadataType),
      arguments=ClientFileMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.clientFileMetadataService.addClientFileMetadata(ctx.arg(ClientFileMetadataInputArg)),
      tags=List(ValidateUserOwnsFiles(ClientFileMetadataInputArg))
    ))
}
