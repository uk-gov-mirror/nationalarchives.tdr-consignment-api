package uk.gov.nationalarchives.tdr.api.utils

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.service.UUIDSource

class FixedUUIDSource extends UUIDSource {
  var idx = -1
  override def uuid: UUID = {
    idx += 1
    List(
      UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("7e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("8e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("9e3b76c4-1745-4467-8ac5-b4dd736e1b3e")
    )(idx)
  }

  def reset: Unit = idx = -1

}
