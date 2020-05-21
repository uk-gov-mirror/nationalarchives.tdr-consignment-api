package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables.FilepropertyRow
import uk.gov.nationalarchives.Tables.FilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: Option[UUID]): Future[Seq[FileMetadata]] = {
    getFileProperty(addFileMetadataInput.filePropertyName).flatMap {
      case Some(property) =>
        val metadataRows: Seq[FilemetadataRow] = addFileMetadataInput.fileMetadataValues.map(metadataValues => {
          FilemetadataRow(uuidSource.uuid,
            metadataValues.fileId,
            property.propertyid,
            metadataValues.value,
            Timestamp.from(timeSource.now),
            userId.get)
        })
        fileMetadataRepository.addFileMetadata(metadataRows).map(rows => {
          rows.map(row => {
            FileMetadata(addFileMetadataInput.filePropertyName, row.fileid, row.value)
          })
        }).recover {
          case e: SQLException => throw InputDataException(e.getLocalizedMessage)
        }
      case None => throw InputDataException(s"The property does not exist")
    }
  }

  def getFileProperty(filePropertyName: String): Future[Option[FilepropertyRow]] = {
    filePropertyRepository.getPropertyByName(filePropertyName)
  }

}
