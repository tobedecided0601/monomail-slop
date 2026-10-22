package com.shrivatsav.monomail.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shrivatsav.monomail.util.AppLog

@Composable
internal fun LogsSettingsScreen(onBack: () -> Unit) {
    var logs by remember { mutableStateOf(AppLog.snapshot()) }
    ScrollableSettingsScaffold(
        title = "Logs",
        onBack = onBack,
        actions = {
            IconButton(onClick = { logs = AppLog.snapshot() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh logs")
            }
            IconButton(onClick = {
                AppLog.clear()
                logs = emptyList()
            }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear logs")
            }
        }
    ) {
        if (logs.isEmpty()) {
            Text(
                "No diagnostic logs yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                logs.asReversed().forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
