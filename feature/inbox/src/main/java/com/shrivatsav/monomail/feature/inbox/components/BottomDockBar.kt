package com.shrivatsav.monomail.feature.inbox.components

import com.shrivatsav.monomail.feature.inbox.*

import com.shrivatsav.monomail.model.InboxTab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.shrivatsav.monomail.ui.theme.cornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.shrivatsav.monomail.core.data.settings.DockConfig
import com.shrivatsav.monomail.core.data.settings.DockTabId
import com.shrivatsav.monomail.core.data.settings.AppSettings

@Composable
internal fun BottomDockBar(
    currentTab: InboxTab,
    dockConfig: DockConfig,
    unifiedInboxEnabled: Boolean,
    appSettings: AppSettings,
    onTabClick: (InboxTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val tabs = listOf(
        listOf(InboxTab.INBOX, InboxTab.SENT, InboxTab.ARCHIVED),
        listOf(InboxTab.STARRED, InboxTab.SNOOZED),
        listOf(InboxTab.TRASH, InboxTab.SPAM, InboxTab.DRAFTS)
    )
    val activeLabel = currentTab.displayLabel(unifiedInboxEnabled)
    Surface(
        shape = cornerShape(24.dp),
        color = if (expanded) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        onClick = { expanded = true },
        modifier = modifier.height(52.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(currentTab.tabIcon(unifiedInboxEnabled), contentDescription = null)
                Text(
                    activeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                    contentDescription = if (expanded) "Close folders" else "Open folders"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, (-8).dp)
            ) {
                tabs.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider()
                    group.forEach { tab ->
                        DropdownMenuItem(
                            text = { Text(tab.displayLabel(unifiedInboxEnabled)) },
                            leadingIcon = { Icon(tab.tabIcon(unifiedInboxEnabled), contentDescription = null) },
                            trailingIcon = if (tab == currentTab) {
                                { Icon(Icons.Rounded.Check, contentDescription = "Active") }
                            } else null,
                            modifier = if (tab == currentTab) {
                                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                            } else {
                                Modifier
                            },
                            onClick = {
                                onTabClick(tab)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun DockTabId.toInboxTab(): InboxTab = when (this) {
    DockTabId.UNIFIED -> InboxTab.UNIFIED
    DockTabId.INBOX -> InboxTab.INBOX
    DockTabId.SENT -> InboxTab.SENT
    DockTabId.ARCHIVED -> InboxTab.ARCHIVED
    DockTabId.SNOOZED -> InboxTab.SNOOZED
    DockTabId.STARRED -> InboxTab.STARRED
    DockTabId.TRASH -> InboxTab.TRASH
    DockTabId.SPAM -> InboxTab.SPAM
}

private fun InboxTab.displayLabel(unifiedInboxEnabled: Boolean): String = when (this) {
    InboxTab.INBOX, InboxTab.UNIFIED -> if (unifiedInboxEnabled) "All Mail" else "Inbox"
    InboxTab.SENT -> "Sent"
    InboxTab.ARCHIVED -> "Archive"
    InboxTab.STARRED -> "Starred"
    InboxTab.TRASH -> "Trash"
    InboxTab.SNOOZED -> "Snoozed"
    InboxTab.SPAM -> "Spam"
    InboxTab.DRAFTS -> "Drafts"
}

private fun InboxTab.tabIcon(unifiedInboxEnabled: Boolean): ImageVector = when (this) {
    InboxTab.INBOX, InboxTab.UNIFIED -> if (unifiedInboxEnabled) Icons.Rounded.Home else Icons.Rounded.Inbox
    InboxTab.SENT -> Icons.AutoMirrored.Rounded.Send
    InboxTab.ARCHIVED -> Icons.Rounded.Archive
    InboxTab.STARRED -> Icons.Rounded.Star
    InboxTab.TRASH -> Icons.Rounded.Delete
    InboxTab.SNOOZED -> Icons.Rounded.Schedule
    InboxTab.SPAM -> Icons.Rounded.Report
    InboxTab.DRAFTS -> Icons.Rounded.EditNote
}

internal fun DockTabId.icon(unifiedInboxEnabled: Boolean): ImageVector = when (this) {
    DockTabId.UNIFIED -> Icons.Rounded.Inbox
    DockTabId.INBOX -> if (unifiedInboxEnabled) Icons.Rounded.Home else Icons.Rounded.Inbox
    DockTabId.SENT -> Icons.AutoMirrored.Rounded.Send
    DockTabId.ARCHIVED -> Icons.Rounded.Archive
    DockTabId.SNOOZED -> Icons.Rounded.Schedule
    DockTabId.STARRED -> Icons.Rounded.Star
    DockTabId.TRASH -> Icons.Rounded.Delete
    DockTabId.SPAM -> Icons.Rounded.Report
}

internal fun DockTabId.label(unifiedInboxEnabled: Boolean): String = when (this) {
    DockTabId.UNIFIED -> "Unified"
    DockTabId.INBOX -> if (unifiedInboxEnabled) "All Mail" else "Inbox"
    DockTabId.SENT -> "Sent"
    DockTabId.ARCHIVED -> "Archived"
    DockTabId.SNOOZED -> "Snoozed"
    DockTabId.STARRED -> "Starred"
    DockTabId.TRASH -> "Trash"
    DockTabId.SPAM -> "Spam"
}
