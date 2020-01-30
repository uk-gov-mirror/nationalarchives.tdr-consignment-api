package graphql

import graphql.fields.SeriesFields
import sangria.schema.{ObjectType, Schema}

object GraphQlTypes {

  private val QueryType = ObjectType("Query", SeriesFields.queryFields)

  val schema: Schema[ConsignmentApiContext, Unit] = Schema(QueryType)
}
