package uk.gov.nationalarchives.tdr.api.graphql.validation

import java.util.UUID

trait UserOwnsFile {
  def fileId: UUID
}
