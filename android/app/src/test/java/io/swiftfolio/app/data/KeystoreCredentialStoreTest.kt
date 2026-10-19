package io.swiftfolio.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.swiftfolio.app.data.security.EncryptedPayload
import io.swiftfolio.app.data.security.KeystoreCredentialStore
import io.swiftfolio.app.data.security.PayloadCipher
import io.swiftfolio.app.fakes.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

class KeystoreCredentialStoreTest {
    private val scope = TestScope()
    private val dataStore = FakePreferencesDataStore()
    private val cipher = FakeCipher()
    private val store = KeystoreCredentialStore(dataStore, cipher, StandardTestDispatcher(scope.testScheduler))

    @Test fun `saved credentials load back`() = scope.runTest {
        assertEquals(CredentialResult.Success(Unit), store.save(TestData.config))
        assertEquals(CredentialResult.Success(TestData.config), store.load())
        assertEquals(CredentialResult.Success(Unit), store.clear())
        assertEquals(CredentialResult.Success(null), store.load())
    }

    @Test fun `an unreadable file is a storage failure, not a crash`() = scope.runTest {
        dataStore.failReads = true
        assertEquals(CredentialResult.Failure(CredentialFailure.STORAGE), store.load())
    }

    @Test fun `a keystore refusal keeps the previous entry`() = scope.runTest {
        store.save(TestData.config)
        cipher.encryptError = ProviderException("keystore busy")
        assertEquals(CredentialResult.Failure(CredentialFailure.KEYSTORE), store.save(TestData.config.copy(bucket = "other")))
        cipher.encryptError = null
        assertEquals(CredentialResult.Success(TestData.config), store.load())
    }

    @Test fun `a failed write is a storage failure`() = scope.runTest {
        dataStore.failWrites = true
        assertEquals(CredentialResult.Failure(CredentialFailure.STORAGE), store.save(TestData.config))
        assertEquals(CredentialResult.Failure(CredentialFailure.STORAGE), store.clear())
    }

    @Test fun `data that no longer decrypts is removed and reported as nothing saved`() = scope.runTest {
        store.save(TestData.config)
        cipher.decryptError = GeneralSecurityException("key invalidated")
        assertEquals(CredentialResult.Success(null), store.load())
        cipher.decryptError = null
        assertEquals(CredentialResult.Success(null), store.load())
    }
}

/** In-memory DataStore that can fail reads or writes with [IOException]. */
private class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    var failReads = false
    var failWrites = false

    override val data: Flow<Preferences> = flow {
        if (failReads) throw IOException("read failed")
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("write failed")
        return transform(state.value).also { state.value = it }
    }
}

/** Reversible "cipher" (XOR) whose operations can be made to fail. */
private class FakeCipher : PayloadCipher {
    var encryptError: Exception? = null
    var decryptError: Exception? = null

    override fun encrypt(plaintext: ByteArray): EncryptedPayload {
        encryptError?.let { throw it }
        return EncryptedPayload(plaintext.map { (it.toInt() xor KEY).toByte() }.toByteArray(), byteArrayOf(1, 2, 3))
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray {
        decryptError?.let { throw it }
        return payload.ciphertext.map { (it.toInt() xor KEY).toByte() }.toByteArray()
    }

    private companion object {
        const val KEY = 0x5A
    }
}
