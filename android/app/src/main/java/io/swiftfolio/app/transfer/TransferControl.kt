package io.swiftfolio.app.transfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Identifies one file of a batch. */
data class ItemRef(val batchId: String, val itemIndex: Int)

/**
 * In-process channel for per-file cancellation between the UI and running workers.
 * Whole-batch cancellation goes through WorkManager instead. Owned by the AppContainer.
 */
class TransferControl {
    private val cancelled = MutableStateFlow<Set<ItemRef>>(emptySet())

    /** Every file the user cancelled in this process. */
    val cancelledItems: StateFlow<Set<ItemRef>> = cancelled.asStateFlow()

    /** Marks [ref] as cancelled. */
    fun cancel(ref: ItemRef) {
        cancelled.update { it + ref }
    }

    /** True when [ref] was cancelled. */
    fun isCancelled(ref: ItemRef): Boolean = ref in cancelled.value

    /** Drops the flags of a finished batch. */
    fun forget(batchId: String) {
        cancelled.update { refs -> refs.filterNot { it.batchId == batchId }.toSet() }
    }
}
