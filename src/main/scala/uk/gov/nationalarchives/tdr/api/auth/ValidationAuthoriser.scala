package uk.gov.nationalarchives.tdr.api.auth

import sangria.execution.{BeforeFieldResult, Middleware, MiddlewareAttachment, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.Tags._
object ValidationAuthoriser extends Middleware[ConsignmentApiContext] with MiddlewareBeforeField[ConsignmentApiContext] {

  case class WrongBodyException(message: String) extends Exception(message)

  override type QueryVal = Unit
  override type FieldVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[ConsignmentApiContext, _, _]): QueryVal = ()

  override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[ConsignmentApiContext, _, _]): Unit = ()

  override def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[ConsignmentApiContext, _, _]
                           , ctx: Context[ConsignmentApiContext, _]): BeforeFieldResult[ConsignmentApiContext, Unit] = {
      val validationList: Seq[BeforeFieldResult[ConsignmentApiContext, Unit]] = ctx.field.tags.map {
        case v: ValidateTags => v.validate(ctx)
      }
    validationList.headOption.getOrElse(continue)
  }
}
