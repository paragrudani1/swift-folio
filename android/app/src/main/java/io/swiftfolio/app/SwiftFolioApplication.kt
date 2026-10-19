package io.swiftfolio.app

import android.app.Application
import androidx.work.Configuration

/**
 * Creates the [AppContainer] and the notification channel at process start, and gives
 * WorkManager the container's worker factory. The default WorkManager initializer is removed
 * in the manifest, so WorkManager is initialised on demand with this configuration.
 */
class SwiftFolioApplication : Application(), Configuration.Provider {
    /** The composition root; built on first access. */
    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(container.workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        container.notifications.ensureChannel()
    }
}
