package io.swiftfolio.app.transfer

import io.swiftfolio.core.TransferProgress
import kotlinx.serialization.Serializable

/**
 * State of one file of a batch.
 *
 * @property errorCode mono error code when [status] is FAILED.
 * @property savedUri openable content URI of a finished download (API 29+ or FileProvider), as text.
 */
@Serializable
data class ItemState(
    val status: TransferItemStatus,
    val bytesDone: Long = 0,
    val errorCode: String? = null,
    val savedUri: String? = null,
) {
    companion object {
        /** Not started yet. */
        val QUEUED = ItemState(TransferItemStatus.QUEUED)
    }
}

/**
 * Per-file states of one batch, index-aligned with the manifest items. Pure model: knows
 * nothing about WorkManager or how it is stored.
 */
data class BatchSnapshot(val items: List<ItemState>) {
    /** Number of files that will not change any more. */
    val finishedCount: Int get() = items.count { it.status.isTerminal }

    /** Index of the file being transferred, or null. */
    val runningIndex: Int? get() = items.indexOfFirst { it.status == TransferItemStatus.RUNNING }.takeIf { it >= 0 }

    /** Copy with file [index] replaced by [state]. */
    fun with(index: Int, state: ItemState): BatchSnapshot = copy(items = items.toMutableList().also { it[index] = state })

    /** Aggregate progress, given the manifest [sizes] (null when unknown). */
    fun progress(sizes: List<Long?>): TransferProgress =
        TransferProgress.of(items.zip(sizes) { state, size -> state.bytesDone to size }, finishedCount)

    /** Fixed-size totals for WorkManager progress data. */
    fun totals(): BatchTotals {
        val running = runningIndex
        return BatchTotals(
            filesDone = finishedCount,
            filesTotal = items.size,
            bytesDone = items.sumOf { it.bytesDone },
            runningIndex = running,
            runningBytes = running?.let { items[it].bytesDone } ?: 0L,
        )
    }

    companion object {
        /**
         * Rebuilds the view of a batch of [count] files from what the worker [recorded]
         * (finished files, by index) and its last reported [totals] (the running file).
         * Files with neither are shown as [pending].
         */
        fun assemble(count: Int, recorded: Map<Int, ItemState>, totals: BatchTotals?, pending: ItemState): BatchSnapshot =
            BatchSnapshot(
                List(count) { index ->
                    recorded[index]
                        ?: totals?.takeIf { it.runningIndex == index }?.let { ItemState(TransferItemStatus.RUNNING, it.runningBytes) }
                        ?: pending
                },
            )
    }
}

/**
 * Batch totals that WorkManager carries as progress and output data. Its size does not depend
 * on the number of files, so it always stays far below the 10 KB `Data` limit.
 *
 * @property runningIndex file currently moving, or null between files.
 * @property runningBytes bytes moved so far for [runningIndex].
 */
data class BatchTotals(
    val filesDone: Int,
    val filesTotal: Int,
    val bytesDone: Long,
    val runningIndex: Int?,
    val runningBytes: Long,
)
