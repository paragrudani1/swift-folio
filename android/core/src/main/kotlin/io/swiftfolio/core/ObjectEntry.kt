package io.swiftfolio.core

/**
 * One row of a folder listing.
 *
 * [id] is the S3 identity of the row: the prefix for folders and the key for files.
 * It is unique within a listing and is what selection and deletion operate on.
 */
sealed interface ObjectEntry {
    /** Prefix (folders) or key (files). */
    val id: String

    /** Display name: last path segment without the trailing slash. */
    val name: String

    /** A folder, modelled by an S3 common prefix ending in `/`. */
    data class Folder(val prefix: String) : ObjectEntry {
        override val id: String get() = prefix
        override val name: String get() = S3Path.folderName(prefix)
    }

    /**
     * A file (object).
     *
     * @property sizeBytes object size; never negative.
     * @property lastModifiedEpochMillis last modification time, or null if S3 did not send it.
     */
    data class File(
        val key: String,
        val sizeBytes: Long,
        val lastModifiedEpochMillis: Long?,
    ) : ObjectEntry {
        override val id: String get() = key
        override val name: String get() = S3Path.fileName(key)

        /** File family derived from the extension. */
        val kind: FileKind get() = FileKind.of(key)
    }
}

/** Raw object as returned by ListObjectsV2, before filtering. */
data class RawObject(
    val key: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long?,
)
