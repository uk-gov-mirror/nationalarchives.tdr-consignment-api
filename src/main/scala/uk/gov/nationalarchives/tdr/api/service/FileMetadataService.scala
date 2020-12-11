package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID

import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FileMetadataRepository, FilePropertyRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata, _}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService.{ChecksumMetadata, MetadataInput, OtherMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          clientFileMetadataService: ClientFileMetadataService,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  @deprecated("This is only used for the single argument addFileMetadata field which will be deleted")
  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: UUID): Future[FileMetadata] = {
    def row(property: FilepropertyRow) = FilemetadataRow(uuidSource.uuid,
      addFileMetadataInput.fileId,
      property.propertyid,
      addFileMetadataInput.value,
      Timestamp.from(timeSource.now),
      userId)

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

  def addFileMetadata(inputs: Seq[AddFileMetadataInput], userId: UUID): Future[List[FileMetadata]] = {
    def metadataRow(fileId: UUID, propertyId: UUID, value: String) = FilemetadataRow(uuidSource.uuid,
      fileId,
      propertyId,
      value,
      Timestamp.from(timeSource.now),
      userId)

    lazy val properties: Future[Seq[Tables.FilepropertyRow]] = filePropertyRepository.getPropertiesByName(inputs.map(_.filePropertyName).toList)

    val metadataInputs: Seq[Future[MetadataInput]] = inputs.map {
      case AddFileMetadataInput(SHA256ServerSideChecksum, fileId, value) => for {
        property <- properties
        clientSideChecksum <- fileMetadataRepository.getFileMetadata(fileId, SHA256ClientSideChecksum)
      } yield {
        val propertyId = propertyIdFromName(property, SHA256ServerSideChecksum)
        val row = metadataRow(fileId, propertyId, value)
        val checksumMatches = clientSideChecksum.map(_.value == value)
        ChecksumMetadata(row, checksumMatches)
      }
      case AddFileMetadataInput(propertyName, fileId, value) => for {
        property <- properties
      } yield {
        OtherMetadata(metadataRow(fileId, propertyIdFromName(property, propertyName), value))
      }
    }

    for {
      property <- properties
      metadata <- Future.sequence(metadataInputs)
      result <- fileMetadataRepository.addMetadata(metadata)
    } yield result.map(r => FileMetadata(propertyNameFromId(property, r.propertyid), r.fileid, r.value)).toList
  }

  def getFileProperty(filePropertyName: String): Future[Option[FilepropertyRow]] = {
    filePropertyRepository.getPropertyByName(filePropertyName)
      .recover(err => throw InputDataException(s"The property does not exist", Some(err)))
  }

  private def propertyIdFromName(properties: Seq[FilepropertyRow], name: String): UUID = properties.filter(_.name.get == name).map(_.propertyid).head
  private def propertyNameFromId(properties: Seq[FilepropertyRow], id: UUID): String = properties.filter(_.propertyid == id).map(_.name.get).head
}

object FileMetadataService {

  trait MetadataInput {
    def fileMetadataRow(): FilemetadataRow
  }

  case class ChecksumMetadata(fileMetadataRow: FilemetadataRow, validationResult: Option[Boolean]) extends MetadataInput

  case class OtherMetadata(fileMetadataRow: FilemetadataRow) extends MetadataInput

}
