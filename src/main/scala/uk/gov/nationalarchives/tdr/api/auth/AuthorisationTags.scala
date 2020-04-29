package uk.gov.nationalarchives.tdr.api.auth

import java.util.UUID

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.{Argument, Context}
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, ValidationTag}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.AddClientFileMetadataInput
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.AddConsignmentInput
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment

import scala.concurrent._
import scala.language.postfixOps

trait AuthorisationTag extends ValidationTag

trait SyncAuthorisationTag extends AuthorisationTag {
  final def validate(ctx: Context[ConsignmentApiContext, _])
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

    if(bodyFromToken != bodyArg) {
      val msg = s"Body for user ${token.userId.getOrElse("")} was $bodyArg in the query and $bodyFromToken in the token"
      throw AuthorisationException(msg)
    }
    continue
  }
}

object ValidateSeries extends AuthorisationTag {
  override def validate(ctx: Context[ConsignmentApiContext, _])
                       (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val userBody = token.transferringBody.getOrElse(
      throw new AuthorisationException(s"No transferring body in user token for user '${token.userId.getOrElse("")}'"))

    val addConsignmentInput = ctx.arg[AddConsignmentInput]("addConsignmentInput")
    val bodyResult = ctx.ctx.transferringBodyService.getBody(addConsignmentInput.seriesid)

    bodyResult.map(body => {
      body.name match {
        case Some(name) if name == userBody => continue
        case Some(name) => {
          val message = s"User '${token.userId}' is from transferring body '$userBody' and does not have permission " +
            s"to create a consignment under series '$addConsignmentInput' owned by body '$name'"
          throw AuthorisationException(message)
        }
        // This exception can be removed when we use body IDs rather than names
        case _ => throw new IllegalStateException("")
      }
    })
  }
}

case class ValidateUserOwnsConsignment[T](argument: Argument[T]) extends AuthorisationTag {
  override def validate(ctx: Context[ConsignmentApiContext, _])
                       (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val userId = token.userId.getOrElse("")

    val arg: T = ctx.arg[T](argument.name)
    val consignmentId: UUID = arg match {
      case uoc: UserOwnsConsignment => uoc.consignmentId
      case id: UUID => id
    }

    ctx.ctx.consignmentService
      .getConsignment(consignmentId)
      .map(consignment => {
        if(consignment.isEmpty) {
          throw AuthorisationException("Invalid consignment id")
        }

        if (consignment.get.userid.toString == userId) {
          continue
        } else {
          throw AuthorisationException("User does not own consignment")
        }
      })
  }
}

object ValidateUserOwnsFiles extends AuthorisationTag {
  override def validate(ctx: Context[ConsignmentApiContext, _])
                       (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val token = ctx.ctx.accessToken
    val tokenUserId = token.userId.getOrElse(
      throw AuthorisationException(s"No user ID in token"))

    val queryInput = ctx.arg[Seq[AddClientFileMetadataInput]]("addClientFileMetadataInput")

    val fileIds = queryInput.map(_.fileId)
    ctx.ctx.fileService
      .getOwnersOfFiles(fileIds)
      .map(fileOwnership => {
        val otherUsersFiles = fileOwnership.filter(_.userId.toString != tokenUserId)

        otherUsersFiles match {
          case Nil => continue
          case files => throw AuthorisationException(
            s"User '$tokenUserId' does not have permission to updatefiles: ${files.map(_.fileId)}")
        }
      })
  }
}
