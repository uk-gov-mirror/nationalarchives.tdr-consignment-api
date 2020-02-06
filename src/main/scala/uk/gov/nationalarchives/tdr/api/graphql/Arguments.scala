package uk.gov.nationalarchives.tdr.api.graphql

import sangria.schema.{Argument, OptionInputType, StringType}

object Arguments {

  val BodyArg = Argument("body", OptionInputType(StringType))

}
