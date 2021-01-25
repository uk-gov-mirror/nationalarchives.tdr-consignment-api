package uk.gov.nationalarchives.tdr.api.utils

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.service.UUIDSource

class RandomUUIDSource extends UUIDSource {
  override def uuid: UUID = UUID.randomUUID()
}
