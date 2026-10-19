package io.swiftfolio.app.ui.browser

import io.swiftfolio.app.transfer.TransferBatch
import io.swiftfolio.app.transfer.TransferItem
import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.core.TransferProgress

/** One file of the transfer queue, addressed by batch and index for per-file cancel. */
data class TransferRowModel(val batchId: String, val index: Int, val kind: TransferKind, val item: TransferItem)

/** Title of the transfer sheet, from the kinds of batches it shows. */
sealed interface QueueHeading {
    /** Only uploads, into [prefix] (of the newest batch). */
    data class UploadingTo(val prefix: String) : QueueHeading

    /** Only downloads. */
    data object Downloading : QueueHeading

    /** Uploads and downloads together, or nothing. */
    data object Transfers : QueueHeading
}

/**
 * What the transfer sheet and the command-bar chip show. While anything is queued or running
 * only active batches are listed; afterwards the finished ones stay until cleared.
 *
 * @property progress aggregate over [rows].
 * @property isActive true while any batch is queued or running.
 * @property hasFinished true when "Clear finished" has something to remove.
 */
data class TransferQueueSummary(
    val rows: List<TransferRowModel> = emptyList(),
    val progress: TransferProgress = TransferProgress.EMPTY,
    val isActive: Boolean = false,
    val heading: QueueHeading = QueueHeading.Transfers,
    val hasFinished: Boolean = false,
) {
    companion object {
        /** Summarises [batches] (newest last). */
        fun of(batches: List<TransferBatch>): TransferQueueSummary {
            val active = batches.filter { it.isActive }
            val shown = active.ifEmpty { batches }
            val rows = shown.flatMap { batch -> batch.items.mapIndexed { index, item -> TransferRowModel(batch.id, index, batch.kind, item) } }
            return TransferQueueSummary(
                rows = rows,
                progress = TransferProgress.of(rows.map { it.item.bytesDone to it.item.sizeBytes }, rows.count { it.item.status.isTerminal }),
                isActive = active.isNotEmpty(),
                heading = headingOf(shown),
                hasFinished = batches.any { !it.isActive },
            )
        }

        private fun headingOf(shown: List<TransferBatch>): QueueHeading = when (shown.map { it.kind }.toSet()) {
            setOf(TransferKind.UPLOAD) -> QueueHeading.UploadingTo(shown.last().prefix)
            setOf(TransferKind.DOWNLOAD) -> QueueHeading.Downloading
            else -> QueueHeading.Transfers
        }
    }
}
