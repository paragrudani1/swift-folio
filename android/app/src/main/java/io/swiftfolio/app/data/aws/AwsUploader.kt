package io.swiftfolio.app.data.aws

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.abortMultipartUpload
import aws.sdk.kotlin.services.s3.completeMultipartUpload
import aws.sdk.kotlin.services.s3.createMultipartUpload
import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.sdk.kotlin.services.s3.model.CompletedMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.uploadPart
import aws.smithy.kotlin.runtime.content.ByteStream
import io.swiftfolio.app.data.UploadSource
import io.swiftfolio.core.UploadPlanner
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Sends one [UploadSource] to S3: a single PutObject when it fits in one part, otherwise
 * CreateMultipartUpload / UploadPart / CompleteMultipartUpload with CRC32 part checksums.
 * A multipart upload that fails or is cancelled is aborted so no orphaned parts are billed.
 *
 * Throws SDK and I/O exceptions; the repository maps them.
 */
internal class AwsUploader(private val client: S3Client, private val bucketName: String) {

    /** Uploads [source] to [objectKey], reporting cumulative bytes sent. Blocking reads: call on I/O. */
    suspend fun upload(source: UploadSource, objectKey: String, onProgress: (Long) -> Unit) {
        val partSize = UploadPlanner.partSize(source.sizeBytes).toInt()
        source.openStream().use { input ->
            val chunks = ChunkReader(input, partSize)
            if (chunks.isSingleChunk) {
                val bytes = chunks.next() ?: ByteArray(0)
                putSingle(objectKey, bytes, source.contentType)
                onProgress(bytes.size.toLong())
            } else {
                uploadMultipart(objectKey, source.contentType, chunks, onProgress)
            }
        }
    }

    private suspend fun putSingle(objectKey: String, bytes: ByteArray, mimeType: String?) {
        client.putObject {
            bucket = bucketName
            key = objectKey
            contentType = mimeType
            body = ByteStream.fromBytes(bytes)
        }
    }

    private suspend fun uploadMultipart(objectKey: String, mimeType: String?, chunks: ChunkReader, onProgress: (Long) -> Unit) {
        val uploadId = client.createMultipartUpload {
            bucket = bucketName
            key = objectKey
            contentType = mimeType
            checksumAlgorithm = ChecksumAlgorithm.Crc32
        }.uploadId ?: error("CreateMultipartUpload returned no upload id")
        try {
            val parts = sendParts(objectKey, uploadId, chunks, onProgress)
            client.completeMultipartUpload {
                bucket = bucketName
                key = objectKey
                this.uploadId = uploadId
                multipartUpload = CompletedMultipartUpload { this.parts = parts }
            }
        } catch (failure: Throwable) {
            abortQuietly(objectKey, uploadId, failure)
            throw failure
        }
    }

    private suspend fun sendParts(objectKey: String, uploadId: String, chunks: ChunkReader, onProgress: (Long) -> Unit): List<CompletedPart> {
        val parts = mutableListOf<CompletedPart>()
        var sent = 0L
        var number = 1
        while (true) {
            val chunk = chunks.next() ?: break
            val response = client.uploadPart {
                bucket = bucketName
                key = objectKey
                this.uploadId = uploadId
                partNumber = number
                checksumAlgorithm = ChecksumAlgorithm.Crc32
                body = ByteStream.fromBytes(chunk)
            }
            val completedNumber = number
            parts += CompletedPart {
                partNumber = completedNumber
                eTag = response.eTag
                checksumCrc32 = response.checksumCrc32
            }
            sent += chunk.size
            onProgress(sent)
            number++
        }
        return parts
    }

    /** Aborts the upload even when the caller was cancelled; an abort failure is attached to [cause]. */
    private suspend fun abortQuietly(objectKey: String, uploadId: String, cause: Throwable) {
        withContext(NonCancellable) {
            try {
                client.abortMultipartUpload {
                    bucket = bucketName
                    key = objectKey
                    this.uploadId = uploadId
                }
            } catch (abortFailure: Exception) {
                cause.addSuppressed(abortFailure)
            }
        }
    }
}
