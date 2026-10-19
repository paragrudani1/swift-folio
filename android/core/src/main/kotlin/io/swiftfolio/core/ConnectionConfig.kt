package io.swiftfolio.core

/**
 * What is needed to talk to one bucket.
 *
 * [toString] redacts the secret and most of the key, so the config can never leak through
 * logs or crash reports.
 */
data class ConnectionConfig(
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucket: String,
    val region: String = CoreConstants.DEFAULT_REGION,
) {
    /** Access key with all but the last four characters masked, e.g. `••••7QX2`. */
    val maskedAccessKeyId: String
        get() = MASK.repeat(MASKED_DOTS) + accessKeyId.takeLast(VISIBLE_KEY_CHARS)

    /** True when every field passes the format validators. */
    val isValid: Boolean
        get() = CredentialValidator.accessKeyId(accessKeyId) == null &&
            CredentialValidator.secretAccessKey(secretAccessKey) == null &&
            BucketNameValidator.validate(bucket) == null &&
            region.isNotBlank()

    override fun toString(): String =
        "ConnectionConfig(accessKeyId=$maskedAccessKeyId, secretAccessKey=<redacted>, bucket=$bucket, region=$region)"

    private companion object {
        const val MASK = "•"
        const val MASKED_DOTS = 4
        const val VISIBLE_KEY_CHARS = 4
    }
}
