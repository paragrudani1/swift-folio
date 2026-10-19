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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * [TransferScheduler] adapter over WorkManager. One unique work chain per [TransferKind]
 * (APPEND_OR_REPLACE) keeps batches sequential; the batch manifest is written before
 * enqueueing and the per-file state travels in WorkInfo progress/output data.
 */
class WorkManagerTransferScheduler(
    private val workManager: WorkManager,
    private val manifestStore: TransferManifestStore,
    private val control: TransferControl,
    private val uploadSources: ContentUploadSources,
) : TransferScheduler {

    @Volatile
    private var knownBatchIds: Set<String> = emptySet()

    override val queue: Flow<TransferQueue> = workManager.getWorkInfosByTagFlow(TAG_TRANSFER).map { infos ->
        val batches = infos.mapNotNull { info -> toBatch(info) }
        knownBatchIds = batches.map { it.id }.toSet()
        TransferQueue(batches)
    }.flowOn(Dispatchers.IO)

    override suspend fun enqueueUploads(bucket: String, prefix: String, uris: List<String>): Int {
        val items = withContext(Dispatchers.IO) { uris.mapNotNull { uploadSources.describe(it, prefix) } }
        if (items.isEmpty()) return 0
        enqueue(TransferManifest(newBatchId(), TransferKind.UPLOAD, bucket, prefix, items))
        return items.size
    }

    override suspend fun enqueueDownloads(bucket: String, keys: List<String>) {
        if (keys.isEmpty()) return
        val items = keys.map { key -> ManifestItem(name = S3Path.fileName(key), key = key) }
        val prefix = S3Path.normalizePrefix(keys.first().substringBeforeLast('/', missingDelimiterValue = ""))
        enqueue(TransferManifest(newBatchId(), TransferKind.DOWNLOAD, bucket, prefix, items))
    }

    override fun cancelAll(kind: TransferKind) {
        workManager.cancelUniqueWork(uniqueName(kind))
    }

    override fun cancelItem(batchId: String, itemIndex: Int) {
        control.cancel(ItemRef(batchId, itemIndex))
    }

    override fun clearFinished() {
        workManager.pruneWork()
        manifestStore.retainOnly(knownBatchIds)
    }

    private suspend fun enqueue(manifest: TransferManifest) {
        withContext(Dispatchers.IO) { manifestStore.save(manifest) }
        val request = OneTimeWorkRequestBuilder<TransferWorker>()
            .setInputData(workDataOf(TransferWorker.KEY_BATCH_ID to manifest.batchId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(TAG_TRANSFER)
            .addTag(BATCH_TAG_PREFIX + manifest.batchId)
            .build()
        workManager.enqueueUniqueWork(uniqueName(manifest.kind), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun toBatch(info: WorkInfo): TransferBatch? {
        val batchId = info.tags.firstOrNull { it.startsWith(BATCH_TAG_PREFIX) }?.removePrefix(BATCH_TAG_PREFIX) ?: return null
        val manifest = manifestStore.load(batchId) ?: return null
        val count = manifest.items.size
        val snapshot = TransferProgressCodec.decode(info.progress, count)
            ?: TransferProgressCodec.decode(info.outputData, count)
            ?: fallbackSnapshot(info.state, count)
        val items = manifest.items.mapIndexed { index, item ->
            TransferItem(item.name, item.key, item.sizeBytes, snapshot.bytesDone[index], snapshot.statuses[index], snapshot.errorCodes[index])
        }
        return TransferBatch(batchId, manifest.kind, manifest.bucket, manifest.prefix, items, isActive = !info.state.isFinished)
    }

    /** State when the worker has not reported yet (queued) or ended without output. */
    private fun fallbackSnapshot(state: WorkInfo.State, count: Int): TransferProgressCodec.Snapshot {
        val status = when (state) {
            WorkInfo.State.CANCELLED -> TransferItemStatus.CANCELLED
            WorkInfo.State.FAILED -> TransferItemStatus.FAILED
            WorkInfo.State.SUCCEEDED -> TransferItemStatus.DONE
            else -> TransferItemStatus.QUEUED
        }
        val queued = TransferProgressCodec.Snapshot.queued(count)
        return queued.copy(statuses = List(count) { status })
    }

    private fun uniqueName(kind: TransferKind): String = UNIQUE_NAME_PREFIX + kind.name.lowercase()

    private fun newBatchId(): String = UUID.randomUUID().toString()

    private companion object {
        const val TAG_TRANSFER = "swift_folio_transfer"
        const val BATCH_TAG_PREFIX = "batch:"
        const val UNIQUE_NAME_PREFIX = "swift_folio_"
    }
}
