package io.swiftfolio.app.bucket

import io.swiftfolio.app.data.DeleteProgress
import io.swiftfolio.app.data.DeleteSummary
import io.swiftfolio.app.session.ActiveRepository
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.S3Result

/**
 * Changes to the bucket made from the browser. Each one keeps [FolderListings] and the
 * [StorageTracker] consistent, so the screen never has to rescan the whole bucket.
 */
interface BucketMutations {
    /**
     * Creates the folder [name] (already validated) inside [parentPrefix]. On success the
     * parent's cached listing is dropped and one object is added to the storage total.
     */
    suspend fun createFolder(parentPrefix: String, name: String): S3Result<Unit>

    /**
     * Deletes [ids] (keys as-is, prefixes recursively), reporting [onProgress]. When it finishes
     * (fully or partly) every cached listing is dropped and the freed bytes and objects are
     * subtracted from the storage total. Per-key failures are in [DeleteSummary.failedKeys].
     */
    suspend fun delete(ids: Collection<String>, onProgress: (DeleteProgress) -> Unit): S3Result<DeleteSummary>

    /** Records that [objects] files totalling [bytes] were uploaded into [prefix]. */
    fun recordUpload(prefix: String, bytes: Long, objects: Int)
}

/** [BucketMutations] over the repository of the active session. */
class RepositoryBucketMutations(
    private val activeRepository: ActiveRepository,
    private val listings: FolderListings,
    private val storage: StorageTracker,
) : BucketMutations {

    override suspend fun createFolder(parentPrefix: String, name: String): S3Result<Unit> {
        val repository = activeRepository.current() ?: return NoSessionFailure
        return repository.createFolder(S3Path.childPrefix(parentPrefix, name)).also { result ->
            if (result is S3Result.Success) {
                listings.invalidate(parentPrefix)
                storage.adjust(bytes = 0, objects = 1)
            }
        }
    }

    override suspend fun delete(ids: Collection<String>, onProgress: (DeleteProgress) -> Unit): S3Result<DeleteSummary> {
        val repository = activeRepository.current() ?: return NoSessionFailure
        // Directly deleted files are sized from the listing; folder contents by the repository.
        val directSizes = ids.filterNot(S3Path::isFolderMarker).associateWith { listings.sizeOf(it) ?: 0L }
        val result = repository.delete(ids, onProgress)
        if (result is S3Result.Success) {
            val summary = result.value
            val failed = summary.failedKeys.toSet()
            val freedBytes = directSizes.filterKeys { it !in failed }.values.sum() + summary.freedBytes
            storage.adjust(bytes = -freedBytes, objects = -summary.deleted.toLong())
        }
        listings.invalidateAll()
        return result
    }

    override fun recordUpload(prefix: String, bytes: Long, objects: Int) {
        listings.invalidate(prefix)
        storage.adjust(bytes = bytes, objects = objects.toLong())
    }
}
