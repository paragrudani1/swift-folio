package io.swiftfolio.app.fakes

import io.swiftfolio.app.data.AppSettings
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.core.ConnectionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CredentialStore]. */
class FakeCredentialStore(var saved: ConnectionConfig? = null) : CredentialStore {
    override suspend fun load(): ConnectionConfig? = saved

    override suspend fun save(config: ConnectionConfig) {
        saved = config
    }

    override suspend fun clear() {
        saved = null
    }
}

/** In-memory [SettingsStore]. */
class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setRememberCredentials(remember: Boolean) = state.update { it.copy(rememberCredentials = remember) }
}
