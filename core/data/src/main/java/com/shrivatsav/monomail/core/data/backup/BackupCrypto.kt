package com.shrivatsav.monomail.core.data.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Passphrase-based encryption for backup files.
 *
 * Layout: MAGIC | iterations (int) | salt (16) | iv (12) | AES-256-GCM ciphertext+tag.
 * The header is bound to the ciphertext as GCM associated data, so any tampering
 * (including a changed iteration count) fails authentication. Uses only
 * javax.crypto so it behaves identically on a JVM and on Android (API 26+).
 */
object BackupCrypto {
    private val MAGIC = "MMBK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    const val DEFAULT_ITERATIONS = 310_000
    private const val MAX_ITERATIONS = 5_000_000
    private const val HEADER_BYTES = 5 + 4 + SALT_BYTES + IV_BYTES

    fun encrypt(plain: ByteArray, passphrase: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).putInt(iterations).put(salt).put(iv).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray, passphrase: CharArray): ByteArray {
        if (blob.size <= HEADER_BYTES || !blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException("This is not a MonoMail backup file.")
        }
        val buffer = ByteBuffer.wrap(blob)
        buffer.position(MAGIC.size)
        val iterations = buffer.int
        if (iterations < 1 || iterations > MAX_ITERATIONS) throw BackupException("The backup file is corrupted.")
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(blob, 0, HEADER_BYTES)
            cipher.doFinal(blob, HEADER_BYTES, blob.size - HEADER_BYTES)
        } catch (e: GeneralSecurityException) {
            throw BackupException("Wrong passphrase, or the backup file is corrupted.", e)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
