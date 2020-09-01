package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.AntivirusMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.{AddAntivirusMetadataInput, AntivirusMetadata}

import scala.concurrent.{ExecutionContext, Future}

class AntivirusMetadataService(antivirusMetadataRepository: AntivirusMetadataRepository)
                              (implicit val executionContext: ExecutionContext) {

  def addAntivirusMetadata(input: AddAntivirusMetadataInput): Future[AntivirusMetadata] = {

    val inputRow = AvmetadataRow(
      input.fileId,
      input.software,
      input.softwareVersion,
      input.databaseVersion,
      input.result,
      Timestamp.from(Instant.ofEpochMilli(input.datetime)))

    antivirusMetadataRepository.addAntivirusMetadata(inputRow).map(row => {
      AntivirusMetadata(
        row.fileid,
        row.software,
        row.softwareversion,
        row.databaseversion,
        row.result,
        row.datetime.getTime
      )
    })
  }
}
