package io.swiftfolio.app.transfer

import io.swiftfolio.core.TransferProgress
import kotlinx.coroutines.flow.Flow

/**
 * Background uploads and downloads that survive rotation, backgrounding and process death.
 *
 * Invariants: batches of one [TransferKind] run one after another in enqueue order; a batch
 * never fails as a whole because of one file (per-file failures are in [TransferItem.status]);
 * no credentials are ever written into scheduler storage.
 */
interface TransferScheduler {
    /** Every known batch (queued, running and recently finished), newest last. Never completes. */
    val queue: Flow<TransferQueue>

    /**
     * Queues an upload of the picked documents [uris] (content URIs as strings) into
     * [prefix] of [bucket]. Names and sizes are read from the content provider.
     * Returns the number of files queued (0 when none could be read).
     */
    suspend fun enqueueUploads(bucket: String, prefix: String, uris: List<String>): Int

    /** Queues a download of [keys] from [bucket] to the device Downloads folder. */
    suspend fun enqueueDownloads(bucket: String, keys: List<String>)

    /** Cancels every queued and running batch of [kind]. Finished files are kept. */
    fun cancelAll(kind: TransferKind)

    /** Cancels one file of a running or queued batch. No-op for finished files. */
    fun cancelItem(batchId: String, itemIndex: Int)

    /** Forgets finished batches so they disappear from [queue]. */
    fun clearFinished()
}

/** Direction of a transfer. */
enum class TransferKind { UPLOAD, DOWNLOAD }

/** Lifecycle of one file in a batch. */
enum class TransferItemStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    CANCELLED;

    /** True once the file will not change any more. */
    val isTerminal: Boolean get() = this == DONE || this == FAILED || this == CANCELLED
}

/**
 * One file of a batch.
 *
 * @property key object key uploaded to or downloaded from.
 * @property errorCode mono error code when [status] is FAILED.
 */
data class TransferItem(
    val name: String,
    val key: String,
    val sizeBytes: Long?,
    val bytesDone: Long,
    val status: TransferItemStatus,
    val errorCode: String? = null,
)

/**
 * A group of files enqueued together.
 *
 * @property prefix destination folder for uploads, source folder for downloads.
 * @property isActive true while queued or running.
 */
data class TransferBatch(
    val id: String,
    val kind: TransferKind,
    val bucket: String,
    val prefix: String,
    val items: List<TransferItem>,
    val isActive: Boolean,
) {
    /** Aggregate progress across [items]. */
    val progress: TransferProgress
        get() = TransferProgress.of(items.map { it.bytesDone to it.sizeBytes }, items.count { it.status.isTerminal })
}

/** Snapshot of all batches. */
data class TransferQueue(val batches: List<TransferBatch> = emptyList()) {
    /** Batches of [kind]. */
    fun ofKind(kind: TransferKind): List<TransferBatch> = batches.filter { it.kind == kind }

    /** True while any batch of [kind] is queued or running. */
    fun isActive(kind: TransferKind): Boolean = batches.any { it.kind == kind && it.isActive }

    /** Combined progress of the active batches of [kind]. */
    fun activeProgress(kind: TransferKind): TransferProgress {
        val items = batches.filter { it.kind == kind && it.isActive }.flatMap { it.items }
        return TransferProgress.of(items.map { it.bytesDone to it.sizeBytes }, items.count { it.status.isTerminal })
    }
}
