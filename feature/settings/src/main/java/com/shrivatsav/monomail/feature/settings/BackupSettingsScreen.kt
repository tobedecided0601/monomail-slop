package com.shrivatsav.monomail.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shrivatsav.monomail.core.data.backup.BackupUiState
import com.shrivatsav.monomail.ui.components.PassphraseDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BackupSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.backupState.collectAsState()

    // Which step the user is in. The picked file is kept until the passphrase is entered.
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> pendingExportUri = uri }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }

    pendingExportUri?.let { uri ->
        PassphraseDialog(
            title = "Protect your backup",
            message = "The backup contains your mail passwords, so it is encrypted with a passphrase. " +
                "There is no way to recover it if you forget it.",
            confirmLabel = "Create backup",
            confirm = true,
            onSubmit = { passphrase ->
                pendingExportUri = null
                viewModel.exportBackup(context.contentResolver, uri, passphrase)
            },
            onDismiss = { pendingExportUri = null }
        )
    }
    pendingRestoreUri?.let { uri ->
        PassphraseDialog(
            title = "Unlock backup",
            message = "Enter the passphrase you chose when you created this backup.",
            confirmLabel = "Restore",
            confirm = false,
            onSubmit = { passphrase ->
                pendingRestoreUri = null
                viewModel.restoreBackup(context.contentResolver, uri, passphrase)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }
    when (val current = state) {
        is BackupUiState.Done -> ResultDialog("Done", current.message, viewModel::clearBackupState)
        is BackupUiState.Failed -> ResultDialog("Couldn't finish", current.message, viewModel::clearBackupState)
        else -> Unit
    }

    ScrollableSettingsScaffold(title = "Backup & restore", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GroupLabel(text = "What's included")
            SettingsCard {
                Text(
                    text = "Your IMAP accounts (Gmail and Yahoo included), all app settings, notification " +
                        "preferences, email templates and PGP keys.\n\n" +
                        "Mail itself is not saved; it downloads again from the server after you restore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            GroupLabel(text = "Backup")
            SettingsCard {
                InfoRow(
                    icon = Icons.Rounded.FileUpload,
                    title = if (state is BackupUiState.Working) "Working…" else "Create backup",
                    value = "Save an encrypted file",
                    onClick = if (state is BackupUiState.Working) null else ({
                        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportPicker.launch("monomail-backup-$stamp.mmbackup")
                    })
                )
                CardDivider()
                InfoRow(
                    icon = Icons.Rounded.FileDownload,
                    title = "Restore from backup",
                    value = "Add accounts and settings from a file",
                    onClick = if (state is BackupUiState.Working) null else ({
                        restorePicker.launch(arrayOf("*/*"))
                    })
                )
            }
            Text(
                text = "Tip: keep the file somewhere that survives uninstalling, like cloud storage. " +
                    "Uninstalling the app deletes everything stored inside it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ResultDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
