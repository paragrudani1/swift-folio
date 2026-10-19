package io.swiftfolio.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.swiftfolio.app.R
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.CoreConstants
import io.swiftfolio.core.FieldError

/**
 * The UI half of error handling: one table from [ConnectErrorKind] to a title and a hint,
 * and one from [FieldError] to a message. Classification itself lives in core's S3ErrorMapper.
 */
object ErrorTexts {
    private data class KindText(@StringRes val title: Int, @StringRes val hint: Int)

    private val kindTexts: Map<ConnectErrorKind, KindText> = mapOf(
        ConnectErrorKind.WRONG_REGION to KindText(R.string.error_wrong_region_title, R.string.error_wrong_region_hint),
        ConnectErrorKind.INVALID_ACCESS_KEY to KindText(R.string.error_bad_keys_title, R.string.error_invalid_key_hint),
        ConnectErrorKind.SIGNATURE_MISMATCH to KindText(R.string.error_bad_keys_title, R.string.error_signature_hint),
        ConnectErrorKind.EXPIRED_CREDENTIALS to KindText(R.string.error_expired_title, R.string.error_expired_hint),
        ConnectErrorKind.CLOCK_SKEW to KindText(R.string.error_clock_title, R.string.error_clock_hint),
        ConnectErrorKind.NO_SUCH_BUCKET to KindText(R.string.error_no_bucket_title, R.string.error_no_bucket_hint),
        ConnectErrorKind.NOT_FOUND to KindText(R.string.error_not_found_title, R.string.error_not_found_hint),
        ConnectErrorKind.ACCESS_DENIED to KindText(R.string.error_access_denied_title, R.string.error_access_denied_hint),
        ConnectErrorKind.TIMEOUT to KindText(R.string.error_timeout_title, R.string.error_network_hint),
        ConnectErrorKind.NETWORK to KindText(R.string.error_network_title, R.string.error_network_hint),
        ConnectErrorKind.SERVICE_UNAVAILABLE to KindText(R.string.error_service_title, R.string.error_service_hint),
        ConnectErrorKind.UNKNOWN to KindText(R.string.error_unknown_title, R.string.error_unknown_hint),
    )

    private val fieldMessages: Map<FieldError, Int> = mapOf(
        FieldError.EMPTY to R.string.field_error_empty,
        FieldError.TOO_SHORT to R.string.field_error_too_short,
        FieldError.TOO_LONG to R.string.field_error_too_long,
        FieldError.INVALID_CHARACTERS to R.string.field_error_invalid_chars,
        FieldError.UPPERCASE to R.string.field_error_uppercase,
        FieldError.BAD_START_OR_END to R.string.field_error_bad_edges,
        FieldError.CONSECUTIVE_DOTS to R.string.field_error_double_dots,
        FieldError.IP_ADDRESS_FORMAT to R.string.field_error_ip,
        FieldError.RESERVED_NAME to R.string.field_error_reserved,
        FieldError.CONTAINS_SLASH to R.string.field_error_slash,
        FieldError.DOT_SEGMENT to R.string.field_error_dot_segment,
        FieldError.ALREADY_EXISTS to R.string.field_error_exists,
        FieldError.KEY_TOO_LONG to R.string.field_error_key_too_long,
    )

    /** Field whose length limits are quoted in TOO_SHORT / TOO_LONG messages. */
    enum class LengthLimits(val min: Int, val max: Int) {
        ACCESS_KEY(CoreConstants.ACCESS_KEY_MIN_LENGTH, CoreConstants.ACCESS_KEY_MAX_LENGTH),
        SECRET(CoreConstants.SECRET_KEY_MIN_LENGTH, Int.MAX_VALUE),
        BUCKET(CoreConstants.BUCKET_MIN_LENGTH, CoreConstants.BUCKET_MAX_LENGTH),
        FOLDER(1, CoreConstants.MAX_KEY_BYTES),
    }

    /** Short sans title for [error]. */
    @Composable
    fun title(error: ConnectError): String = stringResource(kindTexts.getValue(error.kind).title)

    /** One-line fix hint for [error]; mentions the suggested region when known. */
    @Composable
    fun hint(error: ConnectError): String = stringResource(kindTexts.getValue(error.kind).hint)

    /** Message for a field validation error. */
    @Composable
    fun field(error: FieldError, limits: LengthLimits): String = when (error) {
        FieldError.TOO_SHORT -> stringResource(R.string.field_error_too_short, limits.min)
        FieldError.TOO_LONG -> stringResource(R.string.field_error_too_long, limits.max)
        else -> stringResource(fieldMessages.getValue(error))
    }
}
