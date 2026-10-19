package io.swiftfolio.app.transfer

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.swiftfolio.core.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager entry point for one transfer batch. Handles only WorkManager plumbing
 * (foreground promotion, progress data, result); the transfer itself is [BatchRunner].
 * Built by [TransferWorkerFactory] with its [dependencies]. Credentials come from the
 * in-memory session or the encrypted store, never from input data, and the worker uses its own
 * repository so signing out or reconnecting cannot close it mid-transfer.
 */
class TransferWorker(
    context: Context,
    params: WorkerParameters,
    private val dependencies: TransferWorkerDependencies,
) : CoroutineWorker(context, params) {
    private var lastForegroundUpdate = 0L

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val manifest = inputData.getString(KEY_BATCH_ID)?.let(dependencies.manifestStore::load)
        if (manifest == null) {
            // Succeed so the batches appended behind this one in the unique chain still run.
            Log.w(TAG, "Batch manifest missing; nothing to transfer")
            return@withContext Result.success()
        }
        promote(manifest, TransferProgress.EMPTY)
        val snapshot = runBatch(manifest)
        dependencies.notifications.showFinished(manifest, snapshot, dependencies.downloadTargets.location)
        Result.success(TransferProgressCodec.encode(snapshot.totals()))
    }

    private suspend fun runBatch(manifest: TransferManifest): BatchSnapshot {
        val repository = dependencies.credentials.configFor(manifest.bucket)?.let(dependencies.repositoryFactory::create)
        val runner = with(dependencies) { BatchRunner(manifest, control, uploadSources, downloadTargets, manifestStore) }
        return try {
            runner.run(repository) { snapshot -> report(manifest, snapshot) }
        } finally {
            repository?.close()
            dependencies.control.forget(manifest.batchId)
            // A stop by the system is retried later and still needs the grants; user cancels
            // are released by the scheduler.
            if (!isStopped) releaseGrants(manifest)
        }
    }

    private fun releaseGrants(manifest: TransferManifest) {
        if (manifest.kind == TransferKind.UPLOAD) manifest.items.forEach(dependencies.uploadSources::release)
    }

    private suspend fun report(manifest: TransferManifest, snapshot: BatchSnapshot) {
        setProgress(TransferProgressCodec.encode(snapshot.totals()))
        val now = SystemClock.elapsedRealtime()
        if (now - lastForegroundUpdate >= FOREGROUND_UPDATE_MILLIS) {
            lastForegroundUpdate = now
            promote(manifest, snapshot.progress(manifest.items.map { it.sizeBytes }))
        }
    }

    /** Becomes a foreground service; on API 31+ this can be refused while the app is in the background. */
    private suspend fun promote(manifest: TransferManifest, progress: TransferProgress) {
        try {
            setForeground(dependencies.notifications.foregroundInfo(id, manifest, progress))
        } catch (refused: IllegalStateException) {
            Log.w(TAG, "Continuing without foreground service: ${refused::class.java.simpleName}")
        }
    }

    companion object {
        /** Input key holding the batch (manifest) id. */
        const val KEY_BATCH_ID = "batch_id"
        private const val TAG = "TransferWorker"
        private const val FOREGROUND_UPDATE_MILLIS = 1_000L
    }
}
