package uk.gov.nationalarchives.tdr.api.utils

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.service.UUIDSource

class FixedUUIDSource extends UUIDSource {
  var idx = -1
  override def uuid: UUID = {
    idx += 1
    val uuids = List(
      UUID.fromString("6e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("7e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("8e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("9e3b76c4-1745-4467-8ac5-b4dd736e1b3e"),
      UUID.fromString("47e365a4-fc1e-4375-b2f6-dccb6d361f5f"),
      UUID.fromString("0824d319-9c58-4b15-81eb-b71fb460fa36"),
      UUID.fromString("15eddae1-f319-4da1-b2da-cda3bd8f840a"),
      UUID.fromString("854b1c99-5998-4126-a209-6f1bcc38bf74")
    )

    // The static uuids above are used when we're testing the correct id is returned.
    // Some methods call the uuid() method up to 20 times but we don't need to know what the uuid is as we're not testing for it.
    // So we just return a random uuid.
    if(idx >= uuids.size) {
      UUID.randomUUID()
    } else {
      uuids(idx)
    }
  }

  def reset: Unit = idx = -1

}
