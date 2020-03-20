package uk.gov.nationalarchives.tdr.api.graphql

import uk.gov.nationalarchives.tdr.api.graphql.fields.{ClientFileMetadataFields, ConsignmentFields, FileFields, SeriesFields, TransferAgreementFields}
import sangria.schema.{ObjectType, Schema}

object GraphQlTypes {

  private val QueryType = ObjectType("Query", SeriesFields.queryFields ++ ConsignmentFields.queryFields ++ TransferAgreementFields.queryFields)
  private val MutationType = ObjectType("Mutation",
    SeriesFields.mutationFields ++
    ConsignmentFields.mutationFields ++
    TransferAgreementFields.mutationFields ++
    ClientFileMetadataFields.mutationFields ++
    FileFields.mutationFields
  )

  val schema: Schema[ConsignmentApiContext, Unit] = Schema(QueryType, Some(MutationType))
}
