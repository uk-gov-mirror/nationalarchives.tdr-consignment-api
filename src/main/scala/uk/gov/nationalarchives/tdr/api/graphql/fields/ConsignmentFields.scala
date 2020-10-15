package uk.gov.nationalarchives.tdr.api.graphql.fields

import java.util.UUID

import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, Field, InputObjectType, IntType, ObjectType, OptionType, fields}
import uk.gov.nationalarchives.tdr.api.auth.{ValidateSeries, ValidateUserOwnsConsignment}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FieldTypes._
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, DeferFileChecksProgress, DeferTotalFiles}

object ConsignmentFields {
  case class Consignment(consignmentid: Option[UUID] = None, userid: UUID, seriesid: UUID)
  case class AddConsignmentInput(seriesid: UUID)
  case class AntivirusProgress(filesProcessed: Int)
  case class ChecksumProgress(filesProcessed: Int)
  case class FFIDProgress(filesProcessed: Int)
  case class FileChecks(antivirusProgress: AntivirusProgress, checksumProgress: ChecksumProgress, ffidProgress: FFIDProgress)
  case class ParentFolder(parentFolder: Option[String] = None)
  case class TotalFiles(totalFiles: Int)
  case class ParentFolderAndFiles(parentFolder: ParentFolder, totalFiles: TotalFiles)

  implicit val FileChecksType: ObjectType[Unit, FileChecks] =
    deriveObjectType[Unit, FileChecks]()
  implicit val AntivirusProgressType: ObjectType[Unit, AntivirusProgress] =
    deriveObjectType[Unit, AntivirusProgress]()
  implicit val ChecksumProgressType: ObjectType[Unit, ChecksumProgress] =
    deriveObjectType[Unit, ChecksumProgress]()
  implicit val FfidProgressType: ObjectType[Unit, FFIDProgress] =
    deriveObjectType[Unit, FFIDProgress]()
  implicit val ParentFolderAndFilesType: ObjectType[Unit, ParentFolderAndFiles] =
    deriveObjectType[Unit, ParentFolderAndFiles]()
  implicit val ParentFolderType: ObjectType[Unit, ParentFolder] =
    deriveObjectType[Unit, ParentFolder]()
  implicit val TotalFilesType: ObjectType[Unit, TotalFiles] =
    deriveObjectType[Unit, TotalFiles]()

  implicit val ConsignmentType: ObjectType[Unit, Consignment] = ObjectType(
    "Consignment",
    fields[Unit, Consignment](
      Field("consignmentid", OptionType(UuidType), resolve = _.value.consignmentid),
      Field("userid", UuidType, resolve = _.value.userid),
      Field("seriesid", UuidType, resolve = _.value.seriesid),
      Field(
        "totalFiles",
        IntType,
        resolve = context => DeferTotalFiles(context.value.consignmentid)
      ),
      Field(
        "fileChecks",
        FileChecksType,
        resolve = context => DeferFileChecksProgress(context.value.consignmentid)
      )
    )
  )

  implicit val AddConsignmentInputType: InputObjectType[AddConsignmentInput] = deriveInputObjectType[AddConsignmentInput]()

  val ConsignmentInputArg: Argument[AddConsignmentInput] = Argument("addConsignmentInput", AddConsignmentInputType)
  val ConsignmentIdArg: Argument[UUID] = Argument("consignmentid", UuidType)

  val queryFields: List[Field[ConsignmentApiContext, Unit]] = fields[ConsignmentApiContext, Unit](
    Field("getConsignment", OptionType(ConsignmentType),
      arguments=ConsignmentIdArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentService.getConsignment(ctx.arg(ConsignmentIdArg)),
      tags=List(ValidateUserOwnsConsignment(ConsignmentIdArg))
     ),
    Field("getParentFolderAndFiles", OptionType(ParentFolderAndFilesType),
      arguments=ConsignmentIdArg :: Nil,
      resolve = ctx => ctx.ctx.consignmentService.getConsignmentParentFolderAndFiles(ctx.arg(ConsignmentIdArg)),
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
