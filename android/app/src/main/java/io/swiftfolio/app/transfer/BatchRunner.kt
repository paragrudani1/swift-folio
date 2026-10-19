package io.swiftfolio.app.transfer

import android.net.Uri
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

/** What a finished batch produced. [savedUris] are openable downloads (API 29+ only). */
data class BatchResult(val snapshot: TransferProgressCodec.Snapshot, val savedUris: List<Uri>)

/**
 * Transfers the files of one [TransferManifest] sequentially. Knows nothing about
 * WorkManager: progress goes out through `onUpdate`, which is throttled to one call per
 * [TICK_MILLIS] while a file is moving plus one call per status change.
 */
class BatchRunner(
    private val manifest: TransferManifest,
    private val control: TransferControl,
    private val uploadSources: ContentUploadSources,
    private val downloadTargets: DownloadTargetFactory,
) {
    private data class ItemOutcome(val status: TransferItemStatus, val bytes: Long, val errorCode: String? = null, val uri: Uri? = null)

    /** Runs every file; [repository] null means the session is gone and every file fails. */
    suspend fun run(
        repository: S3Repository?,
        onUpdate: suspend (TransferProgressCodec.Snapshot) -> Unit,
    ): BatchResult {
        var snapshot = TransferProgressCodec.Snapshot.queued(manifest.items.size)
        val saved = mutableListOf<Uri>()
        manifest.items.forEachIndexed { index, item ->
            val outcome = when {
                control.isCancelled(ItemRef(manifest.batchId, index)) -> ItemOutcome(TransferItemStatus.CANCELLED, 0)
                repository == null -> ItemOutcome(TransferItemStatus.FAILED, 0, SESSION_EXPIRED)
                else -> runItem(repository, index, item) { bytes ->
                    onUpdate(snapshot.with(index, TransferItemStatus.RUNNING, bytes))
                }
            }
            outcome.uri?.let(saved::add)
            snapshot = snapshot.with(index, outcome.status, outcome.bytes, outcome.errorCode)
            onUpdate(snapshot)
        }
        return BatchResult(snapshot, saved)
    }

    /** Runs one file, cancelling it (but not the batch) when the user cancels that file. */
    private suspend fun runItem(
        repository: S3Repository,
        index: Int,
        item: ManifestItem,
        report: suspend (Long) -> Unit,
    ): ItemOutcome = coroutineScope {
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
            ItemOutcome(TransferItemStatus.CANCELLED, bytes.get())
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

    /** One file; any non-cancellation failure becomes a FAILED outcome so the batch continues. */
    private suspend fun transferItem(repository: S3Repository, item: ManifestItem, bytes: AtomicLong): ItemOutcome =
        try {
            when (manifest.kind) {
                TransferKind.UPLOAD -> upload(repository, item, bytes)
                TransferKind.DOWNLOAD -> download(repository, item, bytes)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failed(error)
        }

    private suspend fun upload(repository: S3Repository, item: ManifestItem, bytes: AtomicLong): ItemOutcome =
        try {
            repository.upload(uploadSources.open(item), item.key) { sent -> bytes.set(sent) }.toOutcome(bytes.get())
        } finally {
            uploadSources.release(item)
        }

    private suspend fun download(repository: S3Repository, item: ManifestItem, bytes: AtomicLong): ItemOutcome {
        val target = downloadTargets.create(item.name)
        val result = try {
            target.openOutputStream().use { sink -> repository.download(item.key, sink) { written, _ -> bytes.set(written) } }
        } catch (error: Throwable) {
            target.discard() // Never leave a partial file behind, including on cancellation.
            throw error
        }
        if (result is S3Result.Success) target.commit() else target.discard()
        return result.toOutcome(bytes.get()).copy(uri = target.uri.takeIf { result is S3Result.Success })
    }

    private fun failed(error: Exception): ItemOutcome =
        ItemOutcome(TransferItemStatus.FAILED, 0, S3ErrorMapper.map(ErrorSignal.fromThrowable(error)).code)

    private fun <T> S3Result<T>.toOutcome(bytesDone: Long): ItemOutcome = when (this) {
        is S3Result.Success -> ItemOutcome(TransferItemStatus.DONE, bytesDone)
        is S3Result.Failure -> ItemOutcome(TransferItemStatus.FAILED, bytesDone, error.code)
    }

    private companion object {
        const val TICK_MILLIS = 300L
        const val SESSION_EXPIRED = "SessionExpired"
    }
}
