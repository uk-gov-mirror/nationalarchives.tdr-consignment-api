package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.time.Instant
import java.util.UUID

import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.tdr.api.db.repository.FileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.{AddClientFileMetadataInput, ClientFileMetadata}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService._
import uk.gov.nationalarchives.Tables.FilemetadataRow

import scala.concurrent.{ExecutionContext, Future}

class ClientFileMetadataService(fileMetadataRepository: FileMetadataRepository,
                                uuidSource: UUIDSource, timeSource: TimeSource)
                               (implicit val executionContext: ExecutionContext) {

  implicit class LongUtils(value: Long) {
    def toTimestampString: String = Timestamp.from(Instant.ofEpochMilli(value)).toString
  }

  def addClientFileMetadata(inputs: Seq[AddClientFileMetadataInput], userId: UUID): Future[List[ClientFileMetadata]] = {
    val time = Timestamp.from(timeSource.now)
    val row: (UUID, String, Option[String]) => Tables.FilemetadataRow = FilemetadataRow(uuidSource.uuid, _,  _, time, userId, _)
    val inputRows = inputs.flatMap(input => {
      List(
        row(input.fileId, input.originalPath.getOrElse(""), Option(ClientSideOriginalFilepath)),
        row(input.fileId, input.lastModified.toTimestampString, Option(ClientSideFileLastModifiedDate)),
        row(input.fileId, input.fileSize.map(_.toString).getOrElse(""), Option(ClientSideFileSize)),
        row(input.fileId, input.checksum.getOrElse(""), Option(SHA256ClientSideChecksum))
      )
    })
    fileMetadataRepository.addFileMetadata(inputRows).map(rows => {
      val fileToRow = rows.groupBy(f => f.fileid)
      fileToRow.map {
        case (fileId, rows) => convertToResponse(fileId, rows)
      }.toList
    })
  }

  def getClientFileMetadata(fileId: UUID): Future[ClientFileMetadata] = {
    fileMetadataRepository.getFileMetadata(fileId, clientSideProperties: _*)
      .map(rows => convertToResponse(fileId, rows))
      .recover {
        case nse: NoSuchElementException => throw InputDataException(s"Could not find client metadata for file $fileId", Some(nse))
        case e: SQLException => throw InputDataException(e.getLocalizedMessage, Some(e))
      }
  }

  private def convertToResponse(fileId: UUID, rows: Seq[FilemetadataRow]): ClientFileMetadata = {
    val propertyNameToValue = rows.map(row => row.propertyname.get -> row.value).toMap
    ClientFileMetadata(fileId,
      propertyNameToValue.get(ClientSideOriginalFilepath),
      propertyNameToValue.get(SHA256ClientSideChecksum),
      Some("SHA256"),
      Timestamp.valueOf(propertyNameToValue(ClientSideFileLastModifiedDate)).getTime,
      propertyNameToValue.get(ClientSideFileSize).map(_.toLong)
    )
  }
}
