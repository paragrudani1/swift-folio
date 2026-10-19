package io.swiftfolio.app.transfer

import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.core.ErrorSignal
import io.swiftfolio.core.S3ErrorMapper
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Transfers the files of one [TransferManifest] sequentially. Knows nothing about
 * WorkManager: progress goes out through `onUpdate`, which is throttled to one call per
 * [TICK_MILLIS] while a file is moving plus one call per finished file.
 *
 * Each finished file is written to the [OutcomeLog] before the next one starts. A re-run of
 * the same batch (WorkManager restarts work after process death) skips files that are
 * already DONE or CANCELLED, so nothing is uploaded or downloaded twice; FAILED files are retried.
 */
class BatchRunner(
    private val manifest: TransferManifest,
    private val control: TransferControl,
    private val uploadSources: UploadSourceProvider,
    private val downloadTargets: DownloadTargets,
    private val outcomeLog: OutcomeLog,
) {
    /** Runs every unfinished file; [repository] null means the session is gone and those files fail. */
    suspend fun run(repository: S3Repository?, onUpdate: suspend (BatchSnapshot) -> Unit): BatchSnapshot {
        val recorded = outcomeLog.outcomes(manifest.batchId)
        var snapshot = BatchSnapshot.assemble(manifest.items.size, recorded, totals = null, pending = ItemState.QUEUED)
        manifest.items.forEachIndexed { index, item ->
            val previous = recorded[index]
            if (previous != null && previous.status in SKIPPED_ON_RERUN) return@forEachIndexed
            val state = when {
                control.isCancelled(ItemRef(manifest.batchId, index)) -> ItemState(TransferItemStatus.CANCELLED)
                repository == null -> ItemState(TransferItemStatus.FAILED, errorCode = SESSION_EXPIRED)
                else -> runItem(repository, index, item) { bytes ->
                    onUpdate(snapshot.with(index, ItemState(TransferItemStatus.RUNNING, bytes)))
                }
            }
            outcomeLog.record(manifest.batchId, index, state)
            snapshot = snapshot.with(index, state)
            onUpdate(snapshot)
        }
        return snapshot
    }

    /** Runs one file, cancelling it (but not the batch) when the user cancels that file. */
    private suspend fun runItem(
        repository: S3Repository,
        index: Int,
        item: ManifestItem,
        report: suspend (Long) -> Unit,
    ): ItemState = coroutineScope {
        val bytes = AtomicLong(0)
        val transfer = async { transferItem(repository, item, bytes) }
        val ticker = launch { tick(bytes, report) }
        val watcher = launch {
            control.cancelledItems.first { ItemRef(manifest.batchId, index) in it }
            transfer.cancel()
        }
        try {
            transfer.await()
        } catch (cancelled: CancellationException) {
            if (!isActive) throw cancelled
            ItemState(TransferItemStatus.CANCELLED, bytes.get())
        } finally {
            ticker.cancel()
            watcher.cancel()
        }
    }

    private suspend fun CoroutineScope.tick(bytes: AtomicLong, report: suspend (Long) -> Unit) {
        report(0)
        while (isActive) {
            delay(TICK_MILLIS)
            report(bytes.get())
        }
    }

    /** One file; any non-cancellation failure becomes a FAILED state so the batch continues. */
    private suspend fun transferItem(repository: S3Repository, item: ManifestItem, bytes: AtomicLong): ItemState =
        try {
            when (manifest.kind) {
                TransferKind.UPLOAD -> repository.upload(uploadSources.open(item), item.key) { sent -> bytes.set(sent) }.toState(bytes.get())
                TransferKind.DOWNLOAD -> download(repository, item, bytes)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ItemState(TransferItemStatus.FAILED, errorCode = S3ErrorMapper.map(ErrorSignal.fromThrowable(error)).code)
        }

    private suspend fun download(repository: S3Repository, item: ManifestItem, bytes: AtomicLong): ItemState {
        val target = downloadTargets.create(item.name)
        val result = try {
            target.openOutputStream().use { sink -> repository.download(item.key, sink) { written, _ -> bytes.set(written) } }
        } catch (error: Throwable) {
            target.discard() // Never leave a partial file behind, including on cancellation.
            throw error
        }
        if (result is S3Result.Success) target.commit() else target.discard()
        return result.toState(bytes.get()).let { state ->
            if (result is S3Result.Success) state.copy(savedUri = target.shareableUri) else state
        }
    }

    private fun <T> S3Result<T>.toState(bytesDone: Long): ItemState = when (this) {
        is S3Result.Success -> ItemState(TransferItemStatus.DONE, bytesDone)
        is S3Result.Failure -> ItemState(TransferItemStatus.FAILED, bytesDone, error.code)
    }

    private companion object {
        const val TICK_MILLIS = 300L
        const val SESSION_EXPIRED = "SessionExpired"
        val SKIPPED_ON_RERUN = setOf(TransferItemStatus.DONE, TransferItemStatus.CANCELLED)
    }
}
