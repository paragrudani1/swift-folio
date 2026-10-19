package io.swiftfolio.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** ISO-like timestamps for metadata lines, e.g. `2026-10-17 09:12`. */
object TimeFormat {
    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    /** Formats [epochMillis] in [zone]; returns null when the time is unknown. */
    fun format(epochMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): String? =
        epochMillis?.let { FORMATTER.format(Instant.ofEpochMilli(it).atZone(zone)) }
}
