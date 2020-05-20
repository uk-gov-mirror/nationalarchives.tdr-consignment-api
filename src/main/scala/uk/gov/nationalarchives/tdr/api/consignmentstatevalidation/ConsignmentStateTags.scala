package uk.gov.nationalarchives.tdr.api.consignmentstatevalidation

import sangria.execution.BeforeFieldResult
import sangria.schema.Context
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.AddFilesInput
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.AddFileMetadataInput
import uk.gov.nationalarchives.tdr.api.graphql.{ConsignmentApiContext, ValidationTag}

import scala.concurrent.{ExecutionContext, Future}

trait ConsignmentStateTag extends ValidationTag

object ValidateNoPreviousUploadForConsignment extends ConsignmentStateTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                       (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val consignmentId = ctx.arg[AddFilesInput]("addFilesInput").consignmentId

    ctx.ctx.consignmentService.consignmentHasFiles(consignmentId).map {
      case false => continue
      case true => throw ConsignmentStateException("Upload already occurred for consignment: " + consignmentId)
    }
  }
}

object ValidateFilesExistForMetadata extends ConsignmentStateTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                            (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val fileIds = ctx.arg[AddFileMetadataInput]("addFileMetadataInput").fileMetadataValues.map(_.fileId)

    ctx.ctx.fileService.findNonExistentFiles(fileIds).map(fileIds => {
      if(fileIds.isEmpty) {
        continue
      } else {
        throw ConsignmentStateException(s"File id(s) ${fileIds.mkString(",")} do not exist")
      }
    })
  }
}

object ValidateFilePropertyExists extends ConsignmentStateTag {
  override def validateAsync(ctx: Context[ConsignmentApiContext, _])
                            (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val filePropertyName = ctx.arg[AddFileMetadataInput]("addFileMetadataInput").filePropertyName
    ctx.ctx.fileMetadataService.getFileProperty(filePropertyName) map {
      case Some(_) => continue
      case None => throw ConsignmentStateException(s"File property name $filePropertyName does not exist")
    }
  }
}