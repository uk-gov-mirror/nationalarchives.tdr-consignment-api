package uk.gov.nationalarchives.tdr.api.graphql

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.auth.ValidationAuthoriser.{AuthorisationException, continue}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.Consignment
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.AddTransferAgreementInput

import scala.util.{Failure, Success}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Tags {

  abstract class ValidateTags() extends FieldTag {
    def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit]
  }

  case class ValidateBody() extends ValidateTags {
    override def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      val token = ctx.ctx.accessToken

      val isAdmin: Boolean = token.roles.contains("tdr_admin")

      val bodyArg: Option[String] = ctx.argOpt("body")

      if(!isAdmin) {
        val bodyFromToken: String = token.transferringBody.getOrElse("")
        if(bodyFromToken != bodyArg.getOrElse("")) {
          val msg = s"Body for user ${token.userId} was ${bodyArg.getOrElse("")} in the query and $bodyFromToken in the token"
          throw AuthorisationException(msg)
        }
        continue
      } else {
        continue
      }
    }
  }

  case class ValidateIsAdmin() extends ValidateTags {
    override def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      val token = ctx.ctx.accessToken
      val isAdmin: Boolean = token.roles.contains("tdr_admin")
      if(isAdmin) {
        continue
      } else {
        throw AuthorisationException(s"Admin permissions required to call ${ctx.field.name}")
      }
    }
  }

  case class GetConsignmentId(argName: String) extends ValidateTags {
    override def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      argName match {
        case "addTransferAgreementInput" => ctx.arg(argName)
      }
    }

  }

  case class ValidateUserOwnsConsignment() extends ValidateTags {
    override def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      val token = ctx.ctx.accessToken
      val userId = token.getOtherClaims.get("user_id").asInstanceOf[String]
      val input: AddTransferAgreementInput = ctx.arg[AddTransferAgreementInput]("addTransferAgreementInput")
      val result = ctx.ctx.consignmentService.getConsignment(input.consignmentId)

      val consignment: Option[Consignment] = Await.result(result, 5 seconds)

      if(consignment.isEmpty) {
        throw AuthorisationException("Invalid consignment id")
      }

      if (consignment.get.userid.toString == userId) {
        continue
      } else {
        throw AuthorisationException("User does not own consignment")
      }
    }
  }
}
