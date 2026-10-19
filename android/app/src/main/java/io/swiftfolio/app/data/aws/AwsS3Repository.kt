package io.swiftfolio.app.data.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObjects
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Response
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.writeToOutputStream
import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.DeleteProgress
import io.swiftfolio.app.data.DeleteSummary
import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.app.data.S3RepositoryFactory
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.data.UploadSource
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.CoreConstants
import io.swiftfolio.core.Listing
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.RawObject
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

/** Factory for the production, AWS SDK backed repository. */
object AwsS3RepositoryFactory : S3RepositoryFactory {
    override fun create(config: ConnectionConfig): S3Repository = AwsS3Repository(config)
}

/**
 * [S3Repository] adapter over the AWS SDK for Kotlin [S3Client].
 *
 * Buckets with dots use path-style addressing because virtual-hosted TLS certificates do
 * not cover dotted bucket names.
 */
internal class AwsS3Repository(private val config: ConnectionConfig) : S3Repository {
    override val bucket: String = config.bucket

    private val client: S3Client = S3Client {
        region = config.region
        forcePathStyle = config.bucket.contains('.')
        credentialsProvider = StaticCredentialsProvider(
            Credentials(accessKeyId = config.accessKeyId, secretAccessKey = config.secretAccessKey),
        )
    }

    private val uploader = AwsUploader(client, config.bucket)

    override suspend fun verifyConnection(onStep: (ConnectStep, StepStatus) -> Unit): S3Result<List<ObjectEntry>> {
        onStep(ConnectStep.REGION, StepStatus.RUNNING)
        val head = execute { client.headBucket { bucket = config.bucket } }
        if (head is S3Result.Failure && head.error.kind != ConnectErrorKind.ACCESS_DENIED) {
            onStep(ConnectStep.REGION, StepStatus.FAILED)
            return S3Result.Failure(head.error.asBucketLevel())
        }
        onStep(ConnectStep.REGION, StepStatus.DONE)
        onStep(ConnectStep.SIGNING, StepStatus.RUNNING)
        val listing = list(prefix = "")
        reportListingSteps(listing, onStep)
        return listing
    }

    override suspend fun list(prefix: String): S3Result<List<ObjectEntry>> = execute {
        val folders = mutableListOf<String>()
        val files = mutableListOf<RawObject>()
        forEachPage(listPrefix = prefix, delimited = true) { page ->
            page.commonPrefixes.orEmpty().mapNotNullTo(folders) { it.prefix }
            page.contents.orEmpty().mapNotNullTo(files) { item ->
                item.key?.let { RawObject(it, item.size ?: 0L, item.lastModified?.epochMilliseconds) }
            }
        }
        Listing.fromRaw(prefix, folders, files)
    }

    override suspend fun computeStorageUsage(): S3Result<StorageUsage> = execute {
        var bytes = 0L
        var count = 0L
        forEachPage(listPrefix = "", delimited = false) { page ->
            page.contents.orEmpty().forEach { bytes += it.size ?: 0L }
            count += page.contents.orEmpty().size
        }
        StorageUsage(totalBytes = bytes, objectCount = count)
    }

    override suspend fun createFolder(prefix: String): S3Result<Unit> = execute {
        require(S3Path.isFolderMarker(prefix)) { "Folder prefix must end with ${CoreConstants.DELIMITER}" }
        client.putObject {
            bucket = config.bucket
            key = prefix
            body = ByteStream.fromBytes(ByteArray(0))
        }
        Unit
    }

    override suspend fun delete(ids: Collection<String>, onProgress: (DeleteProgress) -> Unit): S3Result<DeleteSummary> =
        execute {
            val keys = expandToKeys(ids)
            val failed = mutableListOf<String>()
            var deleted = 0
            onProgress(DeleteProgress(deleted = 0, total = keys.size))
            keys.chunked(CoreConstants.DELETE_BATCH_SIZE).forEach { batch ->
                val batchFailures = deleteBatch(batch)
                failed += batchFailures
                deleted += batch.size - batchFailures.size
                onProgress(DeleteProgress(deleted = deleted, total = keys.size))
            }
            DeleteSummary(deleted = deleted, failedKeys = failed)
        }

    override suspend fun upload(source: UploadSource, key: String, onProgress: (Long) -> Unit): S3Result<Unit> =
        execute { uploader.upload(source, key, onProgress) }

    override suspend fun download(key: String, sink: OutputStream, onProgress: (Long, Long?) -> Unit): S3Result<Long> =
        execute {
            val objectKey = key
            val request = GetObjectRequest {
                bucket = config.bucket
                this.key = objectKey
            }
            client.getObject(request) { response ->
                val total = response.contentLength
                val counting = CountingOutputStream(sink) { written -> onProgress(written, total) }
                response.body?.writeToOutputStream(counting) ?: 0L
            }
        }

    override fun close() {
        client.close()
    }

    /** Runs [block] on the I/O dispatcher and converts failures into [S3Result.Failure]. */
    private suspend fun <T> execute(block: suspend () -> T): S3Result<T> = withContext(Dispatchers.IO) {
        try {
            S3Result.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            S3Result.Failure(error.toConnectError(config.region))
        }
    }

    /** SIGNING succeeds unless S3 rejected the signature; LISTING succeeds only with a listing. */
    private fun reportListingSteps(listing: S3Result<List<ObjectEntry>>, onStep: (ConnectStep, StepStatus) -> Unit) {
        val signatureAccepted = when (listing) {
            is S3Result.Success -> true
            is S3Result.Failure -> listing.error.kind == ConnectErrorKind.ACCESS_DENIED
        }
        onStep(ConnectStep.SIGNING, if (signatureAccepted) StepStatus.DONE else StepStatus.FAILED)
        if (signatureAccepted) {
            onStep(ConnectStep.LISTING, if (listing is S3Result.Success) StepStatus.DONE else StepStatus.FAILED)
        }
    }

    private suspend fun forEachPage(listPrefix: String, delimited: Boolean, onPage: (ListObjectsV2Response) -> Unit) {
        var token: String? = null
        do {
            currentCoroutineContext().ensureActive()
            val page = client.listObjectsV2 {
                bucket = config.bucket
                prefix = listPrefix.ifEmpty { null }
                delimiter = if (delimited) CoreConstants.DELIMITER else null
                maxKeys = CoreConstants.LIST_PAGE_SIZE
                continuationToken = token
            }
            onPage(page)
            token = page.nextContinuationToken.takeIf { page.isTruncated == true }
        } while (token != null)
    }

    private suspend fun expandToKeys(ids: Collection<String>): List<String> {
        val keys = linkedSetOf<String>()
        ids.forEach { id ->
            if (!S3Path.isFolderMarker(id)) {
                keys += id
            } else {
                forEachPage(listPrefix = id, delimited = false) { page -> page.contents.orEmpty().mapNotNullTo(keys) { it.key } }
                keys += id
            }
        }
        return keys.toList()
    }

    private suspend fun deleteBatch(batch: List<String>): List<String> {
        val response = client.deleteObjects {
            bucket = config.bucket
            delete = Delete {
                objects = batch.map { batchKey -> ObjectIdentifier { key = batchKey } }
                quiet = true
            }
        }
        return response.errors.orEmpty().mapNotNull { it.key }
    }
}

/** Treats a generic 404 from a bucket-level call as "no such bucket". */
private fun ConnectError.asBucketLevel(): ConnectError =
    if (kind == ConnectErrorKind.NOT_FOUND) copy(kind = ConnectErrorKind.NO_SUCH_BUCKET) else this
