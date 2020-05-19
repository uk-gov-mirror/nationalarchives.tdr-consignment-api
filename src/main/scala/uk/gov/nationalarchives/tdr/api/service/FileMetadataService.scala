package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables.FilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: Option[UUID]): Future[Seq[FileMetadata]] = {
    filePropertyRepository.getPropertyByName(addFileMetadataInput.filePropertyName).flatMap(property => {
      val metadataRows: Seq[FilemetadataRow] = addFileMetadataInput.fileMetadataValues.map(metadataValues => {
        FilemetadataRow(uuidSource.uuid, Some(metadataValues.fileId), property.headOption.map(_.propertyid), Some(metadataValues.value),Timestamp.from(timeSource.now), userId.get)
      })
      fileMetadataRepository.addFileMetadata(metadataRows).map(rows => {
        rows.map(row => {
          FileMetadata(addFileMetadataInput.filePropertyName, row.fileid.get, row.value.get)
        })
      })
    })
  }

}
