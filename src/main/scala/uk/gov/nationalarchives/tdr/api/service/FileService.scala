package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.tdr.api.db.repository.FileRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileFields.File

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileRepository: FileRepository, timeSource: TimeSource)(implicit val executionContext: ExecutionContext) {

  def addFile(consignmentId: Long): Future[File] = ???
}
