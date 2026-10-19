package io.swiftfolio.core

/** A byte range of a multipart upload. [partNumber] starts at 1, as S3 requires. */
data class PartRange(val partNumber: Int, val offset: Long, val length: Long)

/**
 * Decides how a file is sent to S3 and under which key.
 *
 * Files up to [CoreConstants.MULTIPART_PART_SIZE] use one PutObject; larger files use a
 * multipart upload whose part size grows when needed to stay within
 * [CoreConstants.MAX_MULTIPART_PARTS] parts.
 */
object UploadPlanner {
    private const val MEBIBYTE = 1024L * 1024L
    private const val FALLBACK_FILE_NAME = "upload"

    /** True when a file of [sizeBytes] is uploaded with a single PutObject. */
    fun isSinglePut(sizeBytes: Long): Boolean = sizeBytes <= CoreConstants.MULTIPART_PART_SIZE

    /**
     * Part size for a file of [sizeBytes] (null when the size is unknown): at least 5 MiB,
     * rounded up to whole MiB, and large enough for the part-count limit.
     */
    fun partSize(sizeBytes: Long?): Long {
        val minimum = CoreConstants.MULTIPART_PART_SIZE
        if (sizeBytes == null || sizeBytes <= minimum * CoreConstants.MAX_MULTIPART_PARTS) return minimum
        val needed = ceilDiv(sizeBytes, CoreConstants.MAX_MULTIPART_PARTS.toLong())
        return ceilDiv(needed, MEBIBYTE) * MEBIBYTE
    }

    /** Every part of a [sizeBytes] upload using [partSize]; empty for a zero-byte file. */
    fun parts(sizeBytes: Long, partSize: Long = partSize(sizeBytes)): List<PartRange> {
        require(partSize > 0) { "partSize must be positive" }
        if (sizeBytes <= 0) return emptyList()
        val count = ceilDiv(sizeBytes, partSize).toInt()
        return (0 until count).map { index ->
            val offset = index * partSize
            PartRange(partNumber = index + 1, offset = offset, length = minOf(partSize, sizeBytes - offset))
        }
    }

    /**
     * Object key for a picked file called [displayName] uploaded into [prefix]. Slashes in
     * the name are replaced so a file never creates folders by accident.
     */
    fun objectKey(prefix: String, displayName: String): String {
        val safeName = displayName.replace(CoreConstants.DELIMITER, "_").trim().ifEmpty { FALLBACK_FILE_NAME }
        return S3Path.join(prefix, safeName)
    }

    /**
     * Returns [key], or `name (n).ext` with the smallest n >= 1 that is not in [existingKeys]
     * (the "Keep both" conflict choice).
     */
    fun deduplicate(key: String, existingKeys: Set<String>): String {
        if (key !in existingKeys) return key
        val folder = key.substringBeforeLast(CoreConstants.DELIMITER, missingDelimiterValue = "")
        val name = S3Path.fileName(key)
        val extension = S3Path.extension(name)
        val stem = if (extension == null) name else name.dropLast(extension.length + 1)
        val suffix = extension?.let { ".$it" }.orEmpty()
        val base = if (folder.isEmpty()) "" else folder + CoreConstants.DELIMITER
        return generateSequence(1) { it + 1 }
            .map { "$base$stem ($it)$suffix" }
            .first { it !in existingKeys }
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
}
