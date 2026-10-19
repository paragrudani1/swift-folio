package io.swiftfolio.core

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Human-readable byte sizes with binary units, matching the web app's `formatFileSize`:
 * `0 B`, `512 B`, `1.5 KB`, `2 MB` (one decimal, trailing `.0` dropped).
 */
object ByteFormat {
    private val UNITS = listOf("B", "KB", "MB", "GB", "TB", "PB")

    /** Formats [bytes]; negative values are treated as zero. */
    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 ${UNITS.first()}"
        val exponent = (ln(bytes.toDouble()) / ln(CoreConstants.BYTES_PER_UNIT)).toInt().coerceIn(0, UNITS.lastIndex)
        val value = bytes / CoreConstants.BYTES_PER_UNIT.pow(exponent)
        val rounded = String.format(Locale.US, "%.1f", value).removeSuffix(".0")
        return "$rounded ${UNITS[exponent]}"
    }

    /** Whole percentage of [done] over [total], clamped to 0..100; 0 when total is unknown. */
    fun percent(done: Long, total: Long): Int {
        if (total <= 0) return 0
        return ((done.coerceAtLeast(0) * PERCENT) / total).toInt().coerceIn(0, PERCENT.toInt())
    }

    private const val PERCENT = 100L
}
