package uk.gov.nationalarchives.tdr.api.graphql

import uk.gov.nationalarchives.tdr.api.graphql.fields.{ConsignmentFields, SeriesFields, TransferAgreementFields}
import sangria.schema.{ObjectType, Schema}

object GraphQlTypes {

  private val QueryType = ObjectType("Query", SeriesFields.queryFields)
  private val MutationType = ObjectType("Mutation", SeriesFields.mutationFields ++ ConsignmentFields.mutationFields ++ TransferAgreementFields.mutationFields)

  val schema: Schema[ConsignmentApiContext, Unit] = Schema(QueryType, Some(MutationType))
}
