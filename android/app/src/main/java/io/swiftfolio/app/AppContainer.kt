package io.swiftfolio.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import io.swiftfolio.app.bucket.BrowserUseCases
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.aws.AwsS3RepositoryFactory
import io.swiftfolio.app.data.security.KeystoreCipher
import io.swiftfolio.app.data.security.KeystoreCredentialStore
import io.swiftfolio.app.data.settings.DataStoreSettingsStore
import io.swiftfolio.app.session.CancelTransfersAndSignOut
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.session.SignOut
import io.swiftfolio.app.transfer.ContentUploadSources
import io.swiftfolio.app.transfer.DownloadTargetFactory
import io.swiftfolio.app.transfer.TransferControl
import io.swiftfolio.app.transfer.TransferManifestStore
import io.swiftfolio.app.transfer.TransferNotifications
import io.swiftfolio.app.transfer.TransferScheduler
import io.swiftfolio.app.transfer.TransferWorkerDependencies
import io.swiftfolio.app.transfer.TransferWorkerFactory
import io.swiftfolio.app.transfer.WorkManagerTransferScheduler
import java.io.File

/**
 * Hand-written composition root. Created once by [SwiftFolioApplication]; every
 * long-lived object is built here and handed to its users through constructors.
 * Screens receive it through ViewModel factories in the nav host, workers through
 * [workerFactory], which the Application registers with WorkManager.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** Non-secret preferences. */
    val settingsStore: SettingsStore = DataStoreSettingsStore(preferencesStore(AppConstants.SETTINGS_STORE_NAME))

    /** Encrypted credentials. */
    val credentialStore: CredentialStore = KeystoreCredentialStore(preferencesStore(AppConstants.CREDENTIALS_STORE_NAME), KeystoreCipher())

    /** Owner of the active bucket session. */
    val sessionManager: SessionManager = SessionManager(credentialStore, settingsStore, AwsS3RepositoryFactory)

    /** Per-file cancellation flags shared by the UI and workers. */
    private val transferControl: TransferControl = TransferControl()

    /** Batch manifests and per-file outcomes on disk. */
    private val manifestStore: TransferManifestStore = TransferManifestStore(File(appContext.filesDir, AppConstants.TRANSFER_DIR_NAME))

    /** Content URI access for uploads. */
    private val uploadSources: ContentUploadSources = ContentUploadSources(appContext.contentResolver)

    /** Transfer notifications. */
    val notifications: TransferNotifications = TransferNotifications(appContext)

    /** Builds transfer workers with their dependencies (registered through `Configuration.Provider`). */
    val workerFactory: TransferWorkerFactory = TransferWorkerFactory(
        TransferWorkerDependencies(
            manifestStore = manifestStore,
            credentials = sessionManager,
            repositoryFactory = AwsS3RepositoryFactory,
            control = transferControl,
            uploadSources = uploadSources,
            downloadTargets = DownloadTargetFactory(appContext),
            notifications = notifications,
        ),
    )

    /** Background transfers. WorkManager is initialised on first use, after the Application is created. */
    val transferScheduler: TransferScheduler by lazy {
        WorkManagerTransferScheduler(WorkManager.getInstance(appContext), manifestStore, transferControl, uploadSources)
    }

    /** Sign-out for every screen: cancels transfers, then ends the session. */
    val signOut: SignOut by lazy { CancelTransfersAndSignOut(sessionManager, transferScheduler) }

    /** Fresh browser use cases (per-screen caches) bound to the active session. */
    fun browserUseCases(): BrowserUseCases = BrowserUseCases.create(sessionManager)

    /** A Preferences DataStore that starts empty instead of failing when its file is corrupt. */
    private fun preferencesStore(name: String): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { appContext.preferencesDataStoreFile(name) },
    )
}
