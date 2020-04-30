package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, ListInputType, ListType, ObjectType, fields}
import uk.gov.nationalarchives.tdr.api.auth.ValidateUserOwnsFiles
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsFile

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
                                        datetime: Long) extends UserOwnsFile

  implicit val ClientFileMetadataType: ObjectType[Unit, ClientFileMetadata] = deriveObjectType[Unit, ClientFileMetadata]()
  implicit val AddClientFileMetadataInputType: InputObjectType[AddClientFileMetadataInput] = deriveInputObjectType[AddClientFileMetadataInput]()

  val ClientFileMetadataInputArg = Argument("addClientFileMetadataInput", ListInputType(AddClientFileMetadataInputType))

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addClientFileMetadata", ListType(ClientFileMetadataType),
      arguments=ClientFileMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.clientFileMetadataService.addClientFileMetadata(ctx.arg(ClientFileMetadataInputArg)),
      tags=List(ValidateUserOwnsFiles(ClientFileMetadataInputArg))
    ))
}
