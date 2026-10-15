package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.tdr.api.service.ReferenceGeneratorService.Reference
import uk.gov.nationalarchives.tdr.schema.generated.BaseSchema
import uk.gov.nationalarchives.tdr.schemautils.ConfigUtils

object FileMetadataPropertyName {

  val metadataConfig: ConfigUtils.MetadataConfiguration = ConfigUtils.loadConfiguration
  val tdrDataLoadHeaderToPropertyMapper: String => String = metadataConfig.propertyToOutputMapper("tdrDataLoadHeader")
  val systemProperties: Seq[Reference] = metadataConfig.getPropertiesByPropertyType("System")

  val clientSideChecksum: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.client_side_checksum)
  val filePath: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.file_path)
  val dateLastModified: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.date_last_modified)
  val fileSize: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.file_size)
  val fileName: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.file_name)
  val fileType: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.file_type)
  val fileReference: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.file_reference)
  val parentReference: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.parent_reference)
  val uuid: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.UUID)
  val assetId: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.asset_id)
  val rightsCopyright: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.rights_copyright)
  val legalStatus: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.legal_status)
  val heldBy: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.held_by)
  val language: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.language)
  val foiExemptionCode: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.foi_exemption_code)
  val closurePeriod: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.closure_period)
  val closureStartDate: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.closure_start_date)
  val closureType: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.closure_type)
  val foiExemptionAsserted: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.foi_exemption_asserted)
  val titleClosed: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.title_closed)
  val descriptionClosed: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.description_closed)
  val citableRefPrefix: String = tdrDataLoadHeaderToPropertyMapper(BaseSchema.citable_ref_prefix)
}
