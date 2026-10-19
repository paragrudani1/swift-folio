package io.swiftfolio.app.transfer

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.swiftfolio.app.appContainer
import io.swiftfolio.core.TransferProgress

/**
 * WorkManager entry point for one transfer batch. Handles only WorkManager plumbing
 * (foreground promotion, progress data, result); the transfer itself is [BatchRunner].
 * Credentials come from the in-memory session or the encrypted store, never from input data.
 */
class TransferWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val container = context.appContainer
    private var lastForegroundUpdate = 0L

    override suspend fun doWork(): Result {
        val batchId = inputData.getString(KEY_BATCH_ID) ?: return Result.failure()
        val manifest = container.manifestStore.load(batchId) ?: return Result.failure()
        promote(manifest, TransferProgress.EMPTY)
        val repository = container.sessionManager.repositoryFor(manifest.bucket)
        val runner = BatchRunner(manifest, container.transferControl, container.uploadSources, container.downloadTargets)
        val result = try {
            runner.run(repository) { snapshot -> report(manifest, snapshot) }
        } finally {
            if (repository != null && !container.sessionManager.isSessionRepository(repository)) repository.close()
            container.transferControl.forget(batchId)
        }
        container.notifications.showFinished(manifest, result)
        return Result.success(TransferProgressCodec.encode(result.snapshot))
    }

    private suspend fun report(manifest: TransferManifest, snapshot: TransferProgressCodec.Snapshot) {
        setProgress(TransferProgressCodec.encode(snapshot))
        val now = SystemClock.elapsedRealtime()
        if (now - lastForegroundUpdate >= FOREGROUND_UPDATE_MILLIS) {
            lastForegroundUpdate = now
            val progress = TransferProgress.of(
                snapshot.bytesDone.zip(manifest.items) { bytes, item -> bytes to item.sizeBytes },
                snapshot.statuses.count { it.isTerminal },
            )
            promote(manifest, progress)
        }
    }

    /** Becomes a foreground service; on API 31+ this can be refused while the app is in the background. */
    private suspend fun promote(manifest: TransferManifest, progress: TransferProgress) {
        try {
            setForeground(container.notifications.foregroundInfo(id, manifest, progress))
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
