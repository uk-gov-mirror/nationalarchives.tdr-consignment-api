package uk.gov.nationalarchives.tdr.api.graphql


import java.util.UUID

import sangria.execution.deferred.{Deferred, UnsupportedDeferError}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{FileChecks, TransferringBody}
import uk.gov.nationalarchives.tdr.api.graphql.fields.SeriesFields._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[ConsignmentApiContext] {
  // We may at some point need to do authorisation in this method. There is a ensurePermissions method which needs to be called before returning data.
  override def resolve(deferred: Vector[Deferred[Any]], context: ConsignmentApiContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case DeferTotalFiles(consignmentId) => context.fileService.fileCount(consignmentId)
      case DeferFileChecksProgress(consignmentId) =>
        context.consignmentService.getConsignmentFileProgress(consignmentId)
//          .transform {
//          case x: Success[_] => x
//          case Failure(e) => {
//            throw new RuntimeException("Handled e", e)
//          }
//        }
      case DeferParentFolder(consignmentId) => context.consignmentService.getConsignmentParentFolder(consignmentId)
      case DeferConsignmentSeries(consignmentId) => context.consignmentService.getSeriesOfConsignment(consignmentId)
      case DeferConsignmentBody(consignmentId) => context.consignmentService.getTransferringBodyOfConsignment(consignmentId)
      case other => throw UnsupportedDeferError(other)
    }
  }
}

case class DeferTotalFiles(consignmentId: UUID) extends Deferred[Int]
case class DeferFileChecksProgress(consignmentId: UUID) extends Deferred[FileChecks]
case class DeferParentFolder(consignmentId: UUID) extends Deferred[Option[String]]
case class DeferConsignmentSeries(consignmentId: UUID) extends Deferred[Option[Series]]
case class DeferConsignmentBody(consignmentId: UUID) extends Deferred[TransferringBody]
