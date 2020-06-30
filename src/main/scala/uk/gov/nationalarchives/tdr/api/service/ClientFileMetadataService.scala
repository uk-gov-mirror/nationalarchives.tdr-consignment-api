package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.time.Instant
import java.util.UUID

import uk.gov.nationalarchives.Tables.ClientfilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.ClientFileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.{AddClientFileMetadataInput, ClientFileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class ClientFileMetadataService(clientFileMetadataRepository: ClientFileMetadataRepository, uuidSource: UUIDSource)
                               (implicit val executionContext: ExecutionContext) {

  def getClientFileMetadata(fileId: UUID): Future[ClientFileMetadata] = {
    clientFileMetadataRepository.getClientFileMetadata(fileId).map(_.head).map(rowToOutput).recover {
      case _: NoSuchElementException => throw InputDataException(s"Could not find metadata for file $fileId")
      case e: SQLException => throw InputDataException(e.getLocalizedMessage)
    }
  }

  def addClientFileMetadata(inputs: Seq[AddClientFileMetadataInput]): Future[Seq[ClientFileMetadata]] = {

    val rows: Seq[ClientfilemetadataRow] = inputs.map(i => ClientfilemetadataRow(
      uuidSource.uuid,
      i.fileId,
      i.originalPath,
      i.checksum,
      i.checksumType,
      Timestamp.from(Instant.ofEpochMilli(i.lastModified)),
      Timestamp.from(Instant.ofEpochMilli(i.createdDate)),
      i.fileSize,
      Timestamp.from(Instant.ofEpochMilli(i.datetime))))

    clientFileMetadataRepository.addClientFileMetadata(rows).map(r => {
      r.map(rowToOutput)
    })
  }

  private def rowToOutput(row: ClientfilemetadataRow): ClientFileMetadata = {
    ClientFileMetadata(
      row.fileid,
      row.originalpath,
      row.checksum,
      row.checksumtype,
      row.lastmodified.getTime,
      row.createddate.getTime,
      row.filesize,
      row.datetime.getTime,
      row.clientfilemetadataid
    )
  }
}
