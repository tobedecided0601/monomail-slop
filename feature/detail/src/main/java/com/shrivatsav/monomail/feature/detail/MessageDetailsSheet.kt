package com.shrivatsav.monomail.feature.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shrivatsav.monomail.data.model.Email
import com.shrivatsav.monomail.ui.components.AvatarCircle
import com.shrivatsav.monomail.ui.components.avatarUrlsForEmail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One person in a From/To/Cc/Bcc header. */
internal data class MailAddress(val name: String, val address: String)

/** Splits a header like `Ann <a@x.com>, "Doe, Bob" <b@y.org>, c@z.net` into people. */
internal fun parseAddressList(raw: String): List<MailAddress> {
    if (raw.isBlank()) return emptyList()
    return raw.split(Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)"""))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { part ->
            val angle = Regex("""^(.*?)<([^>]+)>\s*$""").find(part)
            if (angle != null) {
                MailAddress(angle.groupValues[1].trim().trim('"').trim(), angle.groupValues[2].trim())
            } else {
                MailAddress("", part.trim('"', ' '))
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageDetailsSheet(email: Email, use24HourTime: Boolean, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sender = remember(email.from, email.fromEmail) {
        val parsed = parseAddressList(email.from).firstOrNull()
        MailAddress(parsed?.name.orEmpty(), email.fromEmail.ifBlank { parsed?.address.orEmpty() })
    }
    val domain = sender.address.substringAfterLast('@', "")
    val to = remember(email.to) { parseAddressList(email.to) }
    val cc = remember(email.cc) { parseAddressList(email.cc) }
    val bcc = remember(email.bcc) { parseAddressList(email.bcc) }
    val pattern = if (use24HourTime) "EEEE, d MMMM yyyy, HH:mm:ss z" else "EEEE, d MMMM yyyy, h:mm:ss a z"
    val fullDate = remember(email.date, use24HourTime) {
        if (email.date == 0L) "" else SimpleDateFormat(pattern, Locale.getDefault()).format(Date(email.date))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(
                    photoUrl = avatarUrlsForEmail(sender.address).domainLogo,
                    fallbackPhotoUrl = avatarUrlsForEmail(sender.address).gravatar,
                    displayName = sender.name.ifBlank { sender.address },
                    size = 48.dp,
                    textStyle = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sender.name.ifBlank { sender.address },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (sender.name.isNotBlank()) {
                        Text(
                            sender.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                CopyButton(sender.address, "Copy address")
            }

            DetailField("Subject", email.subject.ifBlank { "(no subject)" })
            if (fullDate.isNotEmpty()) DetailField("Sent", fullDate)
            if (domain.isNotEmpty()) DetailField("Sender domain", domain)
            AddressField("From", listOf(sender))
            AddressField("To", to.ifEmpty { listOf(MailAddress("", email.to)).filter { it.address.isNotBlank() } })
            AddressField("Cc", cc)
            AddressField("Bcc", bcc)
            if (email.attachments.isNotEmpty()) {
                DetailField(
                    "Attachments (${email.attachments.size})",
                    email.attachments.joinToString("\n") { it.name }
                )
            }
            DetailField("Message ID", email.id, copyable = true)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AddressField(label: String, people: List<MailAddress>) {
    if (people.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        people.forEach { person ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (person.name.isNotBlank()) {
                        Text(person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            person.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(person.address, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                CopyButton(person.address, "Copy address")
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String, copyable: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FieldLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (copyable) CopyButton(value, "Copy")
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

@Composable
private fun CopyButton(value: String, description: String) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("mail", value))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
