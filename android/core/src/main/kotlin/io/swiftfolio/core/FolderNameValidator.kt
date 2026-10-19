package io.swiftfolio.core

/**
 * Rules for the "New folder" input (web-app parity: non-empty, no slash, unique),
 * plus the S3 key length limit and a ban on `.`/`..` segments.
 */
object FolderNameValidator {
    /**
     * Validates [name] for creation inside [parentPrefix].
     *
     * @param existingFolderNames names of folders already listed in [parentPrefix];
     *   comparison is case-sensitive because S3 keys are.
     * @return null when the trimmed name can be created.
     */
    fun validate(name: String, parentPrefix: String, existingFolderNames: Collection<String>): FieldError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMPTY
            trimmed.contains(CoreConstants.DELIMITER) -> FieldError.CONTAINS_SLASH
            trimmed == "." || trimmed == ".." -> FieldError.DOT_SEGMENT
            trimmed in existingFolderNames -> FieldError.ALREADY_EXISTS
            keyBytes(parentPrefix, trimmed) > CoreConstants.MAX_KEY_BYTES -> FieldError.KEY_TOO_LONG
            else -> null
        }
    }

    /** True when [name] has leading or trailing whitespace (a warning, not an error). */
    fun hasOuterWhitespace(name: String): Boolean = name.isNotEmpty() && name != name.trim()

    private fun keyBytes(parentPrefix: String, name: String): Int =
        S3Path.childPrefix(parentPrefix, name).toByteArray(Charsets.UTF_8).size
}
