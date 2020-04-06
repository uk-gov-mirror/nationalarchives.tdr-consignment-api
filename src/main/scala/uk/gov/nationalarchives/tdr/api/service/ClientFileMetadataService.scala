package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import uk.gov.nationalarchives.Tables.ClientfilemetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.ClientFileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.ClientFileMetadataFields.{AddClientFileMetadataInput, ClientFileMetadata}

import scala.concurrent.{ExecutionContext, Future}

class ClientFileMetadataService(clientFileMetadataRepository: ClientFileMetadataRepository, uuidSource: UUIDSource)
                               (implicit val executionContext: ExecutionContext) {

  def addClientFileMetadata(input: AddClientFileMetadataInput): Future[ClientFileMetadata] = {

    val clientFileMetadataRow = ClientfilemetadataRow(
      uuidSource.uuid,
      input.fileId,
      input.originalPath,
      input.checksum,
      input.checksumType,
      Timestamp.from(Instant.ofEpochMilli(input.lastModified)),
      Timestamp.from(Instant.ofEpochMilli(input.createdDate)),
      input.fileSize,
      Timestamp.from(Instant.ofEpochMilli(input.datetime))
    )

    clientFileMetadataRepository.addClientFileMetadata(clientFileMetadataRow).map(row => ClientFileMetadata(
      row.fileid,
      row.originalpath,
      row.checksum,
      row.checksumtype,
      row.lastmodified.getTime,
      row.createddate.getTime,
      row.filesize,
      row.datetime.getTime,
      row.clientfilemetadataid
    ))
  }
}
