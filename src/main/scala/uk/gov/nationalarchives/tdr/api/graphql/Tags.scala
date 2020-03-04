package uk.gov.nationalarchives.tdr.api.graphql

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.auth.ValidationAuthoriser.{AuthorisationException, continue}


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
        val bodyFromToken: Option[String] = token.transferringBody
        if(bodyFromToken.getOrElse("") != bodyArg.getOrElse("")) {
          val msg = s"Body for user ${token.userId.get} was ${bodyArg.getOrElse("")} in the query and ${bodyFromToken.getOrElse("")} in the token"
          throw AuthorisationException(msg)
        }
        if(bodyFromToken.isEmpty) {
          val msg = s"No body is set on a non admin user ${token.userId.get}"
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
}
