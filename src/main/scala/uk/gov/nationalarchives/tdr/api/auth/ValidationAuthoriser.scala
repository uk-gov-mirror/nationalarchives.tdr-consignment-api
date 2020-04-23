package uk.gov.nationalarchives.tdr.api.auth

import sangria.execution.{BeforeFieldResult, FieldTag, Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ValidationAuthoriser(implicit executionContext: ExecutionContext)
  extends Middleware[ConsignmentApiContext] with MiddlewareBeforeField[ConsignmentApiContext] {

  override type QueryVal = Unit
  override type FieldVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[ConsignmentApiContext, _, _]): QueryVal = ()

  override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[ConsignmentApiContext, _, _]): Unit = ()

  override def beforeField(queryVal: QueryVal,
                           mctx: MiddlewareQueryContext[ConsignmentApiContext, _, _],
                           ctx: Context[ConsignmentApiContext, _]
                          ): BeforeFieldResult[ConsignmentApiContext, Unit] = {

    // All fields must have an authorisation tag defined. This means that if we forget to add authorisation, the
    // query is blocked by default, which prevents some security bugs.
    val isTopLevelField = ctx.path.path.length == 1
    val fieldHasAuthTag = ctx.field.tags.exists(tag => tag.isInstanceOf[ValidationTag])
    if (isTopLevelField && !fieldHasAuthTag) {
      throw new AssertionError(s"Query '${ctx.field.name}' does not have any authorisation steps defined")
    }

    val validationList: Seq[BeforeFieldResult[ConsignmentApiContext, Unit]] = ctx.field.tags.map {
      case v: ValidationTag => {
        val validationResult = v.validate(ctx)

        // Awaiting a Future is risky because the thread will block until the response is returned or the timeout is reached.
        // It could cause the API to be slow because akka-http cannot assign threads to new requests while this one is
        // blocked.
        //
        // We are only using Await because Sangria middleware does not support Futures like the main resolvers do. We should
        // remove it when we find a way to do authorisation in a completely async way in Sangria.
        Await.result(validationResult, 5 seconds)
      }
    }

    validationList.headOption.getOrElse(continue)
  }
}

case class AuthorisationException(message: String) extends Exception(message)
case class ConsignmentStateException(message: String) extends Exception(message)
