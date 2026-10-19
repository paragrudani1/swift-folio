package io.swiftfolio.app.data

import io.swiftfolio.core.ConnectionConfig

/**
 * Persists one [ConnectionConfig] encrypted at rest.
 *
 * Invariants: at most one config is stored; secrets never leave the implementation in
 * plaintext except through [load]; implementations never log values.
 */
interface CredentialStore {
    /**
     * Returns the saved config, or null when nothing is saved or the stored data can no
     * longer be decrypted (for example after the Keystore key was invalidated). In the
     * second case the unreadable entry is removed.
     */
    suspend fun load(): ConnectionConfig?

    /** Encrypts and stores [config], replacing any previous one. Throws only on storage I/O failure. */
    suspend fun save(config: ConnectionConfig)

    /** Removes the saved config, if any. Idempotent. */
    suspend fun clear()
}
