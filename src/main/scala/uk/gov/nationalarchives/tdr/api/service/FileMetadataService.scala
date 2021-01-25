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
  case class StaticMetadata(name: String, value: String)
  val SHA256ClientSideChecksum = "SHA256ClientSideChecksum"
  val ClientSideOriginalFilepath = "ClientSideOriginalFilepath"
  val ClientSideFileLastModifiedDate = "ClientSideFileLastModifiedDate"
  val ClientSideFileSize = "ClientSideFileSize"
  //There was a business decision made in the data enhancements workshop on 03/11/20 to store this metadata with these static values
  val RightsCopyright: StaticMetadata = StaticMetadata("RightsCopyright", "Crown Copyright")
  val LegalStatus: StaticMetadata = StaticMetadata("LegalStatus", "Public Record")
  val HeldBy: StaticMetadata = StaticMetadata("HeldBy", "TNA")
  val Language: StaticMetadata = StaticMetadata("Language", "English")
  val FoiExemptionCode: StaticMetadata = StaticMetadata("FoiExemptionCode", "open")

  val clientSideProperties = List(SHA256ClientSideChecksum, ClientSideOriginalFilepath, ClientSideFileLastModifiedDate, ClientSideFileSize)
  val staticMetadataProperties = List(RightsCopyright, LegalStatus, HeldBy, Language, FoiExemptionCode)
}
