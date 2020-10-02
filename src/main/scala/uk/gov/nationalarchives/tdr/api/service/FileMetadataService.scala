package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{ClientFileMetadataRepository, FileMetadataRepository, FilePropertyRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          clientFileMetadataService: ClientFileMetadataService,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: Option[UUID]): Future[FileMetadata] = {
    def row(property: FilepropertyRow) = FilemetadataRow(uuidSource.uuid,
      addFileMetadataInput.fileId,
      property.propertyid,
      addFileMetadataInput.value,
      Timestamp.from(timeSource.now),
      userId.get)
    val filePropertyName = addFileMetadataInput.filePropertyName

    filePropertyName match {
      case SHA256ServerSideChecksum =>
        (for {
          fileProperty <- getFileProperty(addFileMetadataInput.filePropertyName)
          cfm <- clientFileMetadataService.getClientFileMetadata(addFileMetadataInput.fileId) if fileProperty.isDefined
          row <- fileMetadataRepository.addChecksumMetadata(row(fileProperty.get), cfm.checksum.map(_ == addFileMetadataInput.value))
        } yield FileMetadata(fileProperty.flatMap(_.name).get, row.fileid, row.value)) recover {
          case e: Throwable => throw InputDataException(e.getLocalizedMessage, Some(e))
        }
      case _ => Future.failed(InputDataException(s"$filePropertyName found. We are only expecting checksum updates for now"))
    }

  }

  def getFileProperty(filePropertyName: String): Future[Option[FilepropertyRow]] = {
    filePropertyRepository.getPropertyByName(filePropertyName)
      .recover(err => throw InputDataException(s"The property does not exist", Some(err)))
  }
}
