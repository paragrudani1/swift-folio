package io.swiftfolio.app.transfer

import androidx.work.Data

/**
 * Encodes per-file progress into WorkManager [Data] (kept far below the 10 KB limit by
 * using primitive arrays) and back. Used for both progress and output data.
 */
object TransferProgressCodec {
    private const val KEY_STATUS = "status"
    private const val KEY_BYTES = "bytes"
    private const val KEY_ERRORS = "errors"

    /** Snapshot of all files of one batch, index-aligned with the manifest items. */
    data class Snapshot(
        val statuses: List<TransferItemStatus>,
        val bytesDone: List<Long>,
        val errorCodes: List<String?>,
    ) {
        /** Copy with file [index] updated. */
        fun with(index: Int, status: TransferItemStatus, bytes: Long, errorCode: String? = null): Snapshot = copy(
            statuses = statuses.toMutableList().also { it[index] = status },
            bytesDone = bytesDone.toMutableList().also { it[index] = bytes },
            errorCodes = errorCodes.toMutableList().also { it[index] = errorCode },
        )

        companion object {
            /** All [count] files queued with no progress. */
            fun queued(count: Int) = Snapshot(
                statuses = List(count) { TransferItemStatus.QUEUED },
                bytesDone = List(count) { 0L },
                errorCodes = List(count) { null },
            )
        }
    }

    /** Encodes [snapshot]. Empty error codes are stored as "" because Data arrays are non-null. */
    fun encode(snapshot: Snapshot): Data = Data.Builder()
        .putIntArray(KEY_STATUS, snapshot.statuses.map { it.ordinal }.toIntArray())
        .putLongArray(KEY_BYTES, snapshot.bytesDone.toLongArray())
        .putStringArray(KEY_ERRORS, Array<String?>(snapshot.errorCodes.size) { snapshot.errorCodes[it].orEmpty() })
        .build()

    /** Decodes [data] for a batch of [count] files, or null when [data] holds no snapshot. */
    fun decode(data: Data, count: Int): Snapshot? {
        val statuses = data.getIntArray(KEY_STATUS) ?: return null
        val bytes = data.getLongArray(KEY_BYTES) ?: return null
        val errors = data.getStringArray(KEY_ERRORS) ?: return null
        if (statuses.size != count || bytes.size != count || errors.size != count) return null
        return Snapshot(
            statuses = statuses.map { TransferItemStatus.entries.getOrElse(it) { TransferItemStatus.QUEUED } },
            bytesDone = bytes.toList(),
            errorCodes = errors.map { code -> code?.takeIf { it.isNotEmpty() } },
        )
    }
}
