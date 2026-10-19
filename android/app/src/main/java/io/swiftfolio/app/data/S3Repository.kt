package io.swiftfolio.app.data

import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Result
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Operations on one bucket. Implementations are bound to a single [ConnectionConfig].
 *
 * Contract for every suspend method:
 * - it never throws for S3, network or I/O failures; those come back as [S3Result.Failure]
 *   classified by the single error mapper;
 * - it does propagate coroutine cancellation, so callers can cancel long operations;
 * - it is safe to call from any dispatcher (implementations switch to I/O themselves).
 *
 * Naming: a *prefix* is a folder path ending in `/` (or empty for the root); a *key* is a
 * full object key.
 */
interface S3Repository {
    /** Bucket every call operates on. */
    val bucket: String

    /**
     * Checks that the bucket exists in the configured region and that the keys can list it,
     * reporting each [ConnectStep] through [onStep] in order. On success returns the root
     * listing so the browser can render it without a second request.
     */
    suspend fun verifyConnection(onStep: (ConnectStep, StepStatus) -> Unit): S3Result<List<ObjectEntry>>

    /** Lists the direct children of [prefix] (all pages, delimiter `/`). */
    suspend fun list(prefix: String): S3Result<List<ObjectEntry>>

    /** Sums every object in the bucket. Can take long on big buckets; cancellable per page. */
    suspend fun computeStorageUsage(): S3Result<StorageUsage>

    /** Creates an empty folder marker object at [prefix], which must end with `/`. */
    suspend fun createFolder(prefix: String): S3Result<Unit>

    /**
     * Deletes [ids]: keys are deleted as-is, prefixes (ending in `/`) recursively.
     * Runs DeleteObjects in batches of 1000 and reports progress after each batch.
     * Per-key failures are returned in [DeleteSummary.failedKeys], not as a Failure.
     */
    suspend fun delete(ids: Collection<String>, onProgress: (DeleteProgress) -> Unit): S3Result<DeleteSummary>

    /**
     * Uploads [source] to [key]: one PutObject up to 5 MiB, otherwise a multipart upload in
     * 5 MiB parts that is aborted on failure or cancellation. [onProgress] receives the
     * cumulative bytes sent.
     */
    suspend fun upload(source: UploadSource, key: String, onProgress: (Long) -> Unit): S3Result<Unit>

    /**
     * Streams object [key] into [sink] (the caller owns and closes it). [onProgress]
     * receives (bytes written, total bytes or null). Returns the number of bytes written.
     */
    suspend fun download(key: String, sink: OutputStream, onProgress: (Long, Long?) -> Unit): S3Result<Long>

    /** Releases the underlying HTTP client. Further calls are not allowed. */
    fun close()
}

/** Creates repositories; the seam that lets tests swap the AWS-backed implementation. */
fun interface S3RepositoryFactory {
    /** Returns a new repository bound to [config]. Must not perform network I/O. */
    fun create(config: ConnectionConfig): S3Repository
}

/** A readable upload payload. */
interface UploadSource {
    /** Size in bytes, or null when the provider does not report it. */
    val sizeBytes: Long?

    /** MIME type to store with the object, or null to let S3 default it. */
    val contentType: String?

    /** Opens a fresh stream positioned at the start. The caller closes it. */
    @Throws(IOException::class)
    fun openStream(): InputStream
}

/** The three visible steps of the Connect checklist, in execution order. */
enum class ConnectStep { REGION, SIGNING, LISTING }

/** State of one checklist step. */
enum class StepStatus { PENDING, RUNNING, DONE, FAILED }

/** Result of [S3Repository.computeStorageUsage]. */
data class StorageUsage(val totalBytes: Long, val objectCount: Long)

/** Progress of a running delete: [deleted] of [total] objects so far. */
data class DeleteProgress(val deleted: Int, val total: Int)

/** Outcome of a delete; [failedKeys] is empty when everything was removed. */
data class DeleteSummary(val deleted: Int, val failedKeys: List<String>)
