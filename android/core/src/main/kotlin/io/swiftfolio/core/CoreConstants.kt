package io.swiftfolio.core

/**
 * Numeric and textual constants shared by the pure-Kotlin domain layer.
 *
 * Every limit here mirrors an AWS S3 rule or a web-app behaviour so that the Android
 * and web clients agree on validation and formatting.
 */
object CoreConstants {
    /** Delimiter used to model folders on top of flat S3 keys. */
    const val DELIMITER: String = "/"

    /** Scheme prefix of an S3 URI, as in `s3://bucket/key`. */
    const val S3_URI_SCHEME: String = "s3://"

    /** Default region when the user has not picked one (matches the web app). */
    const val DEFAULT_REGION: String = "us-east-1"

    /** Minimum length of an AWS access key ID accepted by the web app. */
    const val ACCESS_KEY_MIN_LENGTH: Int = 16

    /** Maximum length of an AWS access key ID (IAM limit). */
    const val ACCESS_KEY_MAX_LENGTH: Int = 128

    /** Minimum length of an AWS secret access key accepted by the web app. */
    const val SECRET_KEY_MIN_LENGTH: Int = 40

    /** Bucket name length bounds (S3 general purpose bucket rules). */
    const val BUCKET_MIN_LENGTH: Int = 3
    const val BUCKET_MAX_LENGTH: Int = 63

    /** Maximum size of an S3 object key, in UTF-8 bytes. */
    const val MAX_KEY_BYTES: Int = 1024

    /** Maximum number of keys accepted by one DeleteObjects request. */
    const val DELETE_BATCH_SIZE: Int = 1000

    /** Maximum number of keys returned by one ListObjectsV2 page. */
    const val LIST_PAGE_SIZE: Int = 1000

    /** Multipart part size and single-PUT threshold: 5 MiB (the S3 minimum part size). */
    const val MULTIPART_PART_SIZE: Long = 5L * 1024 * 1024

    /** S3 allows at most this many parts per multipart upload. */
    const val MAX_MULTIPART_PARTS: Int = 10_000

    /** Base used for human-readable byte sizes (the web app uses binary units). */
    const val BYTES_PER_UNIT: Double = 1024.0
}
