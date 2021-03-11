package uk.gov.nationalarchives.tdr.api.service

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import uk.gov.nationalarchives.Tables.{FfidmetadataRow, FfidmetadatamatchesRow}
import uk.gov.nationalarchives.tdr.api.db.repository.{FFIDMetadataMatchesRepository, FFIDMetadataRepository}
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.{FFIDMetadata, FFIDMetadataInput, FFIDMetadataMatches}

import scala.concurrent.{ExecutionContext, Future}

class FFIDMetadataService(ffidMetadataRepository: FFIDMetadataRepository, matchesRepository: FFIDMetadataMatchesRepository,
                          timeSource: TimeSource, uuidSource: UUIDSource)(implicit val executionContext: ExecutionContext) {
  def addFFIDMetadata(ffidMetadata: FFIDMetadataInput): Future[FFIDMetadata] = {
    val metadataRows = FfidmetadataRow(uuidSource.uuid, ffidMetadata.fileId,
      ffidMetadata.software,
      ffidMetadata.softwareVersion,
      Timestamp.from(timeSource.now),
      ffidMetadata.binarySignatureFileVersion,
      ffidMetadata.containerSignatureFileVersion,
      ffidMetadata.method)

    def addMatches(ffidmetadataid: UUID) = {
      val matchRows = ffidMetadata.matches.map(m =>FfidmetadatamatchesRow(ffidmetadataid, m.extension, m.identificationBasis, m.puid))
      matchesRepository.addFFIDMetadataMatches(matchRows)

    }
    (for {
      metadata <- ffidMetadataRepository.addFFIDMetadata(metadataRows)
      matchrow <- addMatches(metadata.ffidmetadataid)
    } yield {
      FFIDMetadata(
        metadata.fileid,
        metadata.software,
        metadata.softwareversion,
        metadata.binarysignaturefileversion,
        metadata.containersignaturefileversion,
        metadata.method,
        matchrow.map(r => FFIDMetadataMatches(r.extension, r.identificationbasis, r.puid)).toList,
        metadata.datetime.getTime
      )
    }).recover {
      case e: SQLException => throw InputDataException(e.getLocalizedMessage)
    }
  }
}
