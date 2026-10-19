package io.swiftfolio.app.transfer

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads and writes batch [TransferManifest]s and the per-file outcomes of their runs, as files
 * in a private directory with an in-memory cache.
 *
 * Outcomes are an append-only JSON-lines log (one line per finished file), so recording a file
 * costs one short write whatever the batch size, and a worker that is re-run after process
 * death can skip the files that already finished. Unreadable files or lines are logged by type
 * and treated as missing. Safe to call from any thread; every method does blocking file I/O.
 */
class TransferManifestStore(private val directory: File) : OutcomeLog {
    private val manifests = ConcurrentHashMap<String, TransferManifest>()
    private val outcomes = ConcurrentHashMap<String, MutableMap<Int, ItemState>>()
    private val json = Json { ignoreUnknownKeys = true }

    /** Persists [manifest]. Throws [IOException] if the file cannot be written. */
    fun save(manifest: TransferManifest) {
        directory.mkdirs()
        manifestFile(manifest.batchId).writeText(json.encodeToString(TransferManifest.serializer(), manifest))
        manifests[manifest.batchId] = manifest
    }

    /** The manifest of [batchId], or null when unknown or unreadable. */
    fun load(batchId: String): TransferManifest? {
        manifests[batchId]?.let { return it }
        val file = manifestFile(batchId)
        if (!file.exists()) return null
        return try {
            json.decodeFromString(TransferManifest.serializer(), file.readText()).also { manifests[batchId] = it }
        } catch (error: IOException) {
            reportUnreadable(error)
        } catch (error: SerializationException) {
            reportUnreadable(error)
        }
    }

    override fun outcomes(batchId: String): Map<Int, ItemState> = outcomesOf(batchId).toMap()

    override fun record(batchId: String, index: Int, state: ItemState): Boolean {
        val line = json.encodeToString(OutcomeLine.serializer(), OutcomeLine(index, state))
        synchronized(this) {
            outcomesOf(batchId)[index] = state
            return try {
                directory.mkdirs()
                outcomeFile(batchId).appendText(line + "\n")
                true
            } catch (error: IOException) {
                Log.w(TAG, "Outcome not persisted: ${error::class.java.simpleName}")
                false
            }
        }
    }

    /** Deletes the manifests and outcome logs of [batchIds]. Missing files are ignored. */
    fun delete(batchIds: Collection<String>) {
        batchIds.forEach { batchId ->
            manifests.remove(batchId)
            outcomes.remove(batchId)
            listOf(manifestFile(batchId), outcomeFile(batchId)).forEach { file ->
                if (file.exists() && !file.delete()) Log.w(TAG, "Could not delete a finished batch file")
            }
        }
    }

    private fun outcomesOf(batchId: String): MutableMap<Int, ItemState> =
        outcomes.computeIfAbsent(batchId) { ConcurrentHashMap(readOutcomes(batchId)) }

    /** Replays the log; later lines win, and a torn last line (process death mid-write) is skipped. */
    private fun readOutcomes(batchId: String): Map<Int, ItemState> {
        val file = outcomeFile(batchId)
        if (!file.exists()) return emptyMap()
        val lines = try {
            file.readLines()
        } catch (error: IOException) {
            reportUnreadable(error)
            return emptyMap()
        }
        return lines.filter { it.isNotBlank() }.mapNotNull(::parseLine).associate { it.index to it.state }
    }

    private fun parseLine(line: String): OutcomeLine? = try {
        json.decodeFromString(OutcomeLine.serializer(), line)
    } catch (error: SerializationException) {
        reportUnreadable(error)
    } catch (error: IllegalArgumentException) {
        reportUnreadable(error)
    }

    private fun reportUnreadable(error: Exception): Nothing? {
        Log.w(TAG, "Transfer file unreadable: ${error::class.java.simpleName}")
        return null
    }

    private fun manifestFile(batchId: String) = File(directory, "$batchId$MANIFEST_EXTENSION")

    private fun outcomeFile(batchId: String) = File(directory, "$batchId$OUTCOME_EXTENSION")

    /** One line of the outcome log. */
    @Serializable
    private data class OutcomeLine(val index: Int, val state: ItemState)

    private companion object {
        const val TAG = "TransferManifests"
        const val MANIFEST_EXTENSION = ".json"
        const val OUTCOME_EXTENSION = ".outcomes.jsonl"
    }
}

/** Where a [BatchRunner] keeps the result of each finished file, so a re-run can resume. */
interface OutcomeLog {
    /** Recorded states of [batchId]'s finished files, by item index. Empty when none or unreadable. */
    fun outcomes(batchId: String): Map<Int, ItemState>

    /**
     * Records the final [state] of item [index] of [batchId]. Never throws; returns false (and
     * logs the failure type) when it could not be persisted, in which case a re-run repeats the file.
     */
    fun record(batchId: String, index: Int, state: ItemState): Boolean
}
