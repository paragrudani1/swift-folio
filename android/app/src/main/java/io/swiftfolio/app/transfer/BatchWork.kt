package io.swiftfolio.app.transfer

/** A batch as the work scheduler tracks it: its id and whether its work has ended. */
data class BatchWork(val batchId: String, val isFinished: Boolean)

/**
 * Batch ids whose work ended (succeeded, failed or cancelled), so their files can be deleted.
 * Batches that also appear as pending, and batches the scheduler does not report at all
 * (for example one that is being enqueued right now), are never selected.
 */
fun finishedBatchIds(works: List<BatchWork>): Set<String> {
    val pending = works.filterNot { it.isFinished }.mapTo(mutableSetOf()) { it.batchId }
    return works.filter { it.isFinished && it.batchId !in pending }.mapTo(mutableSetOf()) { it.batchId }
}

/** Tags that link a unit of work to its batch manifest. */
object BatchTags {
    /** Tag shared by every transfer. */
    const val TRANSFER = "swift_folio_transfer"

    private const val BATCH_PREFIX = "batch:"

    /** The per-batch tag of [batchId]. */
    fun forBatch(batchId: String): String = BATCH_PREFIX + batchId

    /** The batch id carried by [tags], or null when none is present. */
    fun batchIdOf(tags: Set<String>): String? =
        tags.firstOrNull { it.startsWith(BATCH_PREFIX) }?.removePrefix(BATCH_PREFIX)?.takeIf { it.isNotEmpty() }
}
