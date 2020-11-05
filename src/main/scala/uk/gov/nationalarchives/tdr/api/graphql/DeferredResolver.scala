package uk.gov.nationalarchives.tdr.api.graphql


import java.util.UUID

import sangria.execution.deferred.{Deferred, UnsupportedDeferError}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{FileChecks, TransferringBody}
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields._

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[ConsignmentApiContext] {
  // We may at some point need to do authorisation in this method. There is a ensurePermissions method which needs to be called before returning data.
  override def resolve(deferred: Vector[Deferred[Any]], context: ConsignmentApiContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case DeferTotalFiles(consignmentId) => consignmentId.map(id => context.fileService.fileCount(id)).getOrElse(Future.successful(0))
      case DeferFileChecksProgress(consignmentId) =>
        consignmentId.map(
          id => context.consignmentService.getConsignmentFileProgress(id)
        ).getOrElse(Future.successful(0))
      case DeferParentFolder(consignmentId) =>
        consignmentId match {
          case Some(id) => context.consignmentService.getConsignmentParentFolder(id)
          case None => Future.successful(None)
        }
      case DeferConsignmentSeries(consignmentId) =>
        consignmentId.map(
          id => context.consignmentService.getSeriesOfConsignment(id)
        ).getOrElse(Future.successful(None))
      case DeferConsignmentBody(consignmentId) =>
        consignmentId.map(
          id => context.consignmentService.getTbOfConsignment(id)
        ).getOrElse(Future.successful(0))
      case other => throw UnsupportedDeferError(other)
    }
  }
}

case class DeferTotalFiles(consignmentId: Option[UUID]) extends Deferred[Int]
case class DeferFileChecksProgress(consignmentId: Option[UUID]) extends Deferred[FileChecks]
case class DeferParentFolder(consignmentId: Option[UUID]) extends Deferred[Option[String]]
case class DeferConsignmentSeries(consignmentId: Option[UUID]) extends Deferred[Option[Series]]
case class DeferConsignmentBody(consignmentId: Option[UUID]) extends Deferred[TransferringBody]
