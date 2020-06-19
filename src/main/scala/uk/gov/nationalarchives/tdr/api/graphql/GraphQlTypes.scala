package uk.gov.nationalarchives.tdr.api.graphql

import sangria.schema.{ObjectType, Schema}
import uk.gov.nationalarchives.tdr.api.graphql.fields._

object GraphQlTypes {

  private val UserQueryType = ObjectType("Query", SeriesFields.queryFields ++ ConsignmentFields.queryFields ++ TransferAgreementFields.queryFields)
  private val UserMutationType: ObjectType[ConsignmentApiContext, Unit] = ObjectType("Mutation",
    ConsignmentFields.mutationFields ++
    TransferAgreementFields.mutationFields ++
    ClientFileMetadataFields.mutationFields ++
    FileFields.mutationFields
  )

  private val AdminMutationType: ObjectType[ConsignmentApiContext, Unit] = ObjectType("Mutation",
    AntivirusMetadataFields.mutationFields ++
      FileMetadataFields.mutationFields
  )

  private val AllMutationType = ObjectType("Mutation",AdminMutationType.fieldsFn() ++ UserMutationType.fieldsFn())

  val userSchema: Schema[ConsignmentApiContext, Unit] = Schema(UserQueryType, Some(UserMutationType))

  //You can't have a query object without fields and the argument is mandatory so I'll put the user queries in.
  val adminSchema: Schema[ConsignmentApiContext, Unit] = Schema(UserQueryType, Some(AdminMutationType))

  //Needed for the schema gen. You can only have one schema set
  val schema = Schema(UserQueryType, Some(AllMutationType))
}
