package io.swiftfolio.core

/**
 * Format checks for AWS credentials (web-app parity: key >= 16, secret >= 40).
 * Returns null when valid; never throws.
 */
object CredentialValidator {
    private val ACCESS_KEY_CHARS = Regex("^[A-Za-z0-9]+$")

    /** Validates an access key ID: alphanumeric, 16..128 characters. */
    fun accessKeyId(value: String): FieldError? = when {
        value.isBlank() -> FieldError.EMPTY
        !ACCESS_KEY_CHARS.matches(value) -> FieldError.INVALID_CHARACTERS
        value.length < CoreConstants.ACCESS_KEY_MIN_LENGTH -> FieldError.TOO_SHORT
        value.length > CoreConstants.ACCESS_KEY_MAX_LENGTH -> FieldError.TOO_LONG
        else -> null
    }

    /** Validates a secret access key: at least 40 characters, no whitespace. */
    fun secretAccessKey(value: String): FieldError? = when {
        value.isBlank() -> FieldError.EMPTY
        value.any { it.isWhitespace() } -> FieldError.INVALID_CHARACTERS
        value.length < CoreConstants.SECRET_KEY_MIN_LENGTH -> FieldError.TOO_SHORT
        else -> null
    }
}
