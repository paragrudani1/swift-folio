package io.swiftfolio.app.fakes

import io.swiftfolio.app.data.AppSettings
import io.swiftfolio.app.data.CredentialFailure
import io.swiftfolio.app.data.CredentialResult
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.core.ConnectionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CredentialStore]; set [saveFailure] or [loadFailure] to simulate a broken store. */
class FakeCredentialStore(var saved: ConnectionConfig? = null) : CredentialStore {
    var saveFailure: CredentialFailure? = null
    var loadFailure: CredentialFailure? = null

    override suspend fun load(): CredentialResult<ConnectionConfig?> =
        loadFailure?.let { CredentialResult.Failure(it) } ?: CredentialResult.Success(saved)

    override suspend fun save(config: ConnectionConfig): CredentialResult<Unit> {
        saveFailure?.let { return CredentialResult.Failure(it) }
        saved = config
        return CredentialResult.Success(Unit)
    }

    override suspend fun clear(): CredentialResult<Unit> {
        saved = null
        return CredentialResult.Success(Unit)
    }
}

/** In-memory [SettingsStore]. */
class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setRememberCredentials(remember: Boolean) = state.update { it.copy(rememberCredentials = remember) }
}
