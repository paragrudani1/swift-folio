package io.swiftfolio.core

/**
 * The single place that turns low-level failure signals into a [ConnectError].
 *
 * Classification is table driven: [rules] is evaluated top to bottom and the first match
 * wins. To support a new AWS code, add or extend a row; nothing else changes.
 */
object S3ErrorMapper {
    private const val HTTP_MOVED_PERMANENTLY = 301
    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_SERVER_ERROR = 500

    private val REGION_IN_MESSAGE = Regex("expecting '([a-z0-9-]+)'")
    private val REGION_CODES = setOf("AuthorizationHeaderMalformed", "IllegalLocationConstraintException", "PermanentRedirect")

    private class Rule(val kind: ConnectErrorKind, val matches: (ErrorSignal, String?) -> Boolean)

    private fun codes(vararg values: String): (ErrorSignal, String?) -> Boolean =
        { signal, _ -> signal.errorCode in values || signal.exceptionChain.any { it in values } }

    private fun exceptions(vararg names: String): (ErrorSignal, String?) -> Boolean =
        { signal, _ -> signal.exceptionChain.any { it in names } }

    private val rules: List<Rule> = listOf(
        Rule(ConnectErrorKind.WRONG_REGION) { signal, configured -> isWrongRegion(signal, configured) },
        Rule(ConnectErrorKind.INVALID_ACCESS_KEY, codes("InvalidAccessKeyId")),
        Rule(ConnectErrorKind.SIGNATURE_MISMATCH, codes("SignatureDoesNotMatch")),
        Rule(ConnectErrorKind.EXPIRED_CREDENTIALS, codes("ExpiredToken", "InvalidToken", "TokenRefreshRequired")),
        Rule(ConnectErrorKind.CLOCK_SKEW, codes("RequestTimeTooSkewed")),
        Rule(ConnectErrorKind.NO_SUCH_BUCKET, codes("NoSuchBucket")),
        Rule(ConnectErrorKind.NOT_FOUND) { signal, _ ->
            codes("NoSuchKey", "NotFound")(signal, null) || signal.httpStatus == HTTP_NOT_FOUND
        },
        Rule(ConnectErrorKind.ACCESS_DENIED) { signal, _ ->
            codes("AccessDenied", "AllAccessDisabled")(signal, null) || signal.httpStatus == HTTP_FORBIDDEN
        },
        Rule(ConnectErrorKind.TIMEOUT) { signal, _ ->
            codes("RequestTimeout")(signal, null) ||
                exceptions("SocketTimeoutException", "TimeoutException", "InterruptedIOException")(signal, null)
        },
        Rule(
            ConnectErrorKind.NETWORK,
            exceptions("UnknownHostException", "ConnectException", "NoRouteToHostException", "SocketException", "SSLException"),
        ),
        Rule(ConnectErrorKind.SERVICE_UNAVAILABLE) { signal, _ ->
            codes("SlowDown", "ServiceUnavailable", "InternalError")(signal, null) ||
                (signal.httpStatus ?: 0) >= HTTP_SERVER_ERROR
        },
    )

    /**
     * Classifies [signal].
     *
     * @param configuredRegion region the request was sent to; a server-reported region that
     *   differs from it marks the failure as [ConnectErrorKind.WRONG_REGION].
     */
    fun map(signal: ErrorSignal, configuredRegion: String? = null): ConnectError {
        val kind = rules.firstOrNull { it.matches(signal, configuredRegion) }?.kind ?: ConnectErrorKind.UNKNOWN
        return ConnectError(
            kind = kind,
            code = displayCode(signal),
            detail = signal.message,
            suggestedRegion = if (kind == ConnectErrorKind.WRONG_REGION) suggestedRegion(signal, configuredRegion) else null,
        )
    }

    /** Region S3 says the bucket lives in: the `x-amz-bucket-region` header, else the message. */
    fun suggestedRegion(signal: ErrorSignal, configuredRegion: String? = null): String? {
        val candidate = signal.bucketRegionHeader?.takeIf { it.isNotBlank() }
            ?: signal.message?.let { REGION_IN_MESSAGE.find(it)?.groupValues?.get(1) }
        return candidate?.takeIf { it != configuredRegion }
    }

    private fun isWrongRegion(signal: ErrorSignal, configuredRegion: String?): Boolean {
        val redirectCode = signal.errorCode == "PermanentRedirect" || signal.httpStatus == HTTP_MOVED_PERMANENTLY
        val regionMismatch = suggestedRegion(signal, configuredRegion) != null &&
            (signal.errorCode in REGION_CODES || signal.bucketRegionHeader != null)
        return redirectCode || regionMismatch
    }

    private fun displayCode(signal: ErrorSignal): String =
        signal.errorCode
            ?: signal.exceptionChain.lastOrNull { it.endsWith("Exception") && it != "Exception" }
            ?: signal.httpStatus?.let { "HTTP $it" }
            ?: signal.exceptionChain.firstOrNull()
            ?: "UnknownError"
}
