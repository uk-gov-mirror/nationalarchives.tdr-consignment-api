package uk.gov.nationalarchives.tdr.auth

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext

import scala.concurrent.{ExecutionContext, Future}

trait ValidationTag extends FieldTag {
  def validate(ctx: Context[ConsignmentApiContext, _])
              (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]]

  val continue: BeforeFieldResult[ConsignmentApiContext, Unit] = BeforeFieldResult(())
}
