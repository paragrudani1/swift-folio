package io.swiftfolio.app.data.security

/** Ciphertext plus the random IV it was produced with. */
class EncryptedPayload(val ciphertext: ByteArray, val iv: ByteArray)

/**
 * Symmetric authenticated encryption used by [KeystoreCredentialStore]. The seam lets the
 * store be tested without the Android Keystore.
 */
interface PayloadCipher {
    /**
     * Encrypts [plaintext] with a fresh random IV. Throws [java.security.GeneralSecurityException]
     * or [java.security.ProviderException] when the key is unavailable.
     */
    fun encrypt(plaintext: ByteArray): EncryptedPayload

    /**
     * Decrypts [payload]. Throws [java.security.GeneralSecurityException] or
     * [java.security.ProviderException] when the data or key is invalid.
     */
    fun decrypt(payload: EncryptedPayload): ByteArray
}
