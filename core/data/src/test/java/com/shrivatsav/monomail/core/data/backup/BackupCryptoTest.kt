package com.shrivatsav.monomail.core.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {
    private val plain = """{"accounts":[{"email":"me@example.com"}]}""".toByteArray()

    @Test
    fun roundTrip() {
        val blob = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), iterations = 1_000)
        assertArrayEquals(plain, BackupCrypto.decrypt(blob, "correct horse".toCharArray()))
    }

    @Test
    fun encryptionIsRandomizedAndHidesPlaintext() {
        val a = BackupCrypto.encrypt(plain, "pw".toCharArray(), iterations = 1_000)
        val b = BackupCrypto.encrypt(plain, "pw".toCharArray(), iterations = 1_000)
        assertFalse(a.contentEquals(b))
        assertFalse(String(a, Charsets.ISO_8859_1).contains("me@example.com"))
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val blob = BackupCrypto.encrypt(plain, "right".toCharArray(), iterations = 1_000)
        expectFailure { BackupCrypto.decrypt(blob, "wrong".toCharArray()) }
    }

    @Test
    fun tamperedFileIsRejected() {
        val blob = BackupCrypto.encrypt(plain, "pw".toCharArray(), iterations = 1_000)
        for (index in listOf(7, blob.size / 2, blob.size - 1)) { // header, body, tag
            val copy = blob.copyOf()
            copy[index] = (copy[index].toInt() xor 0x01).toByte()
            expectFailure { BackupCrypto.decrypt(copy, "pw".toCharArray()) }
        }
    }

    @Test
    fun nonBackupFilesAreRejected() {
        expectFailure { BackupCrypto.decrypt("hello world, not a backup at all".toByteArray(), "pw".toCharArray()) }
        expectFailure { BackupCrypto.decrypt(ByteArray(0), "pw".toCharArray()) }
    }

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("expected BackupException")
        } catch (e: BackupException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }
}
