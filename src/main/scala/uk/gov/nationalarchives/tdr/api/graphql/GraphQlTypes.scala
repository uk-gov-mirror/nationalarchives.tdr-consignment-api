package uk.gov.nationalarchives.tdr.api.graphql

import sangria.schema.{ObjectType, Schema}
import uk.gov.nationalarchives.tdr.api.graphql.fields._

object GraphQlTypes {

  private val QueryType = ObjectType("Query", SeriesFields.queryFields ++ ConsignmentFields.queryFields ++ TransferAgreementFields.queryFields)
  private val MutationType = ObjectType("Mutation",
    ConsignmentFields.mutationFields ++
    TransferAgreementFields.mutationFields ++
    ClientFileMetadataFields.mutationFields ++
    FileFields.mutationFields ++
    AntivirusMetadataFields.mutationFields ++
    FileMetadataFields.mutationFields
  )

  val schema: Schema[ConsignmentApiContext, Unit] = Schema(QueryType, Some(MutationType))
}
