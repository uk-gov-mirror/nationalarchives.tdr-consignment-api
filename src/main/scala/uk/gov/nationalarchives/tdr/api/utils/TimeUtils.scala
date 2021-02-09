package uk.gov.nationalarchives.tdr.api.utils

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeUtils {
  implicit class ZonedDateTimeUtils(value: ZonedDateTime) {
    //Zoned Date Time truncated to 'seconds' precision to ensure consistent date format irrespective of input precision
    def toSecondsPrecisionString: String = value.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    def toTimestamp: Timestamp = Timestamp.valueOf(value.toLocalDateTime)
  }
}
