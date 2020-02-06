package uk.gov.nationalarchives.tdr.api.graphql

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.auth.ValidationAuthoriser.{WrongBodyException, continue}

object Tags {

  abstract class ValidateTags() extends FieldTag {
    def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit]
  }

  case class ValidateBody() extends ValidateTags {
    override def validate(ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      val token = ctx.ctx.accessToken
      def getProperty(name: String) = token.getOtherClaims.get(name).asInstanceOf[String]

      val isUser = token.getResourceAccess("tdr").getRoles.contains("tdr_user")
      val bodyArg: Option[String] = ctx.argOpt("body")

      if(isUser) {
        val bodyFromToken: String = getProperty("body")
        if(bodyFromToken != bodyArg.getOrElse("")) {
          val msg = s"Body for user ${getProperty("user_id")} was ${bodyArg.getOrElse("")} in the query and $bodyFromToken in the token"
          throw WrongBodyException(msg)
        }
        continue
      } else {
        continue
      }
    }
  }
}
