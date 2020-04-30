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

object AVMetadataFields {
  case class AVMetadata(fileId: UUID,
                          software: Option[String] = None,
                          value: Option[String] = None,
                          softwareVersion: Option[String] = None,
                          databaseVersion: Option[String] = None,
                          result: Option[String] = None,
                          datetime: Long)

  case class AddAVMetadataInput(fileId: UUID,
                                 software: Option[String] = None,
                                 value: Option[String] = None,
                                 softwareVersion: Option[String] = None,
                                 databaseVersion: Option[String] = None,
                                 result: Option[String] = None,
                                 datetime: Long) extends UserOwnsFile

  implicit val AVMetadataType: ObjectType[Unit, AVMetadata] = deriveObjectType[Unit, AVMetadata]()
  implicit val AddAVMetadataInputType: InputObjectType[AddAVMetadataInput] = deriveInputObjectType[AddAVMetadataInput]()

  val AVMetadataInputArg = Argument("addAVMetadataInput", ListInputType(AddAVMetadataInputType))

  val mutationFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("addAVMetadata", ListType(AVMetadataType),
      arguments=AVMetadataInputArg :: Nil,
      resolve = ctx => ctx.ctx.avMetadataService.addAVMetadata(ctx.arg(AVMetadataInputArg)),
      tags=List(ValidateUserOwnsFiles(AVMetadataInputArg))
    ))
}
