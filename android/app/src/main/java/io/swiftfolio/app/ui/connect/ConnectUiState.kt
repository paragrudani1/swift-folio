package io.swiftfolio.app.ui.connect

import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.core.BucketNameValidator
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.CoreConstants
import io.swiftfolio.core.CredentialValidator
import io.swiftfolio.core.FieldError

/** Inputs of the Connect form that show validation errors. */
enum class ConnectField { ACCESS_KEY, SECRET, BUCKET, REGION }

/** One line of the connect checklist. */
data class StepRow(val step: ConnectStep, val status: StepStatus)

/**
 * Everything the Connect screen renders. Field errors appear only after the field lost
 * focus once (or after a submit attempt), never while the user is still typing.
 */
data class ConnectUiState(
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val bucket: String = "",
    val region: String = CoreConstants.DEFAULT_REGION,
    val remember: Boolean = true,
    val secretVisible: Boolean = false,
    val touchedFields: Set<ConnectField> = emptySet(),
    val isConnecting: Boolean = false,
    val steps: List<StepRow> = emptyList(),
    val error: ConnectError? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    /** The config that Connect would use (inputs trimmed). */
    val config: ConnectionConfig
        get() = ConnectionConfig(accessKeyId.trim(), secretAccessKey.trim(), bucket.trim(), region)

    /** Access key error, shown only once the field was touched. */
    val accessKeyError: FieldError?
        get() = CredentialValidator.accessKeyId(accessKeyId.trim()).takeIf { ConnectField.ACCESS_KEY in touchedFields }

    /** Secret error, shown only once the field was touched. */
    val secretError: FieldError?
        get() = CredentialValidator.secretAccessKey(secretAccessKey.trim()).takeIf { ConnectField.SECRET in touchedFields }

    /** Bucket error, shown only once the field was touched. */
    val bucketError: FieldError?
        get() = BucketNameValidator.validate(bucket.trim()).takeIf { ConnectField.BUCKET in touchedFields }

    /** True when the access key is well formed (shows the green tick). */
    val accessKeyLooksValid: Boolean get() = CredentialValidator.accessKeyId(accessKeyId.trim()) == null

    /** True when the form can be submitted. */
    val canSubmit: Boolean get() = !isConnecting && config.isValid

    /** Field to flag after a server-side failure, from the [errorFields] table. */
    val errorField: ConnectField? get() = error?.kind?.let { errorFields[it] }

    private companion object {
        val errorFields: Map<ConnectErrorKind, ConnectField> = mapOf(
            ConnectErrorKind.INVALID_ACCESS_KEY to ConnectField.ACCESS_KEY,
            ConnectErrorKind.SIGNATURE_MISMATCH to ConnectField.SECRET,
            ConnectErrorKind.EXPIRED_CREDENTIALS to ConnectField.ACCESS_KEY,
            ConnectErrorKind.NO_SUCH_BUCKET to ConnectField.BUCKET,
            ConnectErrorKind.WRONG_REGION to ConnectField.REGION,
        )
    }
}

/** User intents on the Connect screen. */
sealed interface ConnectAction {
    /** Access key text changed. */
    data class AccessKeyChanged(val value: String) : ConnectAction

    /** Secret text changed. */
    data class SecretChanged(val value: String) : ConnectAction

    /** Bucket text changed. */
    data class BucketChanged(val value: String) : ConnectAction

    /** A region was picked. */
    data class RegionSelected(val code: String) : ConnectAction

    /** Remember switch toggled. */
    data class RememberChanged(val remember: Boolean) : ConnectAction

    /** A field lost focus. */
    data class FieldBlurred(val field: ConnectField) : ConnectAction

    /** Reveal/mask toggle on the secret field. */
    data object SecretVisibilityToggled : ConnectAction

    /** App went to the background: mask the secret. */
    data object AppBackgrounded : ConnectAction

    /** Connect button pressed (or IME Done). */
    data object Submit : ConnectAction

    /** "Switch to <region>" in the error banner. */
    data object ApplySuggestedRegion : ConnectAction

    /** Error banner dismissed. */
    data object DismissError : ConnectAction

    /** Theme toggle in the top row. */
    data object CycleTheme : ConnectAction
}

/** One-shot effects for the Connect screen. */
sealed interface ConnectEffect {
    /** A session is active; show the browser. */
    data object NavigateToBrowser : ConnectEffect
}
