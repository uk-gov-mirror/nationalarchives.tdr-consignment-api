package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.AntivirusMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.{AntivirusMetadata, AddAntivirusMetadataInput}

import scala.concurrent.{ExecutionContext, Future}

class AntivirusMetadataService(antivirusMetadataRepository: AntivirusMetadataRepository)
                              (implicit val executionContext: ExecutionContext) {

  def addAntivirusMetadata(inputs: Seq[AddAntivirusMetadataInput]): Future[Seq[AntivirusMetadata]] = {

    val rows: Seq[AvmetadataRow] = inputs.map(i => AvmetadataRow(
        i.fileId,
        i.software,
        i.value,
        i.softwareVersion,
        i.databaseVersion,
        i.result,
        Timestamp.from(Instant.ofEpochMilli(i.datetime)
      )))

    antivirusMetadataRepository.addAntivirusMetadata(rows).map(r => {
      r.map(row => {
        AntivirusMetadata(
          row.fileid,
          row.software,
          row.value,
          row.softwareversion,
          row.databaseversion,
          row.result,
          row.datetime.getTime
        )
      })
    })
}}
