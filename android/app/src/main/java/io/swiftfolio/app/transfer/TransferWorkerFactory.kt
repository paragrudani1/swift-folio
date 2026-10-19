package io.swiftfolio.app.transfer

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.swiftfolio.app.data.S3RepositoryFactory
import io.swiftfolio.core.ConnectionConfig

/** Supplies the credentials a background transfer of one bucket may use. */
fun interface TransferCredentials {
    /** Config for [bucket] from the active session or the encrypted store, or null when there is none. */
    suspend fun configFor(bucket: String): ConnectionConfig?
}

/** Everything a [TransferWorker] needs, handed over by the composition root. */
class TransferWorkerDependencies(
    val manifestStore: TransferManifestStore,
    val credentials: TransferCredentials,
    val repositoryFactory: S3RepositoryFactory,
    val control: TransferControl,
    val uploadSources: UploadSourceProvider,
    val downloadTargets: DownloadTargets,
    val notifications: TransferNotifications,
)

/**
 * Builds [TransferWorker]s with constructor-injected [dependencies]. Registered through
 * `Configuration.Provider` in the Application; returns null for any other worker class so
 * WorkManager falls back to its default factory.
 */
class TransferWorkerFactory(private val dependencies: TransferWorkerDependencies) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == TransferWorker::class.java.name) TransferWorker(appContext, workerParameters, dependencies) else null
}
