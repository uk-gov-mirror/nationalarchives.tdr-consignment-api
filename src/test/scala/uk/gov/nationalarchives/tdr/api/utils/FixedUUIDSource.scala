package uk.gov.nationalarchives.tdr.api.utils

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.service.UUIDSource

class FixedUUIDSource extends UUIDSource {
  override def uuid: UUID = UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e")

}
