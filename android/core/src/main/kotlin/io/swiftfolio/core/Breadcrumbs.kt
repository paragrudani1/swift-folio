package io.swiftfolio.core

/**
 * One segment of the path pill.
 *
 * @property prefix the folder this crumb jumps to (empty for the bucket root).
 * @property isEllipsis true for the collapsed `…` crumb that stands for hidden ancestors.
 */
data class Crumb(
    val label: String,
    val prefix: String,
    val isCurrent: Boolean,
    val isEllipsis: Boolean = false,
)

/** Builds the path pill: `bucket / … / parent / current` for deep paths. */
object Breadcrumbs {
    /** Default number of crumbs shown before collapsing (root, ellipsis, parent, current). */
    const val DEFAULT_MAX_VISIBLE: Int = 4

    private const val ELLIPSIS = "…"
    private const val TAIL_CRUMBS = 2

    /**
     * Crumbs from the bucket root to [prefix]. When there are more than [maxVisible], the
     * middle is replaced by one ellipsis crumb that points at the deepest hidden ancestor.
     */
    fun build(bucket: String, prefix: String, maxVisible: Int = DEFAULT_MAX_VISIBLE): List<Crumb> {
        val ancestors = S3Path.ancestors(prefix)
        val all = ancestors.mapIndexed { index, ancestor ->
            Crumb(
                label = if (index == 0) bucket else S3Path.folderName(ancestor),
                prefix = ancestor,
                isCurrent = index == ancestors.lastIndex,
            )
        }
        if (all.size <= maxVisible || maxVisible < TAIL_CRUMBS + 2) return all
        val tail = all.takeLast(TAIL_CRUMBS)
        val hiddenDeepest = all[all.size - TAIL_CRUMBS - 1]
        val ellipsis = Crumb(label = ELLIPSIS, prefix = hiddenDeepest.prefix, isCurrent = false, isEllipsis = true)
        return listOf(all.first(), ellipsis) + tail
    }
}
