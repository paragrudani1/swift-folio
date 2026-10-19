package io.swiftfolio.core

/**
 * S3 general purpose bucket naming rules. Each rule is a row in [rules]; the first
 * failing rule decides the error, so adding a rule never edits existing branches.
 */
object BucketNameValidator {
    private val ALLOWED = Regex("^[a-z0-9.-]+$")
    private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val RESERVED_PREFIXES = listOf("xn--", "sthree-", "amzn-s3-demo-")
    private val RESERVED_SUFFIXES = listOf("-s3alias", "--ol-s3", ".mrap", "--x-s3")

    private val rules: List<Pair<FieldError, (String) -> Boolean>> = listOf(
        FieldError.EMPTY to { name -> name.isEmpty() },
        FieldError.TOO_SHORT to { name -> name.length < CoreConstants.BUCKET_MIN_LENGTH },
        FieldError.TOO_LONG to { name -> name.length > CoreConstants.BUCKET_MAX_LENGTH },
        FieldError.UPPERCASE to { name -> name.any { it.isUpperCase() } },
        FieldError.INVALID_CHARACTERS to { name -> !ALLOWED.matches(name) },
        FieldError.BAD_START_OR_END to { name -> !name.first().isLetterOrDigit() || !name.last().isLetterOrDigit() },
        FieldError.CONSECUTIVE_DOTS to { name -> name.contains("..") },
        FieldError.IP_ADDRESS_FORMAT to { name -> IPV4.matches(name) },
        FieldError.RESERVED_NAME to { name ->
            RESERVED_PREFIXES.any { name.startsWith(it) } || RESERVED_SUFFIXES.any { name.endsWith(it) }
        },
    )

    /** Returns the first broken rule for [name], or null when the name is valid. */
    fun validate(name: String): FieldError? = rules.firstOrNull { (_, broken) -> broken(name) }?.first
}
