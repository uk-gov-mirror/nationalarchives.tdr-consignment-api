package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant

import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.AVMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.AVMetadataFields.{AVMetadata, AddAVMetadataInput}

import scala.concurrent.{ExecutionContext, Future}

class AVMetadataService(avMetadataRepository: AVMetadataRepository)
                       (implicit val executionContext: ExecutionContext) {

  def addAVMetadata(inputs: Seq[AddAVMetadataInput]): Future[Seq[AVMetadata]] = {

    val rows: Seq[AvmetadataRow] = inputs.map(i => AvmetadataRow(
        i.fileId,
        i.software,
        i.value,
        i.softwareVersion,
        i.databaseVersion,
        i.result,
        Timestamp.from(Instant.ofEpochMilli(i.datetime)
      )))

    avMetadataRepository.addAVMetadata(rows).map(r => {
      r.map(row => {
        AVMetadata(
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
