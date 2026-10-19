package io.swiftfolio.app.session

import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.app.data.S3RepositoryFactory
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.StepStatus
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
 * remember/forget and sign out. Screens observe [session]; nothing else creates repositories.
 *
 * Thread-safety: methods are meant to be called from the main thread (ViewModels).
 */
class SessionManager(
    private val credentialStore: CredentialStore,
    private val settingsStore: SettingsStore,
    private val repositoryFactory: S3RepositoryFactory,
) {
    private val mutableSession = MutableStateFlow<Session?>(null)
    private var pendingRestoreFailure: RestoreResult.Failed? = null

    /** The active session, or null when signed out. */
    val session: StateFlow<Session?> = mutableSession.asStateFlow()

    /**
     * Verifies [config], reporting checklist steps through [onStep]. On success the session
     * becomes active and credentials are saved or forgotten according to [remember].
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
        val saved = credentialStore.load() ?: return RestoreResult.NothingSaved
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

    /** Saved config used to prefill the Connect form, or null. */
    suspend fun savedConfig(): ConnectionConfig? = credentialStore.load()

    /** True when credentials are saved (used to choose the start screen). */
    suspend fun hasSavedCredentials(): Boolean = credentialStore.load() != null

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

    /** Ends the session; optionally forgets saved credentials too. */
    suspend fun signOut(forgetCredentials: Boolean) {
        mutableSession.value?.repository?.close()
        mutableSession.value = null
        if (forgetCredentials) persistChoice(null, remember = false)
    }

    /**
     * Repository for background transfers of [bucket]: the active session's, or one built
     * from saved credentials when the process was restarted. Null when neither matches.
     * The caller must not close a repository that belongs to the active session.
     */
    suspend fun repositoryFor(bucket: String): S3Repository? {
        mutableSession.value?.takeIf { it.config.bucket == bucket }?.let { return it.repository }
        val saved = credentialStore.load()?.takeIf { it.bucket == bucket } ?: return null
        return repositoryFactory.create(saved)
    }

    /** True when [repository] is owned by the active session (and must stay open). */
    fun isSessionRepository(repository: S3Repository): Boolean = mutableSession.value?.repository === repository

    /** Current remember flag. */
    suspend fun isRememberEnabled(): Boolean = settingsStore.settings.first().rememberCredentials

    private fun activate(session: Session): Session {
        mutableSession.value?.repository?.close()
        mutableSession.value = session
        return session
    }

    private suspend fun persistChoice(config: ConnectionConfig?, remember: Boolean) {
        settingsStore.setRememberCredentials(remember)
        if (remember && config != null) credentialStore.save(config) else credentialStore.clear()
    }
}
