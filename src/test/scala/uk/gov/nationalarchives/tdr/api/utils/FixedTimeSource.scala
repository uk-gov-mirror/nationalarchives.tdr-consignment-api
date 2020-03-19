package uk.gov.nationalarchives.tdr.api.utils

import java.time.Instant

import uk.gov.nationalarchives.tdr.api.service.TimeSource

object FixedTimeSource extends TimeSource {
  override def now: Instant = Instant.parse("2020-01-01T09:00:00Z")
}
