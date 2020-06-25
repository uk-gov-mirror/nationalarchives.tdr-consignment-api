package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.time.Instant

import uk.gov.nationalarchives.tdr.api.db.repository.FFIDMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.{FFIDMetadata, FFIDMetadataInput}
import uk.gov.nationalarchives.Tables.FfidmetadataRow
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException

import scala.concurrent.{ExecutionContext, Future}

class FFIDMetadataService(ffidMetadataRespoitory: FFIDMetadataRepository)(implicit val executionContext: ExecutionContext) {
  def addFFIDMetadata(ffidMetadata: FFIDMetadataInput): Future[FFIDMetadata] = {
    val row = FfidmetadataRow(ffidMetadata.fileId, ffidMetadata.software, ffidMetadata.softwareVersion, ffidMetadata.binarySignatureFileVersion, ffidMetadata.containerSignatureFileVersion, ffidMetadata.method, ffidMetadata.extension, ffidMetadata.identificationBasis, ffidMetadata.puid, Timestamp.from(Instant.ofEpochMilli(ffidMetadata.datetime)))
    ffidMetadataRespoitory.addFFIDMetadata(row).map(r => FFIDMetadata(
      r.fileid, r.software, r.softwareversion, r.binarysignaturefileversion, r.containersignaturefileversion, r.method, r.extension, r.identificationbasis, r.puid, r.datetime.getTime
    )).recover {
      case e: SQLException => throw InputDataException(e.getLocalizedMessage)
    }
  }
}
