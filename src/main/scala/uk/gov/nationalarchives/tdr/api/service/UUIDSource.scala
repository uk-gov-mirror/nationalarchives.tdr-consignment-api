package uk.gov.nationalarchives.tdr.api.service

import java.util.UUID

trait UUIDSource {
  def uuid: UUID
}

class RandomUUID extends UUIDSource {
  override def uuid: UUID = UUID.randomUUID()
}
