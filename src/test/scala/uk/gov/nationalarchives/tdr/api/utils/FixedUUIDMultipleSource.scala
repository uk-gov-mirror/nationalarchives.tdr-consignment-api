package uk.gov.nationalarchives.tdr.api.utils

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.service.UUIDSource

object FixedUUIDMultipleSource extends UUIDSource {
  var idx = -1
  override def uuid: UUID = {
    idx += 1
    List(
    UUID.fromString("bbcae38a-4334-41a5-9564-99ad90b7254c"),
    UUID.fromString("bbcae38a-4334-41a5-9564-99ad90b7254c"),
    UUID.fromString("bbcae38a-4334-41a5-9564-99ad90b7254c")
  )(idx)
  }

}
