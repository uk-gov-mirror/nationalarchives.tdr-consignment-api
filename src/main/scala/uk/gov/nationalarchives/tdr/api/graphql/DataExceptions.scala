package uk.gov.nationalarchives.tdr.api.graphql

object DataExceptions {

  case class InputDataException(message: String, throwable: Throwable = null) extends Exception(message)

}
