package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import uk.gov.nationalarchives
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{Avmetadata, File}
import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.{AntivirusMetadataRepository, FileRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.{AddAntivirusMetadataInput, AntivirusMetadata}
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.{ConsignmentFileMetadataProgress}

import scala.concurrent.{Await, ExecutionContext, Future}

class AntivirusMetadataService(antivirusMetadataRepository: AntivirusMetadataRepository, fileRepository: FileRepository)
                              (implicit val executionContext: ExecutionContext) {

  def addAntivirusMetadata(input: AddAntivirusMetadataInput): Future[AntivirusMetadata] = {

    val inputRow = AvmetadataRow(
      input.fileId,
      input.software,
      input.value,
      input.softwareVersion,
      input.databaseVersion,
      input.result,
      Timestamp.from(Instant.ofEpochMilli(input.datetime)))

    antivirusMetadataRepository.addAntivirusMetadata(inputRow).map(row => {
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
  }

  def getFileMetadataProgress(consignmentId: UUID): Future[ConsignmentFileMetadataProgress] = {
      for (
        total <- fileRepository.countFilesInConsignment(consignmentId);
        processed <- fileRepository.countProcessedFilesInConsignment(consignmentId)
      ) yield ConsignmentFileMetadataProgress(processed, total)
  }
}
