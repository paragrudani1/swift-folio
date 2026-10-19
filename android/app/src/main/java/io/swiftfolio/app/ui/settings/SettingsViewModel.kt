package io.swiftfolio.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.session.SignOut
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Settings state holder; persistence goes through [SettingsStore] and [SessionManager], sign-out through [SignOut]. */
class SettingsViewModel(
    private val sessionManager: SessionManager,
    private val settingsStore: SettingsStore,
    private val signOut: SignOut,
    appVersion: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState(appVersion = appVersion))
    private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)

    /** Current screen state. */
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    /** One-shot navigation effects. */
    val effects: Flow<SettingsEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.session, settingsStore.settings) { session, settings -> session to settings }
                .collect { (session, settings) ->
                    mutableState.update {
                        it.copy(
                            bucket = session?.config?.bucket.orEmpty(),
                            region = session?.config?.region.orEmpty(),
                            maskedAccessKey = session?.config?.maskedAccessKeyId.orEmpty(),
                            isConnected = session != null,
                            themeMode = settings.themeMode,
                            rememberCredentials = settings.rememberCredentials,
                        )
                    }
                }
        }
    }

    /** Single entry point for user intents. */
    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetTheme -> viewModelScope.launch { settingsStore.setThemeMode(action.mode) }
            is SettingsAction.SetRemember -> viewModelScope.launch { sessionManager.setRememberCredentials(action.remember) }
            SettingsAction.RequestSignOut -> mutableState.update { it.copy(signOutDialog = SignOutDialog(forgetCredentials = it.rememberCredentials)) }
            is SettingsAction.ForgetOnSignOutChanged -> mutableState.update { it.copy(signOutDialog = it.signOutDialog?.copy(forgetCredentials = action.forget)) }
            SettingsAction.DismissSignOut -> mutableState.update { it.copy(signOutDialog = null) }
            SettingsAction.ConfirmSignOut -> confirmSignOut()
        }
    }

    private fun confirmSignOut() {
        val forget = state.value.signOutDialog?.forgetCredentials ?: false
        mutableState.update { it.copy(signOutDialog = null) }
        viewModelScope.launch {
            signOut(forgetCredentials = forget)
            effectChannel.send(SettingsEffect.NavigateToConnect)
        }
    }
}
