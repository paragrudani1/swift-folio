package io.swiftfolio.app.transfer

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.swiftfolio.core.S3Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * [TransferScheduler] adapter over WorkManager. One unique work chain per [TransferKind]
 * (APPEND_OR_REPLACE) keeps batches sequential. The batch manifest is written before
 * enqueueing; per-file outcomes come from [TransferManifestStore] and only fixed-size totals
 * travel in WorkInfo progress and output data.
 */
class WorkManagerTransferScheduler(
    private val workManager: WorkManager,
    private val manifestStore: TransferManifestStore,
    private val control: TransferControl,
    private val uploadSources: ContentUploadSources,
) : TransferScheduler {

    override val queue: Flow<TransferQueue> = workManager.getWorkInfosByTagFlow(BatchTags.TRANSFER)
        .map { infos -> TransferQueue(infos.mapNotNull(::toBatch)) }
        .flowOn(Dispatchers.IO)

    override suspend fun enqueueUploads(bucket: String, prefix: String, uris: List<String>): Int {
        if (bucket.isEmpty()) return 0
        val items = withContext(Dispatchers.IO) { uris.mapNotNull { uploadSources.describe(it, prefix) } }
        if (items.isEmpty()) return 0
        enqueue(TransferManifest(newBatchId(), TransferKind.UPLOAD, bucket, prefix, items))
        return items.size
    }

    override suspend fun enqueueDownloads(bucket: String, keys: List<String>): Int {
        if (bucket.isEmpty() || keys.isEmpty()) return 0
        val items = keys.map { key -> ManifestItem(name = S3Path.fileName(key), key = key) }
        val prefix = S3Path.normalizePrefix(keys.first().substringBeforeLast('/', missingDelimiterValue = ""))
        enqueue(TransferManifest(newBatchId(), TransferKind.DOWNLOAD, bucket, prefix, items))
        return items.size
    }

    override suspend fun cancelAll(kind: TransferKind) {
        val pending = withContext(Dispatchers.IO) {
            currentWork().filterNot { it.isFinished }.mapNotNull { manifestStore.load(it.batchId) }.filter { it.kind == kind }
        }
        workManager.cancelUniqueWork(uniqueName(kind))
        withContext(Dispatchers.IO) { pending.forEach(::releaseGrants) }
    }

    override fun cancelItem(batchId: String, itemIndex: Int) {
        control.cancel(ItemRef(batchId, itemIndex))
    }

    override suspend fun clearFinished() = withContext(Dispatchers.IO) {
        val finished = finishedBatchIds(currentWork())
        finished.mapNotNull(manifestStore::load).forEach(::releaseGrants)
        manifestStore.delete(finished)
        workManager.pruneWork()
        Unit
    }

    private suspend fun currentWork(): List<BatchWork> =
        workManager.getWorkInfosByTagFlow(BatchTags.TRANSFER).first().mapNotNull { info ->
            BatchTags.batchIdOf(info.tags)?.let { BatchWork(it, info.state.isFinished) }
        }

    /** Grants of picked documents are only needed until their batch can no longer run. */
    private fun releaseGrants(manifest: TransferManifest) {
        if (manifest.kind == TransferKind.UPLOAD) manifest.items.forEach(uploadSources::release)
    }

    private suspend fun enqueue(manifest: TransferManifest) {
        withContext(Dispatchers.IO) { manifestStore.save(manifest) }
        val request = OneTimeWorkRequestBuilder<TransferWorker>()
            .setInputData(workDataOf(TransferWorker.KEY_BATCH_ID to manifest.batchId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(BatchTags.TRANSFER)
            .addTag(BatchTags.forBatch(manifest.batchId))
            .build()
        workManager.enqueueUniqueWork(uniqueName(manifest.kind), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun toBatch(info: WorkInfo): TransferBatch? {
        val batchId = BatchTags.batchIdOf(info.tags) ?: return null
        val manifest = manifestStore.load(batchId) ?: return null
        val totals = TransferProgressCodec.decode(info.progress).takeUnless { info.state.isFinished }
        val snapshot = BatchSnapshot.assemble(manifest.items.size, manifestStore.outcomes(batchId), totals, unrecordedState(info.state))
        val items = manifest.items.zip(snapshot.items) { item, state ->
            TransferItem(item.name, item.key, item.sizeBytes, state.bytesDone, state.status, state.errorCode)
        }
        return TransferBatch(batchId, manifest.kind, manifest.bucket, manifest.prefix, items, isActive = !info.state.isFinished)
    }

    /** State of a file with no recorded outcome: not reached yet, or ended with the whole batch. */
    private fun unrecordedState(state: WorkInfo.State): ItemState = when (state) {
        WorkInfo.State.CANCELLED -> ItemState(TransferItemStatus.CANCELLED)
        WorkInfo.State.FAILED -> ItemState(TransferItemStatus.FAILED)
        WorkInfo.State.SUCCEEDED -> ItemState(TransferItemStatus.DONE)
        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> ItemState.QUEUED
    }

    private fun uniqueName(kind: TransferKind): String = UNIQUE_NAME_PREFIX + kind.name.lowercase()

    private fun newBatchId(): String = UUID.randomUUID().toString()

    private companion object {
        const val UNIQUE_NAME_PREFIX = "swift_folio_"
    }
}
