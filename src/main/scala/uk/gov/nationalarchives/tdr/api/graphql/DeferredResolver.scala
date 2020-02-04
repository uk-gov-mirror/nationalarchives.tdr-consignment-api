package uk.gov.nationalarchives.tdr.api.graphql


import sangria.execution.deferred.{Deferred, UnsupportedDeferError}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[ConsignmentApiContext] {
  override def resolve(deferred: Vector[Deferred[Any]], context: ConsignmentApiContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case other => throw UnsupportedDeferError(other)
    }
  }
}
