package io.swiftfolio.app.bucket

import io.swiftfolio.app.session.ActiveRepository
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.S3Result
import java.util.concurrent.ConcurrentHashMap

/**
 * Folder listings of the active bucket with a per-prefix cache, so revisiting a folder shows
 * its last rows at once while it is revalidated.
 */
interface FolderListings {
    /** The last successful listing of [prefix], or null when it was never loaded or was invalidated. */
    fun cached(prefix: String): List<ObjectEntry>?

    /** Stores [entries] as the listing of [prefix] (the root listing fetched while connecting). */
    fun seed(prefix: String, entries: List<ObjectEntry>)

    /**
     * Fetches the direct children of [prefix] and caches them on success. A failure leaves the
     * cache as it was. Never throws except for coroutine cancellation.
     */
    suspend fun load(prefix: String): S3Result<List<ObjectEntry>>

    /** Size of file [key] as last listed in its folder, or null when unknown. */
    fun sizeOf(key: String): Long?

    /** Drops the cached listing of [prefix]. */
    fun invalidate(prefix: String)

    /** Drops every cached listing. */
    fun invalidateAll()
}

/** [FolderListings] over the repository of the active session. Thread-safe. */
class CachedFolderListings(private val activeRepository: ActiveRepository) : FolderListings {
    private val cache = ConcurrentHashMap<String, List<ObjectEntry>>()

    override fun cached(prefix: String): List<ObjectEntry>? = cache[prefix]

    override fun seed(prefix: String, entries: List<ObjectEntry>) {
        cache[prefix] = entries
    }

    override suspend fun load(prefix: String): S3Result<List<ObjectEntry>> {
        val repository = activeRepository.current() ?: return NoSessionFailure
        return repository.list(prefix).also { result -> if (result is S3Result.Success) cache[prefix] = result.value }
    }

    override fun sizeOf(key: String): Long? {
        val parent = S3Path.parentOf(key)
        return cache[parent]?.firstNotNullOfOrNull { entry -> (entry as? ObjectEntry.File)?.takeIf { it.key == key }?.sizeBytes }
    }

    override fun invalidate(prefix: String) {
        cache.remove(prefix)
    }

    override fun invalidateAll() {
        cache.clear()
    }
}
