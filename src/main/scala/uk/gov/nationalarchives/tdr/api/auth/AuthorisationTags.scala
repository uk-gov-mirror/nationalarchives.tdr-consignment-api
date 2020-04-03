package uk.gov.nationalarchives.tdr.api.auth

import java.util.UUID

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.{Argument, Context}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment

import scala.concurrent._
import scala.language.postfixOps

trait AuthorisationTag extends FieldTag {
  def validate(ctx: Context[ConsignmentApiContext, _])
              (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]]

  val continue: BeforeFieldResult[ConsignmentApiContext, Unit] = BeforeFieldResult(())
}

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
