package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilepropertyRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{ClientFileMetadataRepository, FileMetadataRepository, FilePropertyRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.{AddFileMetadataInput, FileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository, filePropertyRepository: FilePropertyRepository,
                          clientFileMetadataService: ClientFileMetadataService,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val ec: ExecutionContext) {

  def addFileMetadata(addFileMetadataInput: AddFileMetadataInput, userId: Option[UUID]): Future[FileMetadata] = {
    //Add checksum validation result to File. We may move this later
    val addChecksumValidation = addFileMetadataInput.filePropertyName match {
      case "SHA256ServerSideChecksum" =>
        addChecksumValidationResult(addFileMetadataInput).recover {
          case e: Exception => throw InputDataException(e.getLocalizedMessage, Some(e))
        }
      // We should never need this because we only currently send checksum updates but it keeps it neat-ish
      case _ => Future.successful(1)
    }
    addChecksumValidation.flatMap(_ => {
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
    })
  }

  def addChecksumValidationResult(addFileMetadataInput: AddFileMetadataInput): Future[Int] = {
    val fileId = addFileMetadataInput.fileId
    for {
      cfm <- clientFileMetadataService.getClientFileMetadata(fileId)
      cvr <- fileMetadataRepository.addChecksumValidationResult(fileId, cfm.checksum.map(_ == addFileMetadataInput.value))
    } yield cvr
  }

  def getFileProperty(filePropertyName: String): Future[Option[FilepropertyRow]] = {
    filePropertyRepository.getPropertyByName(filePropertyName)
  }
}
