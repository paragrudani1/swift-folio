package io.swiftfolio.core

/**
 * Aggregate progress of a batch of transfers.
 *
 * @property bytesTotal sum of known file sizes; files of unknown size count as zero.
 */
data class TransferProgress(
    val bytesDone: Long,
    val bytesTotal: Long,
    val filesDone: Int,
    val filesTotal: Int,
) {
    /** Progress in 0f..1f. Uses bytes when known, otherwise completed files. */
    val fraction: Float
        get() = when {
            bytesTotal > 0 -> (bytesDone.toDouble() / bytesTotal).toFloat().coerceIn(0f, 1f)
            filesTotal > 0 -> (filesDone.toFloat() / filesTotal).coerceIn(0f, 1f)
            else -> 0f
        }

    /** Whole percent of [fraction]. */
    val percent: Int get() = (fraction * PERCENT).toInt()

    companion object {
        private const val PERCENT = 100

        /** Nothing to transfer. */
        val EMPTY: TransferProgress = TransferProgress(0, 0, 0, 0)

        /**
         * Sums per-file progress.
         *
         * @param files pairs of (bytes done, total bytes or null when unknown).
         * @param finished number of files in a terminal state (done, failed or cancelled).
         */
        fun of(files: List<Pair<Long, Long?>>, finished: Int): TransferProgress = TransferProgress(
            bytesDone = files.sumOf { (done, total) -> if (total == null) 0L else done.coerceAtMost(total) },
            bytesTotal = files.sumOf { (_, total) -> total ?: 0L },
            filesDone = finished,
            filesTotal = files.size,
        )
    }
}
