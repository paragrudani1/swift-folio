package io.swiftfolio.app.data.security

import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.swiftfolio.app.data.CredentialStore
import io.swiftfolio.core.ConnectionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.ProviderException

/**
 * [CredentialStore] that serialises the config to JSON, encrypts it with [KeystoreCipher]
 * (AES/GCM, key in Android Keystore) and stores ciphertext and IV as Base64 in DataStore.
 * Keystore operations are blocking IPC, so they run on [Dispatchers.Default].
 */
class KeystoreCredentialStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : CredentialStore {

    override suspend fun load(): ConnectionConfig? {
        val preferences = dataStore.data.first()
        val ciphertext = preferences[CIPHERTEXT] ?: return null
        val iv = preferences[IV] ?: return null
        return try {
            val plaintext = withContext(Dispatchers.Default) { cipher.decrypt(EncryptedPayload(decode(ciphertext), decode(iv))) }
            Json.decodeFromString<StoredConfig>(plaintext.decodeToString()).toConfig()
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

    override suspend fun save(config: ConnectionConfig) {
        val plaintext = Json.encodeToString(StoredConfig.from(config)).encodeToByteArray()
        val payload = withContext(Dispatchers.Default) { cipher.encrypt(plaintext) }
        dataStore.edit { preferences ->
            preferences[CIPHERTEXT] = encode(payload.ciphertext)
            preferences[IV] = encode(payload.iv)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(CIPHERTEXT)
            preferences.remove(IV)
        }
    }

    /** Logs only the failure type (never values), removes the entry and reports "nothing saved". */
    private suspend fun discardUnreadable(error: Exception): ConnectionConfig? {
        Log.w(TAG, "Saved credentials are unreadable and were removed: ${error::class.java.simpleName}")
        clear()
        return null
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

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
