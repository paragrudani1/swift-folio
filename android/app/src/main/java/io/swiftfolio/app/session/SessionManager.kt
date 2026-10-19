package io.swiftfolio.app.session

import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.CredentialResult
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.app.data.S3RepositoryFactory
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.valueOrNull
import io.swiftfolio.app.transfer.TransferCredentials
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * A verified connection to one bucket.
 *
 * @property initialListing root listing fetched while verifying, or null if not fetched.
 */
data class Session(
    val config: ConnectionConfig,
    val repository: S3Repository,
    val initialListing: List<ObjectEntry>?,
)

/** Result of trying to reconnect with saved credentials at launch. */
sealed interface RestoreResult {
    /** No credentials are saved; the user has to connect manually. */
    data object NothingSaved : RestoreResult

    /** Saved credentials were verified and a session is active. */
    data class Connected(val session: Session) : RestoreResult

    /** Saved credentials exist but verification failed with [error]. */
    data class Failed(val config: ConnectionConfig, val error: ConnectError) : RestoreResult
}

/**
 * Owns the lifecycle of the active [Session]: connect, restore from saved credentials,
 * remember/forget and sign out. Screens observe [session]. The session's repository is shared
 * by the screens; background transfers build their own from [configFor], so closing the
 * session never breaks a running transfer.
 *
 * Thread-safety: methods are meant to be called from the main thread (ViewModels).
 */
class SessionManager(
    private val credentialStore: CredentialStore,
    private val settingsStore: SettingsStore,
    private val repositoryFactory: S3RepositoryFactory,
) : ActiveRepository, TransferCredentials {
    private val mutableSession = MutableStateFlow<Session?>(null)
    private var pendingRestoreFailure: RestoreResult.Failed? = null

    /** The active session, or null when signed out. */
    val session: StateFlow<Session?> = mutableSession.asStateFlow()

    /**
     * Verifies [config], reporting checklist steps through [onStep]. On success the session
     * becomes active and credentials are saved or forgotten according to [remember]; if they
     * cannot be saved the session still starts and "remember" is switched off.
     * On failure the previous session is left untouched.
     */
    suspend fun connect(
        config: ConnectionConfig,
        remember: Boolean,
        onStep: (ConnectStep, StepStatus) -> Unit,
    ): S3Result<Session> {
        val repository = repositoryFactory.create(config)
        return when (val verified = repository.verifyConnection(onStep)) {
            is S3Result.Failure -> {
                repository.close()
                verified
            }
            is S3Result.Success -> {
                persistChoice(config, remember)
                S3Result.Success(activate(Session(config, repository, verified.value)))
            }
        }
    }

    /** Reconnects with saved credentials. A failure is kept for [takeRestoreFailure]. */
    suspend fun restore(): RestoreResult {
        mutableSession.value?.let { return RestoreResult.Connected(it) }
        val saved = loadSaved() ?: return RestoreResult.NothingSaved
        val repository = repositoryFactory.create(saved)
        return when (val verified = repository.verifyConnection { _, _ -> }) {
            is S3Result.Success -> RestoreResult.Connected(activate(Session(saved, repository, verified.value)))
            is S3Result.Failure -> {
                repository.close()
                RestoreResult.Failed(saved, verified.error).also { pendingRestoreFailure = it }
            }
        }
    }

    /** Returns and clears the last failed restore, so Connect can show it once. */
    fun takeRestoreFailure(): RestoreResult.Failed? = pendingRestoreFailure.also { pendingRestoreFailure = null }

    /** Saved config used to prefill the Connect form, or null (also when the store is unreadable). */
    suspend fun savedConfig(): ConnectionConfig? = loadSaved()

    /** True when credentials are saved and readable (used to choose the start screen). */
    suspend fun hasSavedCredentials(): Boolean = loadSaved() != null

    /**
     * Turns "remember" on or off for the active session. Turning it off wipes saved
     * credentials immediately; turning it on saves the active config (if any).
     */
    suspend fun setRememberCredentials(remember: Boolean) {
        val active = mutableSession.value?.config
        if (remember && active != null) persistChoice(active, remember = true) else persistChoice(null, remember)
    }

    /** Removes saved credentials but keeps the active session. */
    suspend fun forgetSavedCredentials() {
        persistChoice(null, remember = false)
    }

    /**
     * Ends the session; optionally forgets saved credentials too. Does not touch transfers:
     * use [SignOut] from screens, which cancels them first.
     */
    suspend fun signOut(forgetCredentials: Boolean) {
        mutableSession.value?.repository?.close()
        mutableSession.value = null
        if (forgetCredentials) persistChoice(null, remember = false)
    }

    /**
     * Credentials for background transfers of [bucket]: the active session's config, or the
     * saved one when the process was restarted. Null when neither matches [bucket].
     */
    override suspend fun configFor(bucket: String): ConnectionConfig? =
        mutableSession.value?.config?.takeIf { it.bucket == bucket } ?: loadSaved()?.takeIf { it.bucket == bucket }

    override fun current(): S3Repository? = mutableSession.value?.repository

    /** Current remember flag. */
    suspend fun isRememberEnabled(): Boolean = settingsStore.settings.first().rememberCredentials

    private fun activate(session: Session): Session {
        mutableSession.value?.repository?.close()
        mutableSession.value = session
        return session
    }

    /** Saves [config] (or clears the store); a failed save records "remember" as off. */
    private suspend fun persistChoice(config: ConnectionConfig?, remember: Boolean) {
        val saved = if (remember && config != null) credentialStore.save(config) else credentialStore.clear()
        val saveFailed = config != null && saved is CredentialResult.Failure
        settingsStore.setRememberCredentials(remember && !saveFailed)
    }

    private suspend fun loadSaved(): ConnectionConfig? = credentialStore.load().valueOrNull()
}
