package uk.gov.nationalarchives.tdr.api.graphql

import sangria.schema.{ObjectType, Schema}
import uk.gov.nationalarchives.tdr.api.graphql.fields.{ClientFileMetadataFields, ConsignmentFields, SeriesFields, TransferAgreementFields}

object GraphQlTypes {

  private val QueryType = ObjectType("Query", SeriesFields.queryFields ++ ConsignmentFields.queryFields)
  private val MutationType = ObjectType("Mutation",
    SeriesFields.mutationFields
      ++ ConsignmentFields.mutationFields
      ++ TransferAgreementFields.mutationFields
      ++ ClientFileMetadataFields.mutationFields)

  val schema: Schema[ConsignmentApiContext, Unit] = Schema(QueryType, Some(MutationType))
}
