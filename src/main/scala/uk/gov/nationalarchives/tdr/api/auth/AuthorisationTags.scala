package uk.gov.nationalarchives.tdr.api.auth

import java.util.UUID

import sangria.execution.BeforeFieldResult
import sangria.schema.{Argument, Context}
import uk.gov.nationalarchives.tdr.api.auth.ValidateHasChecksumMetadataAccess.{checksumRole, continue}
import uk.gov.nationalarchives.tdr.api.auth.ValidateUserOwnsFiles.continue
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.AddClientFileMetadataInput
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.AddConsignmentInput
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields._
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, ValidationTag}
import uk.gov.nationalarchives.tdr.api.service.FileService
import uk.gov.nationalarchives.tdr.keycloak.Token

import scala.concurrent._
import scala.language.postfixOps

trait AuthorisationTag extends ValidationTag {
  val antiVirusRole = "antivirus"
  val checksumRole = "checksum"
  val clientFileMetadataRole = "client_file_metadata"
  val fileFormatRole = "file_format"
  val exportRole = "export"
}

trait SyncAuthorisationTag extends AuthorisationTag {
  final def validateAsync(ctx: Context[ConsignmentApiContext, _])
                         (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    Future.successful(validateSync(ctx))
  }

  def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit]
}

object ValidateBody extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken

    val bodyArg: String = ctx.arg("body")
    val bodyFromToken: String = token.transferringBody.getOrElse("")

    if (bodyFromToken != bodyArg) {
      val msg = s"Body for user ${token.userId} was $bodyArg in the query and $bodyFromToken in the token"
      throw AuthorisationException(msg)
    }
    continue
  }
}

object ValidateSeries extends AuthorisationTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                            (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val userBody = token.transferringBody.getOrElse(
      throw AuthorisationException(s"No transferring body in user token for user '${token.userId}'"))

    val addConsignmentInput = ctx.arg[AddConsignmentInput]("addConsignmentInput")
    val bodyResult = ctx.ctx.transferringBodyService.getBody(addConsignmentInput.seriesid)

    bodyResult.map(body => {
      body.code match {
        case Some(code) if code == userBody => continue
        case Some(code) =>
          val message = s"User '${token.userId}' is from transferring body '$userBody' and does not have permission " +
            s"to create a consignment under series '$addConsignmentInput' owned by body '$code'"
          throw AuthorisationException(message)
        // This exception can be removed when we use body IDs rather than names
        case _ => throw new IllegalStateException("")
      }
    })
  }
}

case class ValidateUserOwnsConsignment[T](argument: Argument[T]) extends AuthorisationTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                            (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val userId = token.userId

    val arg: T = ctx.arg[T](argument.name)
    val consignmentId: UUID = arg match {
      case uoc: UserOwnsConsignment => uoc.consignmentId
      case id: UUID => id
    }

    ctx.ctx.consignmentService
      .getConsignment(consignmentId)
      .map(consignment => {
        if (consignment.isEmpty) {
          throw AuthorisationException("Invalid consignment id")
        }

        if (consignment.get.userid == userId) {
          continue
        } else {
          throw AuthorisationException(s"User '$userId' does not own consignment '$consignmentId'")
        }
      })
  }
}

@deprecated("This is only used for the client file metadata fields which are being deleted")
object ValidateUserOwnsFiles extends AuthorisationTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                            (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val tokenUserId = token.userId

    val queryInput = ctx.arg[Seq[AddClientFileMetadataInput]]("addClientFileMetadataInput")

    val fileIds = queryInput.map(_.fileId)
    ctx.ctx.fileService
      .getOwnersOfFiles(fileIds)
      .map(fileOwnership => {
        val otherUsersFiles = fileOwnership.filter(_.userId != tokenUserId)

        otherUsersFiles match {
          case Nil => continue
          case files => throw AuthorisationException(
            s"User '$tokenUserId' does not have permission to updatefiles: ${files.map(_.fileId)}")
        }
      })
  }
}

object ValidateHasAntiVirusMetadataAccess extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken
    val antivirusAccess = token.backendChecksRoles.contains(antiVirusRole)

    if (antivirusAccess) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to update antivirus metadata")
    }
  }
}

object ValidateAddFileMetadataAccess extends AuthorisationTag {
  def hasChecksumMetadataAccess(token: Token) =
    if (token.backendChecksRoles.contains(checksumRole)) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to update checksum metadata")
    }

  def userOwnsFiles(token: Token, fileService: FileService, fileIds: List[UUID])(implicit executionContext: ExecutionContext) = {
    val tokenUserId = token.userId

    fileService
      .getOwnersOfFiles(fileIds)
      .map(fileOwnership => {
        val otherUsersFiles = fileOwnership.filter(_.userId != tokenUserId)

        otherUsersFiles match {
          case Nil => continue
          case files => throw AuthorisationException(
            s"User '$tokenUserId' does not have permission to updatefiles: ${files.map(_.fileId)}")
        }
      })
  }

  override def validateAsync(ctx: Context[ConsignmentApiContext, _])(implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val clientFileMetadataProperties = List(ClientSideOriginalFilepath, ClientSideFileSize, ClientSideFileLastModifiedDate, SHA256ClientSideChecksum)
    val inputs = ctx.arg[Seq[AddFileMetadataInput]]("addFileMetadataInputs")
    val clientFileMetadataInputs = inputs.filter(p => clientFileMetadataProperties.contains(p))
    if (inputs.exists(p => p.filePropertyName == SHA256ServerSideChecksum)) {
      Future(hasChecksumMetadataAccess(ctx.ctx.accessToken))
    } else if (clientFileMetadataInputs.nonEmpty) {
      userOwnsFiles(ctx.ctx.accessToken, ctx.ctx.fileService, clientFileMetadataInputs.map(_.fileId).toList)
    } else {
      throw AuthorisationException(s"User ${ctx.ctx.accessToken.userId} does not have permission to add file metadata")
    }
  }
}

@deprecated("This is only used for the single argument addFileMetadata field which will be deleted")
object ValidateHasChecksumMetadataAccess extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken
    val checksumAccess = token.backendChecksRoles.contains(checksumRole)

    if (checksumAccess) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to update checksum metadata")
    }
  }
}

object ValidateHasClientFileMetadataAccess extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken
    val clientFileMetadataAccess = token.backendChecksRoles.contains(clientFileMetadataRole)
    val fileId = ctx.arg[UUID]("fileId")

    if (clientFileMetadataAccess) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to access the client file metadata for file $fileId")
    }
  }
}

object ValidateHasFFIDMetadataAccess extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken
    val fileFormatAccess = token.backendChecksRoles.contains(fileFormatRole)
    if (fileFormatAccess) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to update file format metadata")
    }
  }
}

object ValidateHasExportAccess extends SyncAuthorisationTag {
  override def validateSync(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
    val token = ctx.ctx.accessToken
    val exportAccess = token.backendChecksRoles.contains(exportRole)
    if (exportAccess) {
      continue
    } else {
      val tokenUserId = token.userId
      throw AuthorisationException(s"User '$tokenUserId' does not have permission to export the files")
    }
  }
}
