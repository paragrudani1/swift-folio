package io.swiftfolio.app.transfer

import kotlinx.coroutines.flow.Flow

/**
 * Background uploads and downloads that survive rotation, backgrounding and process death.
 *
 * Invariants: batches of one [TransferKind] run one after another in enqueue order; a batch
 * never fails as a whole because of one file (per-file failures are in [TransferItem.status]);
 * a batch restarted after process death skips files that already finished; no credentials are
 * ever written into scheduler storage. Suspend methods do their I/O off the main thread.
 */
interface TransferScheduler {
    /** Every known batch (queued, running and recently finished), newest last. Never completes. */
    val queue: Flow<TransferQueue>

    /**
     * Queues an upload of the picked documents [uris] (content URIs as strings) into
     * [prefix] of [bucket]. Names and sizes are read from the content provider.
     * Returns the number of files queued (0 when none could be read or [bucket] is empty).
     */
    suspend fun enqueueUploads(bucket: String, prefix: String, uris: List<String>): Int

    /**
     * Queues a download of [keys] from [bucket] to the device Downloads folder. Returns the
     * number of files queued (0 when [keys] or [bucket] is empty).
     */
    suspend fun enqueueDownloads(bucket: String, keys: List<String>): Int

    /**
     * Cancels every queued and running batch of [kind] and releases the read grants of their
     * picked documents. Files that already finished are kept.
     */
    suspend fun cancelAll(kind: TransferKind)

    /** Cancels one file of a running or queued batch. No-op for finished files. */
    fun cancelItem(batchId: String, itemIndex: Int)

    /**
     * Forgets batches whose work has ended so they disappear from [queue], deleting their
     * stored files. Queued and running batches, including one enqueued concurrently, are kept.
     */
    suspend fun clearFinished()
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
)

/** Snapshot of all batches. */
data class TransferQueue(val batches: List<TransferBatch> = emptyList()) {
    /** Batches of [kind]. */
    fun ofKind(kind: TransferKind): List<TransferBatch> = batches.filter { it.kind == kind }
}
