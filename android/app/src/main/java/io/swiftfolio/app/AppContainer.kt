package io.swiftfolio.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.aws.AwsS3RepositoryFactory
import io.swiftfolio.app.data.security.KeystoreCipher
import io.swiftfolio.app.data.security.KeystoreCredentialStore
import io.swiftfolio.app.data.settings.DataStoreSettingsStore
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.transfer.ContentUploadSources
import io.swiftfolio.app.transfer.DownloadTargetFactory
import io.swiftfolio.app.transfer.TransferControl
import io.swiftfolio.app.transfer.TransferManifestStore
import io.swiftfolio.app.transfer.TransferNotifications
import io.swiftfolio.app.transfer.TransferScheduler
import io.swiftfolio.app.transfer.WorkManagerTransferScheduler
import java.io.File

/**
 * Hand-written composition root. Created once by [SwiftFolioApplication]; every
 * long-lived object is built here and handed to its users through constructors.
 * Screens receive it through ViewModel factories in the nav host, workers through [appContainer].
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** Non-secret preferences. */
    val settingsStore: SettingsStore = DataStoreSettingsStore(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile(AppConstants.SETTINGS_STORE_NAME) },
    )

    /** Encrypted credentials. */
    val credentialStore: CredentialStore = KeystoreCredentialStore(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile(AppConstants.CREDENTIALS_STORE_NAME) },
        KeystoreCipher(),
    )

    /** Owner of the active bucket session. */
    val sessionManager: SessionManager = SessionManager(credentialStore, settingsStore, AwsS3RepositoryFactory)

    /** Per-file cancellation flags shared by the UI and workers. */
    val transferControl: TransferControl = TransferControl()

    /** Batch manifests on disk. */
    val manifestStore: TransferManifestStore = TransferManifestStore(File(appContext.filesDir, AppConstants.TRANSFER_DIR_NAME))

    /** Content URI access for uploads. */
    val uploadSources: ContentUploadSources = ContentUploadSources(appContext.contentResolver)

    /** Destinations for downloads. */
    val downloadTargets: DownloadTargetFactory = DownloadTargetFactory(appContext)

    /** Transfer notifications. */
    val notifications: TransferNotifications = TransferNotifications(appContext)

    /** Background transfers. */
    val transferScheduler: TransferScheduler by lazy {
        WorkManagerTransferScheduler(WorkManager.getInstance(appContext), manifestStore, transferControl, uploadSources)
    }
}

/** The process-wide [AppContainer], reachable from any [Context] of this app. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SwiftFolioApplication).container
