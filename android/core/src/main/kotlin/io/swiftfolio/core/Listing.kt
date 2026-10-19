package io.swiftfolio.core

/**
 * Sort keys. Each entry owns its file comparator, so adding a sort option means adding
 * one entry here (plus a label in the UI table) without touching [Listing].
 */
enum class SortField(internal val fileComparator: Comparator<ObjectEntry.File>) {
    NAME(compareBy<ObjectEntry.File, String>(NaturalOrder) { it.name }),
    MODIFIED(compareBy<ObjectEntry.File, Long?>(nullsFirst(naturalOrder())) { it.lastModifiedEpochMillis }),
    SIZE(compareBy<ObjectEntry.File> { it.sizeBytes }),
}

/** Sort direction. */
enum class SortDirection {
    ASCENDING,
    DESCENDING;

    /** The other direction. */
    fun flipped(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}

/** Field plus direction. The default matches the web app (name, ascending). */
data class SortOrder(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASCENDING,
) {
    /**
     * Web-app column-header behaviour: choosing the active field flips the direction,
     * choosing another field starts ascending.
     */
    fun select(newField: SortField): SortOrder =
        if (newField == field) copy(direction = direction.flipped()) else SortOrder(newField)
}

/** Which kinds of rows a search shows. */
enum class EntryTypeFilter(internal val accepts: (ObjectEntry) -> Boolean) {
    ALL({ true }),
    FILES_ONLY({ it is ObjectEntry.File }),
    FOLDERS_ONLY({ it is ObjectEntry.Folder }),
}

/**
 * Pure listing operations shared by the browser: building rows from a ListObjectsV2
 * page, sorting and filtering. Input lists are never mutated.
 */
object Listing {
    /**
     * Builds rows for [prefix] the way the web app does: only direct, non-blank child
     * folders (never [prefix] itself) and only real files (no folder markers).
     */
    fun fromRaw(prefix: String, commonPrefixes: List<String>, objects: List<RawObject>): List<ObjectEntry> {
        val folders = commonPrefixes
            .filter { isDirectChildFolder(prefix, it) }
            .distinct()
            .map { ObjectEntry.Folder(it) }
        val files = objects
            .filter { !S3Path.isFolderMarker(it.key) && it.key != prefix && S3Path.fileName(it.key).isNotBlank() }
            .map { ObjectEntry.File(it.key, it.sizeBytes.coerceAtLeast(0), it.lastModifiedEpochMillis) }
        return folders + files
    }

    /**
     * Folders always come first and are ordered by name (reversed only for a descending
     * name sort, because folders have no size or date). Files use [order].
     */
    fun sort(entries: List<ObjectEntry>, order: SortOrder): List<ObjectEntry> {
        val folderComparator = compareBy<ObjectEntry.Folder, String>(NaturalOrder) { it.name }
        val folders = entries.filterIsInstance<ObjectEntry.Folder>().sortedWith(
            if (order.field == SortField.NAME) folderComparator.directed(order.direction) else folderComparator,
        )
        val fileComparator = order.field.fileComparator.thenBy(NaturalOrder) { it.name }
        val files = entries.filterIsInstance<ObjectEntry.File>().sortedWith(fileComparator.directed(order.direction))
        return folders + files
    }

    /** Case-insensitive substring match on the display name, combined with [typeFilter]. */
    fun filter(entries: List<ObjectEntry>, query: String, typeFilter: EntryTypeFilter = EntryTypeFilter.ALL): List<ObjectEntry> {
        val needle = query.trim()
        return entries.filter { entry ->
            typeFilter.accepts(entry) && (needle.isEmpty() || entry.name.contains(needle, ignoreCase = true))
        }
    }

    /** Filter then sort: what the browser renders. */
    fun visible(entries: List<ObjectEntry>, query: String, order: SortOrder, typeFilter: EntryTypeFilter): List<ObjectEntry> =
        sort(filter(entries, query, typeFilter), order)

    /** Ranges of [name] matching [query] (case-insensitive), for search highlighting. */
    fun matchRanges(name: String, query: String): List<IntRange> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var start = name.indexOf(needle, ignoreCase = true)
        while (start >= 0) {
            ranges += start until start + needle.length
            start = name.indexOf(needle, startIndex = start + needle.length, ignoreCase = true)
        }
        return ranges
    }

    private fun isDirectChildFolder(prefix: String, candidate: String): Boolean {
        if (candidate == prefix || !candidate.startsWith(prefix)) return false
        val name = candidate.removePrefix(prefix).removeSuffix(CoreConstants.DELIMITER)
        return name.isNotBlank() && !name.contains(CoreConstants.DELIMITER)
    }

    private fun <T> Comparator<T>.directed(direction: SortDirection): Comparator<T> =
        if (direction == SortDirection.ASCENDING) this else reversed()
}
