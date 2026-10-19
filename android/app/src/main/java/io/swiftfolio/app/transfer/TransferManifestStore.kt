package io.swiftfolio.app.transfer

import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads and writes [TransferManifest]s as JSON files in a private directory, with an
 * in-memory cache. Unreadable manifests are reported (logged by type) and treated as missing.
 */
class TransferManifestStore(private val directory: File) {
    private val cache = ConcurrentHashMap<String, TransferManifest>()
    private val json = Json { ignoreUnknownKeys = true }

    /** Persists [manifest]. Throws [IOException] if the file cannot be written. */
    fun save(manifest: TransferManifest) {
        directory.mkdirs()
        fileFor(manifest.batchId).writeText(json.encodeToString(TransferManifest.serializer(), manifest))
        cache[manifest.batchId] = manifest
    }

    /** The manifest of [batchId], or null when unknown or unreadable. */
    fun load(batchId: String): TransferManifest? {
        cache[batchId]?.let { return it }
        val file = fileFor(batchId)
        if (!file.exists()) return null
        return try {
            json.decodeFromString(TransferManifest.serializer(), file.readText()).also { cache[batchId] = it }
        } catch (error: IOException) {
            reportUnreadable(batchId, error)
        } catch (error: SerializationException) {
            reportUnreadable(batchId, error)
        }
    }

    /** Deletes every manifest whose id is not in [keepIds]. */
    fun retainOnly(keepIds: Set<String>) {
        directory.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension
            if (id !in keepIds) {
                cache.remove(id)
                if (!file.delete()) Log.w(TAG, "Could not delete stale manifest")
            }
        }
    }

    private fun reportUnreadable(batchId: String, error: Exception): TransferManifest? {
        Log.w(TAG, "Manifest $batchId unreadable: ${error::class.java.simpleName}")
        return null
    }

    private fun fileFor(batchId: String) = File(directory, "$batchId$EXTENSION")

    private companion object {
        const val TAG = "TransferManifests"
        const val EXTENSION = ".json"
    }
}
