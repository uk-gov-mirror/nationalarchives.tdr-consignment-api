package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: Option[UUID]): Future[FileMetadata] = {
    getFileProperty(addFileMetadataInput.filePropertyName).flatMap {
      case Some(property) =>
        val row = FilemetadataRow(uuidSource.uuid,
          addFileMetadataInput.fileId,
          property.propertyid,
          addFileMetadataInput.value,
          Timestamp.from(timeSource.now),
          userId.get)

        fileMetadataRepository.addFileMetadata(row).map(r => FileMetadata(property.name.get, r.fileid, r.value)).recover {
          case e: SQLException => throw InputDataException(e.getLocalizedMessage, Some(e))
        }
      case None => throw InputDataException(s"The property does not exist", Option.empty)
    }
  }

  def getFileProperty(filePropertyName: String): Future[Option[FilepropertyRow]] = {
    filePropertyRepository.getPropertyByName(filePropertyName)
  }
}
