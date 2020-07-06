package uk.gov.nationalarchives.tdr.api.graphql

object DataExceptions {

  case class InputDataException(message: String, throwable: Option[Throwable] = None) extends Exception(message, throwable.orNull)

}
