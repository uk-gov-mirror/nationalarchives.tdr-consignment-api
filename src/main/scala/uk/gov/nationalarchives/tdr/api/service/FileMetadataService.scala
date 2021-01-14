package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.tdr.api.db.repository.FileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata, SHA256ServerSideChecksum}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService.SHA256ClientSideChecksum
import uk.gov.nationalarchives.Tables.FilemetadataRow

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: UUID): Future[FileMetadata] = {
    val filePropertyName = addFileMetadataInput.filePropertyName
    val row =
      FilemetadataRow(uuidSource.uuid, addFileMetadataInput.fileId,
      addFileMetadataInput.value,
      Timestamp.from(timeSource.now),
      userId, addFileMetadataInput.filePropertyName)

    filePropertyName match {
      case SHA256ServerSideChecksum =>
        (for {
          cfm <- fileMetadataRepository.getFileMetadata(addFileMetadataInput.fileId, SHA256ClientSideChecksum)
          row <- fileMetadataRepository.addChecksumMetadata(row, cfm.headOption.map(_.value == addFileMetadataInput.value))
        } yield FileMetadata(filePropertyName, row.fileid, row.value)) recover {
          case e: Throwable => throw InputDataException(s"Could not find metadata for file ${addFileMetadataInput.fileId}", Some(e))
        }
      case _ => Future.failed(InputDataException(s"$filePropertyName found. We are only expecting checksum updates for now"))
    }
  }
}

object FileMetadataService {
  val SHA256ClientSideChecksum = "SHA256ClientSideChecksum"
  val ClientSideOriginalFilepath = "ClientSideOriginalFilepath"
  val ClientSideFileLastModifiedDate = "ClientSideFileLastModifiedDate"
  val ClientSideFileSize = "ClientSideFileSize"

  val clientSideProperties = List(SHA256ClientSideChecksum, ClientSideOriginalFilepath, ClientSideFileLastModifiedDate, ClientSideFileSize)
}
