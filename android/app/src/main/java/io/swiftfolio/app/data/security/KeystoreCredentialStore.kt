package io.swiftfolio.app.data.security

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.swiftfolio.app.data.CredentialFailure
import io.swiftfolio.app.data.CredentialResult
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.core.ConnectionConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.util.Base64

/**
 * [CredentialStore] that serialises the config to JSON, encrypts it with a [PayloadCipher]
 * (AES/GCM, key in Android Keystore) and stores ciphertext and IV as Base64 in DataStore.
 * Keystore operations are blocking IPC, so they run on [cipherDispatcher]. Failures are
 * logged by type only and returned as [CredentialResult.Failure].
 */
class KeystoreCredentialStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: PayloadCipher,
    private val cipherDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CredentialStore {

    override suspend fun load(): CredentialResult<ConnectionConfig?> {
        val preferences = try {
            dataStore.data.first()
        } catch (error: IOException) {
            return failure(CredentialFailure.STORAGE, error)
        }
        val ciphertext = preferences[CIPHERTEXT] ?: return CredentialResult.Success(null)
        val iv = preferences[IV] ?: return CredentialResult.Success(null)
        return try {
            CredentialResult.Success(decrypt(ciphertext, iv))
        } catch (error: GeneralSecurityException) {
            discardUnreadable(error)
        } catch (error: ProviderException) {
            discardUnreadable(error)
        } catch (error: SerializationException) {
            discardUnreadable(error)
        } catch (error: IllegalArgumentException) {
            discardUnreadable(error)
        }
    }

    override suspend fun save(config: ConnectionConfig): CredentialResult<Unit> {
        val payload = try {
            val plaintext = Json.encodeToString(StoredConfig.serializer(), StoredConfig.from(config)).encodeToByteArray()
            withContext(cipherDispatcher) { cipher.encrypt(plaintext) }
        } catch (error: GeneralSecurityException) {
            return failure(CredentialFailure.KEYSTORE, error)
        } catch (error: ProviderException) {
            return failure(CredentialFailure.KEYSTORE, error)
        }
        return write { preferences ->
            preferences[CIPHERTEXT] = encode(payload.ciphertext)
            preferences[IV] = encode(payload.iv)
        }
    }

    override suspend fun clear(): CredentialResult<Unit> = write { preferences ->
        preferences.remove(CIPHERTEXT)
        preferences.remove(IV)
    }

    private suspend fun decrypt(ciphertext: String, iv: String): ConnectionConfig {
        val payload = EncryptedPayload(decode(ciphertext), decode(iv))
        val plaintext = withContext(cipherDispatcher) { cipher.decrypt(payload) }
        return Json.decodeFromString(StoredConfig.serializer(), plaintext.decodeToString()).toConfig()
    }

    private suspend fun write(change: (MutablePreferences) -> Unit): CredentialResult<Unit> =
        try {
            dataStore.edit { preferences -> change(preferences) }
            CredentialResult.Success(Unit)
        } catch (error: IOException) {
            failure(CredentialFailure.STORAGE, error)
        }

    /** Removes an entry that can no longer be decrypted and reports "nothing saved". */
    private suspend fun discardUnreadable(error: Exception): CredentialResult<ConnectionConfig?> {
        Log.w(TAG, "Saved credentials are unreadable and were removed: ${error::class.java.simpleName}")
        return when (val cleared = clear()) {
            is CredentialResult.Success -> CredentialResult.Success(null)
            is CredentialResult.Failure -> cleared
        }
    }

    private fun failure(reason: CredentialFailure, error: Exception): CredentialResult.Failure {
        Log.w(TAG, "Credential store $reason failure: ${error::class.java.simpleName}")
        return CredentialResult.Failure(reason)
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    /** On-disk (pre-encryption) shape; decoupled from the domain type so it can evolve. */
    @Serializable
    private data class StoredConfig(
        val accessKeyId: String,
        val secretAccessKey: String,
        val bucket: String,
        val region: String,
    ) {
        fun toConfig() = ConnectionConfig(accessKeyId, secretAccessKey, bucket, region)

        override fun toString(): String = "StoredConfig(<redacted>)"

        companion object {
            fun from(config: ConnectionConfig) =
                StoredConfig(config.accessKeyId, config.secretAccessKey, config.bucket, config.region)
        }
    }

    private companion object {
        const val TAG = "CredentialStore"
        val CIPHERTEXT = stringPreferencesKey("credentials_ciphertext")
        val IV = stringPreferencesKey("credentials_iv")
    }
}
