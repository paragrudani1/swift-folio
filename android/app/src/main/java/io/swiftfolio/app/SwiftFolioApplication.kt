package io.swiftfolio.app

import android.app.Application

/** Creates the [AppContainer] and the notification channel at process start. */
class SwiftFolioApplication : Application() {
    /** The composition root; built on first access. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.notifications.ensureChannel()
    }
}
