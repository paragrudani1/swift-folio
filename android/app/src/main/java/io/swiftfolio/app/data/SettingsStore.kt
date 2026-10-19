package io.swiftfolio.app.data

import kotlinx.coroutines.flow.Flow

/** Appearance preference. [SYSTEM] follows the device dark-theme setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Next mode in the Connect screen's toggle cycle: System -> Light -> Dark -> System. */
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** User preferences that are not secrets. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val rememberCredentials: Boolean = false,
)

/**
 * Non-secret preferences. [settings] emits the current value immediately and again after
 * every change; it never completes and falls back to defaults if storage is unreadable.
 */
interface SettingsStore {
    /** Current settings, replayed to new collectors. */
    val settings: Flow<AppSettings>

    /** Persists [mode]. Never throws; a failed write is logged and the old value stays. */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Persists whether credentials should be remembered. Does not touch the credential store.
     * Never throws; a failed write is logged and the old value stays.
     */
    suspend fun setRememberCredentials(remember: Boolean)
}
