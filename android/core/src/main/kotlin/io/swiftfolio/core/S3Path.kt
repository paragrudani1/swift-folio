package io.swiftfolio.core

/**
 * Helpers for S3 "paths".
 *
 * Naming convention used across the app:
 * - a **prefix** is a folder path. It is either empty (bucket root) or ends with `/`.
 * - a **key** is a full object key and never ends with `/` (folder markers are prefixes).
 *
 * All functions are pure and never throw.
 */
object S3Path {
    private const val SEPARATOR = CoreConstants.DELIMITER

    /**
     * Normalises user or server input into a prefix: removes leading slashes, collapses
     * repeated slashes and guarantees a trailing slash unless the result is the root.
     */
    fun normalizePrefix(raw: String): String {
        val parts = raw.split(SEPARATOR).filter { it.isNotEmpty() }
        return if (parts.isEmpty()) "" else parts.joinToString(SEPARATOR, postfix = SEPARATOR)
    }

    /** Returns the folder names of [prefix], for example `a/b/` -> `[a, b]`. */
    fun segments(prefix: String): List<String> =
        prefix.split(SEPARATOR).filter { it.isNotEmpty() }

    /** Number of folder levels below the bucket root (`""` is 0, `a/b/` is 2). */
    fun depth(prefix: String): Int = segments(prefix).size

    /** Parent prefix of [prefix]; the root is its own parent. */
    fun parentOf(prefix: String): String {
        val parts = segments(prefix)
        return if (parts.size <= 1) "" else parts.dropLast(1).joinToString(SEPARATOR, postfix = SEPARATOR)
    }

    /** Every ancestor of [prefix] from the root to [prefix] itself, e.g. `["", "a/", "a/b/"]`. */
    fun ancestors(prefix: String): List<String> {
        val parts = segments(prefix)
        return (0..parts.size).map { count ->
            if (count == 0) "" else parts.take(count).joinToString(SEPARATOR, postfix = SEPARATOR)
        }
    }

    /** Prefix of the child folder [name] inside [parentPrefix]. */
    fun childPrefix(parentPrefix: String, name: String): String =
        normalizePrefix(parentPrefix) + name.trim(SEPARATOR[0]) + SEPARATOR

    /** Object key of a file called [fileName] inside [prefix]. */
    fun join(prefix: String, fileName: String): String =
        normalizePrefix(prefix) + fileName.trimStart(SEPARATOR[0])

    /** Display name of a folder prefix (`a/b/` -> `b`); empty for the root. */
    fun folderName(prefix: String): String = segments(prefix).lastOrNull().orEmpty()

    /** Display name of an object key (`a/b/c.txt` -> `c.txt`). */
    fun fileName(key: String): String = key.substringAfterLast(SEPARATOR)

    /** Lower-case extension of [name] without the dot, or null when there is none. */
    fun extension(name: String): String? {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return null
        return name.substring(dot + 1).lowercase()
    }

    /** `s3://bucket/keyOrPrefix` URI used by the copy actions. */
    fun s3Uri(bucket: String, keyOrPrefix: String): String =
        CoreConstants.S3_URI_SCHEME + bucket + SEPARATOR + keyOrPrefix

    /** True when [key] is a folder marker (ends with the delimiter). */
    fun isFolderMarker(key: String): Boolean = key.endsWith(SEPARATOR)
}
