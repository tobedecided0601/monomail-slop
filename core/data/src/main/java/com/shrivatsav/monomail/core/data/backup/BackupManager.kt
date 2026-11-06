package com.shrivatsav.monomail.core.data.backup

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.shrivatsav.monomail.core.data.auth.AuthManager
import com.shrivatsav.monomail.core.data.auth.UserProfile
import com.shrivatsav.monomail.core.data.settings.SettingsDataStore
import com.shrivatsav.monomail.core.data.worker.DeepSyncService
import com.shrivatsav.monomail.core.network.provider.ProviderCache
import com.shrivatsav.monomail.data.pgp.PgpKeyInfo
import com.shrivatsav.monomail.data.pgp.PgpKeyStorage
import com.shrivatsav.monomail.security.SecurityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** One DataStore preference, stored as text plus its type so it can be restored exactly. */
data class BackupPref(val type: String, val value: String? = null, val values: List<String>? = null)

data class BackupPgpKey(
    val info: PgpKeyInfo,
    val privateKey: String?,
    val publicKey: String?,
    val passphrase: String?
)

/** Everything in a backup file. Contains plaintext credentials: only ever written encrypted. */
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val accounts: List<UserProfile> = emptyList(),
    val activeAccountId: String? = null,
    val settings: Map<String, BackupPref> = emptyMap(),
    val pgpKeys: List<BackupPgpKey> = emptyList()
) {
    companion object { const val CURRENT_VERSION = 1 }
}

data class BackupSummary(
    val accounts: Int,
    val skippedAccounts: Int,
    val settings: Int,
    val pgpKeys: Int
)

/** Progress of a backup or restore, for the screens that start one. */
sealed class BackupUiState {
    object Idle : BackupUiState()
    object Working : BackupUiState()
    data class Done(val message: String) : BackupUiState()
    data class Failed(val message: String) : BackupUiState()
}

/**
 * Exports app state to a passphrase-encrypted blob and restores it on a fresh install.
 *
 * Included: IMAP accounts (server config + password), app settings (including
 * per-account notification profiles and templates), and PGP keys.
 * Not included: downloaded mail (it re-syncs from the server) and Gmail-API /
 * Outlook accounts, whose OAuth tokens cannot be reused on another install.
 */
class BackupManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val settingsDataStore: SettingsDataStore,
    private val pgpKeyStorage: PgpKeyStorage
) {
    private val gson = Gson()

    /** Encrypts a backup with [passphrase] and writes it to [uri] (a document picked by the user). */
    suspend fun exportTo(resolver: ContentResolver, uri: Uri, passphrase: CharArray): String {
        val (blob, summary) = createBackup(passphrase)
        withContext(Dispatchers.IO) {
            val out = resolver.openOutputStream(uri, "wt") ?: throw BackupException("Could not write to the chosen location.")
            out.use { it.write(blob) }
        }
        return "Backup saved: ${describe(summary)}"
    }

    /** Reads the backup at [uri], decrypts it with [passphrase] and restores it. */
    suspend fun restoreFrom(resolver: ContentResolver, uri: Uri, passphrase: CharArray): String {
        val blob = withContext(Dispatchers.IO) {
            val input = resolver.openInputStream(uri) ?: throw BackupException("Could not read the chosen file.")
            input.use { stream ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val read = stream.read(chunk)
                    if (read < 0) break
                    buffer.write(chunk, 0, read)
                    if (buffer.size() > MAX_BACKUP_BYTES) throw BackupException("That file is too large to be a MonoMail backup.")
                }
                buffer.toByteArray()
            }
        }
        val summary = restoreBackup(blob, passphrase)
        return "Restored: ${describe(summary)}"
    }

    private fun describe(summary: BackupSummary): String {
        val parts = mutableListOf("${summary.accounts} account${if (summary.accounts == 1) "" else "s"}")
        parts += "${summary.settings} settings"
        if (summary.pgpKeys > 0) parts += "${summary.pgpKeys} PGP key${if (summary.pgpKeys == 1) "" else "s"}"
        val skipped = if (summary.skippedAccounts > 0) {
            ". ${summary.skippedAccounts} Gmail/Outlook sign-in account${if (summary.skippedAccounts == 1) " was" else "s were"} not included; add ${if (summary.skippedAccounts == 1) "it" else "them"} again."
        } else ""
        return parts.joinToString(", ") + skipped
    }

    suspend fun createBackup(passphrase: CharArray): Pair<ByteArray, BackupSummary> {
        val allAccounts = authManager.getAccounts()
        val accounts = allAccounts.filter { it.provider == "imap" }.mapNotNull { account ->
            // IMAP config and password are Keystore-encrypted on this device; decrypt
            // them so the backup can be restored on a device with a different Keystore.
            val config = SecurityUtil.decryptString(account.accessToken)
            val password = SecurityUtil.decryptString(account.refreshToken)
            if (config == null || password == null) null
            else account.copy(accessToken = config, refreshToken = password)
        }
        val settings = settingsDataStore.exportPreferences()
        val pgpKeys = pgpKeyStorage.loadAllMetadata().values.map { info ->
            BackupPgpKey(
                info = info,
                privateKey = pgpKeyStorage.loadPrivateKey(info.fingerprint),
                publicKey = pgpKeyStorage.loadPublicKey(info.fingerprint),
                passphrase = pgpKeyStorage.loadPassphrase(info.fingerprint)
            )
        }
        val payload = BackupPayload(
            accounts = accounts,
            activeAccountId = authManager.currentUser?.id?.takeIf { id -> accounts.any { it.id == id } },
            settings = settings,
            pgpKeys = pgpKeys
        )
        val encrypted = BackupCrypto.encrypt(gson.toJson(payload).toByteArray(Charsets.UTF_8), passphrase)
        return encrypted to BackupSummary(
            accounts = accounts.size,
            skippedAccounts = allAccounts.size - accounts.size,
            settings = settings.size,
            pgpKeys = pgpKeys.size
        )
    }

    suspend fun restoreBackup(data: ByteArray, passphrase: CharArray): BackupSummary {
        val json = String(BackupCrypto.decrypt(data, passphrase), Charsets.UTF_8)
        val payload = try {
            gson.fromJson(json, BackupPayload::class.java)
        } catch (e: Exception) {
            throw BackupException("The backup file could not be read.", e)
        } ?: throw BackupException("The backup file is empty.")
        if (payload.version > BackupPayload.CURRENT_VERSION) {
            throw BackupException("This backup was made by a newer version of MonoMail. Update the app and try again.")
        }

        val settingsApplied = settingsDataStore.importPreferences(payload.settings)

        payload.pgpKeys.forEach { key ->
            val fingerprint = key.info.fingerprint
            key.privateKey?.let { pgpKeyStorage.savePrivateKey(fingerprint, it) }
            key.publicKey?.let { pgpKeyStorage.savePublicKey(fingerprint, it) }
            key.passphrase?.let { pgpKeyStorage.savePassphrase(fingerprint, it) }
            pgpKeyStorage.saveKeyMetadata(fingerprint, key.info)
        }

        val restored = mutableListOf<UserProfile>()
        for (account in payload.accounts.filter { it.provider == "imap" }) {
            val profile = account.copy(
                accessToken = SecurityUtil.encryptString(account.accessToken),
                refreshToken = SecurityUtil.encryptString(account.refreshToken)
            )
            authManager.addAccount(profile)
            ProviderCache.invalidate(profile.id)
            restored += profile
        }
        val active = restored.firstOrNull { it.id == payload.activeAccountId } ?: restored.firstOrNull()
        if (active != null) authManager.switchAccount(active.id)
        restored.forEach { profile ->
            try {
                DeepSyncService.start(context, profile.id, INITIAL_SYNC_DAYS)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start initial sync for ${profile.id}", e)
            }
        }

        return BackupSummary(
            accounts = restored.size,
            skippedAccounts = payload.accounts.size - restored.size,
            settings = settingsApplied,
            pgpKeys = payload.pgpKeys.size
        )
    }

    private companion object {
        const val TAG = "BackupManager"
        const val INITIAL_SYNC_DAYS = 30
        const val MAX_BACKUP_BYTES = 16 * 1024 * 1024
    }
}
