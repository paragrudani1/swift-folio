package io.swiftfolio.app.fakes

import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.DeleteProgress
import io.swiftfolio.app.data.DeleteSummary
import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.app.data.S3RepositoryFactory
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.data.UploadSource
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.CompletableDeferred
import java.io.OutputStream

/** In-memory [S3Repository] whose answers are set by the test. */
class FakeS3Repository(override val bucket: String = TestData.BUCKET) : S3Repository {
    var verifyResult: S3Result<List<ObjectEntry>> = S3Result.Success(TestData.root)

    /** When set, verification waits for it (to observe the screen before the session is bound). */
    var verifyGate: CompletableDeferred<Unit>? = null
    val listings = mutableMapOf<String, S3Result<List<ObjectEntry>>>("" to S3Result.Success(TestData.root))
    var storageResult: S3Result<StorageUsage> = S3Result.Success(StorageUsage(totalBytes = 4_120, objectCount = 2))
    var createFolderResult: S3Result<Unit> = S3Result.Success(Unit)
    var deleteFailures: List<String> = emptyList()
    var deleteFreedBytes: Long = 0
    var uploadResult: S3Result<Unit> = S3Result.Success(Unit)
    var downloadResult: S3Result<Long> = S3Result.Success(0)
    val listCalls = mutableListOf<String>()
    var storageCalls = 0
        private set
    val createdPrefixes = mutableListOf<String>()
    val deletedIds = mutableListOf<List<String>>()
    val uploadedKeys = mutableListOf<String>()
    val downloadedKeys = mutableListOf<String>()
    var closed = false
        private set

    override suspend fun verifyConnection(onStep: (ConnectStep, StepStatus) -> Unit): S3Result<List<ObjectEntry>> {
        verifyGate?.await()
        ConnectStep.entries.forEach { step ->
            onStep(step, StepStatus.RUNNING)
            onStep(step, if (verifyResult is S3Result.Success) StepStatus.DONE else StepStatus.FAILED)
        }
        return verifyResult
    }

    override suspend fun list(prefix: String): S3Result<List<ObjectEntry>> {
        listCalls += prefix
        return listings[prefix] ?: S3Result.Success(emptyList())
    }

    override suspend fun computeStorageUsage(): S3Result<StorageUsage> {
        storageCalls++
        return storageResult
    }

    override suspend fun createFolder(prefix: String): S3Result<Unit> {
        createdPrefixes += prefix
        return createFolderResult
    }

    override suspend fun delete(ids: Collection<String>, onProgress: (DeleteProgress) -> Unit): S3Result<DeleteSummary> {
        deletedIds += ids.toList()
        onProgress(DeleteProgress(deleted = ids.size, total = ids.size))
        return S3Result.Success(DeleteSummary(deleted = ids.size - deleteFailures.size, failedKeys = deleteFailures, freedBytes = deleteFreedBytes))
    }

    override suspend fun upload(source: UploadSource, key: String, onProgress: (Long) -> Unit): S3Result<Unit> {
        uploadedKeys += key
        source.sizeBytes?.let(onProgress)
        return uploadResult
    }

    override suspend fun download(key: String, sink: OutputStream, onProgress: (Long, Long?) -> Unit): S3Result<Long> {
        downloadedKeys += key
        return downloadResult
    }

    override fun close() {
        closed = true
    }
}

/** Factory that hands out [FakeS3Repository]s and records them. */
class FakeS3RepositoryFactory(private val build: (ConnectionConfig) -> FakeS3Repository = { FakeS3Repository(it.bucket) }) :
    S3RepositoryFactory {
    val created = mutableListOf<Pair<ConnectionConfig, FakeS3Repository>>()

    override fun create(config: ConnectionConfig): S3Repository = build(config).also { created += config to it }

    companion object {
        /** A factory whose repositories fail verification with [error]. */
        fun failing(error: ConnectError) = FakeS3RepositoryFactory { config ->
            FakeS3Repository(config.bucket).apply { verifyResult = S3Result.Failure(error) }
        }
    }
}
