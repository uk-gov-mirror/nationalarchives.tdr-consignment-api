package uk.gov.nationalarchives.tdr.api.service

import sangria.macros.derive.GraphQLDeprecated
import uk.gov.nationalarchives.Tables.{ConsignmentmetadataRow, FilemetadataRow}
import uk.gov.nationalarchives.tdr.api.db.repository.FileMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.DataExceptions.InputDataException
import uk.gov.nationalarchives.tdr.api.graphql.fields.AntivirusMetadataFields.AntivirusMetadata
import uk.gov.nationalarchives.tdr.api.graphql.fields.ConsignmentFields.Consignment
import uk.gov.nationalarchives.tdr.api.graphql.fields.FFIDMetadataFields.FFIDMetadata
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields._
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileStatusFields.FileStatus
import uk.gov.nationalarchives.tdr.api.service.FileMetadataPropertyName.{tdrDataLoadHeaderToPropertyMapper, _}
import uk.gov.nationalarchives.tdr.api.service.FileMetadataService.{AddFileMetadataInput, FileMetadataValues, getFileMetadataValues}
import uk.gov.nationalarchives.tdr.api.service.ReferenceGeneratorService.Reference
import uk.gov.nationalarchives.tdr.schema.generated.BaseSchema

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class FileMetadataService(fileMetadataRepository: FileMetadataRepository)(implicit val ec: ExecutionContext) {

  def getSumOfFileSizes(consignmentId: UUID): Future[Long] = fileMetadataRepository.getSumOfFileSizes(consignmentId)

  def addFileMetadata(input: AddFileMetadataWithFileIdInput, userId: UUID): Future[List[FileMetadataWithFileId]] = {
    val metadataRow = input.metadataInputValues
      .map(addFileMetadataInput => {
        val fileId = addFileMetadataInput.fileId
        AddFileMetadataInput(fileId, addFileMetadataInput.value, userId, addFileMetadataInput.filePropertyName)
      })
    fileMetadataRepository
      .addFileMetadata(metadataRow)
      .map(_.map(row => FileMetadataWithFileId(row.propertyname, row.fileid, row.value)).toList)
      .recover(err => throw InputDataException(err.getMessage))
  }

  def addOrUpdateBulkFileMetadata(metadataInput: AddOrUpdateBulkFileMetadataInput, userId: UUID): Future[List[FileMetadataWithFileId]] = {
    val protectedMetadata = systemProperties.map(p => tdrDataLoadHeaderToPropertyMapper(p))
    metadataInput.fileMetadata.map { addOrUpdateFileMetadata =>
      addOrUpdateFileMetadata.metadata.map { metadata =>
        if (protectedMetadata.contains(metadata.filePropertyName)) {
          throw InputDataException(s"Protected metadata property found: ${metadata.filePropertyName}")
        }
      }
    }

    // Separate empty values (for deletion) from non-empty values (for upsert)
    val (emptyValueMetadata, nonEmptyValueMetadata) = metadataInput.fileMetadata
      .flatMap { fileMetadata =>
        fileMetadata.metadata.map { metadata =>
          (fileMetadata.fileId, metadata)
        }
      }
      .partition { case (_, metadata) => metadata.value.isEmpty }

    // Delete properties with empty values
    val deleteFutures = emptyValueMetadata
      .groupBy(_._1)
      .map { case (fileId, metadataList) =>
        val propertiesToDelete = metadataList.map(_._2.filePropertyName).toSet
        fileMetadataRepository.deleteFileMetadata(fileId, propertiesToDelete)
      }
      .toSeq

    // Upsert properties with non-empty values
    val fileMetadataInputs = nonEmptyValueMetadata.map { case (fileId, metadata) =>
      AddFileMetadataInput(fileId, metadata.value, userId, metadata.filePropertyName)
    }.toList

    for {
      _ <- Future.sequence(deleteFutures)
      insertedRows <- if (fileMetadataInputs.nonEmpty) fileMetadataRepository.addOrUpdateFileMetadata(fileMetadataInputs) else Future.successful(Seq.empty)
      medataPropertiesAdded = insertedRows.map(r => FileMetadataWithFileId(r.propertyname, r.fileid, r.value)).toList
    } yield medataPropertiesAdded
  }

  def totalClosedRecords(consignmentId: UUID): Future[Int] = {
    fileMetadataRepository.totalClosedRecords(consignmentId)
  }

  def totalRetainedRecords(consignmentId: UUID): Future[Int] = {
    fileMetadataRepository.totalRetainedRecords(consignmentId)
  }

  def getFileMetadata(consignmentId: Option[UUID], selectedFileIds: Option[Set[UUID]] = None): Future[Map[UUID, FileMetadataValues]] =
    fileMetadataRepository.getFileMetadata(consignmentId, selectedFileIds).map { rows =>
      rows.groupBy(_.fileid).map { case (fileId, fileMetadata) =>
        fileId -> getFileMetadataValues(fileMetadata)
      }
    }

  def addDefaultCitableRefPrefix(fileIds: Seq[UUID], seriesName: String, userId: UUID): Future[Seq[FilemetadataRow]] = {
    val metadataInput = fileIds.map(fileId => AddFileMetadataInput(fileId, seriesName, userId, citableRefPrefix))
    fileMetadataRepository.addFileMetadata(metadataInput)
  }

  def consignmentLevelMetadata(consignment: Option[Consignment], consignmentMetadata: Seq[ConsignmentmetadataRow]): Map[String, String] = {
    val legalStatusMetadata = consignmentMetadata.find(_.propertyname == legalStatus).map(metadata => BaseSchema.legal_status -> metadata.value).toMap
    val citableRefPrefixMetadata = consignment.get.seriesName.map(seriesName => BaseSchema.citable_ref_prefix -> seriesName).toMap
    legalStatusMetadata ++ citableRefPrefixMetadata
  }
}

object FileMetadataService {

  val clientSideProperties: List[String] =
    List(
      clientSideChecksum,
      filePath,
      dateLastModified,
      fileSize,
      fileName,
      fileType
    )

  def getFileMetadataValues(fileMetadataRow: Seq[FilemetadataRow]): FileMetadataValues = {
    val propertyNameMap: Map[String, String] = fileMetadataRow.groupBy(_.propertyname).transform { (_, value) =>
      value.head.value
    }
    FileMetadataValues(
      propertyNameMap.get(clientSideChecksum),
      propertyNameMap.get(filePath),
      propertyNameMap.get(dateLastModified).map(d => Timestamp.valueOf(d).toLocalDateTime),
      propertyNameMap.get(fileSize).map(_.toLong),
      propertyNameMap.get(rightsCopyright),
      propertyNameMap.get(legalStatus),
      propertyNameMap.get(heldBy),
      propertyNameMap.get(language),
      propertyNameMap.get(foiExemptionCode),
      propertyNameMap.get(closurePeriod),
      propertyNameMap.get(closureStartDate).map(d => Timestamp.valueOf(d).toLocalDateTime),
      propertyNameMap.get(foiExemptionAsserted).map(d => Timestamp.valueOf(d).toLocalDateTime),
      propertyNameMap.get(titleClosed).map(_.toBoolean),
      propertyNameMap.get(descriptionClosed).map(_.toBoolean),
      propertyNameMap.get(assetId).map(UUID.fromString)
    )
  }

  case class FileMetadataValue(name: String, value: String)

  case class AddFileMetadataInput(fileId: UUID, value: String, userId: UUID, filePropertyName: String)

  case class File(
      fileId: UUID,
      uploadMatchId: Option[String] = None,
      fileType: Option[String] = None,
      fileName: Option[String] = None,
      fileReference: Option[Reference],
      parentId: Option[UUID] = None,
      parentReference: Option[Reference],
      metadata: FileMetadataValues,
      @GraphQLDeprecated("Should use 'fileStatuses' field")
      fileStatus: Option[String] = None,
      ffidMetadata: Option[FFIDMetadata],
      antivirusMetadata: Option[AntivirusMetadata],
      originalFilePath: Option[String] = None,
      fileMetadata: List[FileMetadataValue] = Nil,
      fileStatuses: List[FileStatus] = Nil,
      assetId: Option[UUID] = None
  )

  case class FileMetadataValues(
      sha256ClientSideChecksum: Option[String],
      clientSideOriginalFilePath: Option[String],
      clientSideLastModifiedDate: Option[LocalDateTime],
      clientSideFileSize: Option[Long],
      rightsCopyright: Option[String],
      legalStatus: Option[String],
      heldBy: Option[String],
      language: Option[String],
      foiExemptionCode: Option[String],
      closurePeriod: Option[String],
      closureStartDate: Option[LocalDateTime],
      foiExemptionAsserted: Option[LocalDateTime],
      titleClosed: Option[Boolean],
      descriptionClosed: Option[Boolean],
      assetId: Option[UUID]
  )
}
