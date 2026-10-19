package io.swiftfolio.app

/** App-module constants (storage names, timings). UI dimensions live in the theme. */
object AppConstants {
    /** DataStore file for non-secret settings. */
    const val SETTINGS_STORE_NAME = "settings"

    /** DataStore file for encrypted credentials. */
    const val CREDENTIALS_STORE_NAME = "credentials"

    /** Directory (inside filesDir) for transfer manifests. */
    const val TRANSFER_DIR_NAME = "transfers"

    /** Suffix of the FileProvider authority (after the application id) that shares API 26-28 downloads. */
    const val FILE_PROVIDER_SUFFIX = ".downloads"

    /** How long the revealed secret stays visible before it is masked again. */
    const val SECRET_REVEAL_MILLIS = 10_000L

    /** Longest time the splash screen waits for the start destination. */
    const val SPLASH_MAX_MILLIS = 400L

    /** Folder deletes or bulk deletes from this size require hold-to-confirm. */
    const val HOLD_TO_CONFIRM_THRESHOLD = 50
}
