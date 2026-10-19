package io.swiftfolio.app.transfer

import androidx.work.Data

/**
 * Encodes [BatchTotals] into WorkManager [Data] and back. Only five fixed-size values are
 * written, whatever the batch size, so `Data`'s 10 KB limit can never be reached. Per-file
 * states live in [TransferManifestStore].
 */
object TransferProgressCodec {
    private const val KEY_FILES_DONE = "files_done"
    private const val KEY_FILES_TOTAL = "files_total"
    private const val KEY_BYTES_DONE = "bytes_done"
    private const val KEY_RUNNING_INDEX = "running_index"
    private const val KEY_RUNNING_BYTES = "running_bytes"
    private const val ABSENT = -1

    /** Encodes [totals]; a null running index is stored as -1. */
    fun encode(totals: BatchTotals): Data = Data.Builder()
        .putInt(KEY_FILES_DONE, totals.filesDone)
        .putInt(KEY_FILES_TOTAL, totals.filesTotal)
        .putLong(KEY_BYTES_DONE, totals.bytesDone)
        .putInt(KEY_RUNNING_INDEX, totals.runningIndex ?: ABSENT)
        .putLong(KEY_RUNNING_BYTES, totals.runningBytes)
        .build()

    /** Decodes [data], or null when it holds no totals (the worker has not reported yet). */
    fun decode(data: Data): BatchTotals? {
        val filesTotal = data.getInt(KEY_FILES_TOTAL, ABSENT)
        if (filesTotal == ABSENT) return null
        return BatchTotals(
            filesDone = data.getInt(KEY_FILES_DONE, 0),
            filesTotal = filesTotal,
            bytesDone = data.getLong(KEY_BYTES_DONE, 0L),
            runningIndex = data.getInt(KEY_RUNNING_INDEX, ABSENT).takeIf { it != ABSENT },
            runningBytes = data.getLong(KEY_RUNNING_BYTES, 0L),
        )
    }
}
