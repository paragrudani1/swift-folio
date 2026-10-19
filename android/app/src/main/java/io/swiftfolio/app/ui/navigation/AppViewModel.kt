package io.swiftfolio.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** App-wide state: theme preference and the start destination (null until known). */
data class AppUiState(val themeMode: ThemeMode = ThemeMode.SYSTEM, val startRoute: String? = null)

/**
 * Decides the first screen: the browser (which auto-connects) when credentials are
 * remembered, otherwise Connect. Also exposes the theme preference for the root theme.
 */
class AppViewModel(sessionManager: SessionManager, settingsStore: SettingsStore) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())

    /** Current app state. */
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val route = if (sessionManager.hasSavedCredentials()) Routes.BROWSER else Routes.CONNECT
            mutableState.update { it.copy(startRoute = route) }
        }
        viewModelScope.launch {
            settingsStore.settings.collect { settings -> mutableState.update { it.copy(themeMode = settings.themeMode) } }
        }
    }
}
