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
      def getProperty(name: String) = token.getOtherClaims.get(name).asInstanceOf[String]

      val resourceAccess = Option(token.getResourceAccess("tdr"))

      val isAdmin: Boolean = resourceAccess match {
        case Some(access) => access.getRoles.contains("tdr_admin")
        case None => throw AuthorisationException("A role has not been assigned for this user")
      }

      val bodyArg: Option[String] = ctx.argOpt("body")

      if(!isAdmin) {
        val bodyFromToken: String = getProperty("body")
        if(bodyFromToken != bodyArg.getOrElse("")) {
          val msg = s"Body for user ${getProperty("user_id")} was ${bodyArg.getOrElse("")} in the query and $bodyFromToken in the token"
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
      val isAdmin = token.getResourceAccess("tdr").getRoles.contains("tdr_admin")
      if(isAdmin) {
        continue
      } else {
        throw AuthorisationException(s"Admin permissions required to call ${ctx.field.name}")
      }
    }
  }
}
