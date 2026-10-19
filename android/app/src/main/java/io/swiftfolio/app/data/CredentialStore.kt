package io.swiftfolio.app.data

import io.swiftfolio.core.ConnectionConfig

/**
 * Persists one [ConnectionConfig] encrypted at rest.
 *
 * Invariants: at most one config is stored; secrets never leave the implementation in
 * plaintext except through [load]; implementations never log values. No method throws for
 * storage or Keystore failures: they come back as [CredentialResult.Failure]. Coroutine
 * cancellation is propagated.
 */
interface CredentialStore {
    /**
     * Returns the saved config, or `Success(null)` when nothing is saved. Data that can no longer
     * be decrypted (for example after the Keystore key was invalidated) is removed and reported
     * as `Success(null)`. Fails with [CredentialFailure.STORAGE] when the file cannot be read.
     */
    suspend fun load(): CredentialResult<ConnectionConfig?>

    /**
     * Encrypts and stores [config], replacing any previous one. Fails with
     * [CredentialFailure.KEYSTORE] when encryption is refused and [CredentialFailure.STORAGE]
     * when the file cannot be written; the previous entry is then left as it was.
     */
    suspend fun save(config: ConnectionConfig): CredentialResult<Unit>

    /** Removes the saved config, if any. Idempotent. Fails only with [CredentialFailure.STORAGE]. */
    suspend fun clear(): CredentialResult<Unit>
}

/** Outcome of a [CredentialStore] operation. */
sealed interface CredentialResult<out T> {
    /** The operation completed. */
    data class Success<out T>(val value: T) : CredentialResult<T>

    /** The operation failed for [reason]; nothing was changed. */
    data class Failure(val reason: CredentialFailure) : CredentialResult<Nothing>
}

/** Why a [CredentialStore] operation failed. */
enum class CredentialFailure {
    /** The encrypted file could not be read or written. */
    STORAGE,

    /** The Android Keystore refused to encrypt. */
    KEYSTORE,
}

/** The value of a successful result, or null after a failure. */
fun <T> CredentialResult<T>.valueOrNull(): T? = (this as? CredentialResult.Success)?.value
