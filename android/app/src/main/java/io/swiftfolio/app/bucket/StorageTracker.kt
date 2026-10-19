package io.swiftfolio.app.bucket

import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.session.ActiveRepository
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Storage usage chip. */
sealed interface StorageState {
    /** Computing; the chip shimmers. */
    data object Computing : StorageState

    /** Total known (exact after a scan, then kept up to date by local adjustments). */
    data class Ready(val usage: StorageUsage) : StorageState

    /** Could not be computed (for example missing permission). */
    data class Unavailable(val error: ConnectError) : StorageState
}

/**
 * The bucket's storage total. Scanning lists every object, so it only runs when asked
 * ([rescan]: on connect and on explicit refresh); changes made from this app adjust the
 * known total locally instead.
 */
interface StorageTracker {
    /** Current total; starts as [StorageState.Computing]. */
    val state: StateFlow<StorageState>

    /** Lists the whole bucket and publishes the exact total. Cancellable; never throws otherwise. */
    suspend fun rescan()

    /**
     * Adds [bytes] and [objects] (negative to subtract) to a known total, clamped at zero.
     * Ignored while computing or unavailable, because the next scan result replaces it anyway.
     */
    fun adjust(bytes: Long, objects: Long)
}

/** [StorageTracker] over the repository of the active session. */
class BucketStorageTracker(private val activeRepository: ActiveRepository) : StorageTracker {
    private val mutableState = MutableStateFlow<StorageState>(StorageState.Computing)

    override val state: StateFlow<StorageState> = mutableState.asStateFlow()

    override suspend fun rescan() {
        mutableState.value = StorageState.Computing
        val result = activeRepository.current()?.computeStorageUsage() ?: NoSessionFailure
        mutableState.value = when (result) {
            is S3Result.Success -> StorageState.Ready(result.value)
            is S3Result.Failure -> StorageState.Unavailable(result.error)
        }
    }

    override fun adjust(bytes: Long, objects: Long) {
        mutableState.update { current ->
            if (current !is StorageState.Ready) return@update current
            val usage = current.usage
            StorageState.Ready(
                StorageUsage(
                    totalBytes = (usage.totalBytes + bytes).coerceAtLeast(0),
                    objectCount = (usage.objectCount + objects).coerceAtLeast(0),
                ),
            )
        }
    }
}
