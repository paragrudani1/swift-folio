package io.swiftfolio.app.ui.settings

import io.swiftfolio.app.data.ThemeMode

/** Settings screen state. */
data class SettingsUiState(
    val bucket: String = "",
    val region: String = "",
    val maskedAccessKey: String = "",
    val isConnected: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val rememberCredentials: Boolean = false,
    val appVersion: String = "",
    val signOutDialog: SignOutDialog? = null,
)

/** The sign-out confirmation; [forgetCredentials] defaults to the remember setting. */
data class SignOutDialog(val forgetCredentials: Boolean)

/** User intents on Settings. */
sealed interface SettingsAction {
    /** Theme segment picked. */
    data class SetTheme(val mode: ThemeMode) : SettingsAction

    /** Remember switch toggled; off wipes saved credentials immediately. */
    data class SetRemember(val remember: Boolean) : SettingsAction

    /** Sign-out button pressed. */
    data object RequestSignOut : SettingsAction

    /** Checkbox in the sign-out dialog. */
    data class ForgetOnSignOutChanged(val forget: Boolean) : SettingsAction

    /** Sign-out confirmed. */
    data object ConfirmSignOut : SettingsAction

    /** Sign-out dialog dismissed. */
    data object DismissSignOut : SettingsAction
}

/** One-shot effects. */
sealed interface SettingsEffect {
    /** Signed out: go to Connect and clear the back stack. */
    data object NavigateToConnect : SettingsEffect
}
