package io.swiftfolio.core

/**
 * Categories of S3 failures the UI explains differently. Named after the Connect screen,
 * where the categories are first shown, but used for every operation.
 */
enum class ConnectErrorKind {
    WRONG_REGION,
    INVALID_ACCESS_KEY,
    SIGNATURE_MISMATCH,
    EXPIRED_CREDENTIALS,
    CLOCK_SKEW,
    NO_SUCH_BUCKET,
    NOT_FOUND,
    ACCESS_DENIED,
    TIMEOUT,
    NETWORK,
    SERVICE_UNAVAILABLE,
    UNKNOWN;

    /** True when the stored keys themselves are the problem (re-entering them may help). */
    val isCredentialProblem: Boolean
        get() = this == INVALID_ACCESS_KEY || this == SIGNATURE_MISMATCH || this == EXPIRED_CREDENTIALS

    /** True for transient connectivity failures where retrying later makes sense. */
    val isTransient: Boolean
        get() = this == TIMEOUT || this == NETWORK || this == SERVICE_UNAVAILABLE
}

/**
 * A classified S3 failure.
 *
 * @property code machine identifier shown in mono (AWS error code, exception name or `HTTP 403`).
 * @property detail the service message, if any. Never contains credentials.
 * @property suggestedRegion region the bucket actually lives in, when S3 told us
 *   (only set for [ConnectErrorKind.WRONG_REGION]).
 */
data class ConnectError(
    val kind: ConnectErrorKind,
    val code: String,
    val detail: String? = null,
    val suggestedRegion: String? = null,
)

/**
 * Everything the mapper looks at, extracted from an exception by the data layer.
 * Keeping this a plain value lets the mapper stay SDK-agnostic and unit-testable.
 *
 * @property exceptionChain simple class names from the outermost exception to the root cause.
 */
data class ErrorSignal(
    val errorCode: String? = null,
    val httpStatus: Int? = null,
    val message: String? = null,
    val exceptionChain: List<String> = emptyList(),
    val bucketRegionHeader: String? = null,
) {
    companion object {
        private const val MAX_CAUSE_DEPTH = 8

        /** Builds a signal from [throwable] and its causes, plus SDK-specific fields. */
        fun fromThrowable(
            throwable: Throwable,
            errorCode: String? = null,
            httpStatus: Int? = null,
            bucketRegionHeader: String? = null,
        ): ErrorSignal {
            val chain = generateSequence(throwable) { it.cause.takeIf { cause -> cause !== it } }
                .take(MAX_CAUSE_DEPTH)
                .toList()
            return ErrorSignal(
                errorCode = errorCode,
                httpStatus = httpStatus,
                message = chain.firstNotNullOfOrNull { it.message },
                exceptionChain = chain.map { it::class.java.simpleName },
                bucketRegionHeader = bucketRegionHeader,
            )
        }
    }
}
