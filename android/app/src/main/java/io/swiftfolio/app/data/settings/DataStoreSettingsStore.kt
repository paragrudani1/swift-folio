package io.swiftfolio.app.data.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.swiftfolio.app.data.AppSettings
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** [SettingsStore] backed by a Preferences DataStore. Read and write failures are logged by type, never thrown. */
class DataStoreSettingsStore(private val dataStore: DataStore<Preferences>) : SettingsStore {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Log.w(TAG, "Settings unreadable, using defaults: ${error::class.java.simpleName}")
            emit(emptyPreferences())
        }
        .map { preferences -> preferences.toSettings() }

    override suspend fun setThemeMode(mode: ThemeMode) = write { it[THEME_MODE] = mode.name }

    override suspend fun setRememberCredentials(remember: Boolean) = write { it[REMEMBER] = remember }

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { preferences -> change(preferences) }
        } catch (error: IOException) {
            Log.w(TAG, "Settings not saved: ${error::class.java.simpleName}")
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themeMode = this[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM,
        rememberCredentials = this[REMEMBER] ?: false,
    )

    private companion object {
        const val TAG = "SettingsStore"
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMEMBER = booleanPreferencesKey("remember_credentials")
    }
}
