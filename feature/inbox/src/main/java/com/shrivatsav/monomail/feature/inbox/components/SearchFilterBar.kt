package com.shrivatsav.monomail.feature.inbox.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.shrivatsav.monomail.core.data.repository.SearchField
import com.shrivatsav.monomail.feature.inbox.SearchFilters

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterBar(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit,
    onQueryChanged: (String) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search filters",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse filters" else "Expand filters"
                )
            }
            AnimatedVisibility(visible = expanded) {
            Column {
            Text(
                text = "Search operators",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            Text(
                text = "Tap an operator to add it to your search.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "from:" to "Sender",
                    "to:" to "Recipient",
                    "cc:" to "Cc",
                    "bcc:" to "Bcc",
                    "subject:" to "Subject",
                    "body:" to "Body",
                    "has:attachment" to "Attachments",
                    "is:unread" to "Unread",
                    "is:starred" to "Starred",
                    "in:inbox" to "Inbox",
                    "after:" to "After date",
                    "before:" to "Before date",
                    "-term" to "Exclude"
                ).forEach { (operator, label) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val separator = if (filters.query.isBlank() || filters.query.endsWith(" ")) "" else " "
                            onQueryChanged(filters.query + separator + operator)
                        },
                        label = { Text(label) }
                    )
                }
            }
            // Field scope chips
            Text(
                text = "Search in",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField.entries.forEach { field ->
                    FilterChip(
                        selected = filters.searchField == field,
                        onClick = { onFiltersChanged(filters.copy(searchField = field)) },
                        label = { Text(field.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                }
            Spacer(Modifier.height(8.dp))

            // Date range chips
            Text(
                text = "Date range",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data class DatePreset(val label: String, val dateFrom: Long?, val dateTo: Long?)
                val datePresets = listOf(
                    DatePreset("Any time", null, null),
                    DatePreset("Today", todayStart(), todayEnd()),
                    DatePreset("7 days", sevenDaysAgo(), null),
                    DatePreset("30 days", thirtyDaysAgo(), null)
                )
                datePresets.forEach { preset ->
                    val selected = filters.dateFrom == preset.dateFrom && filters.dateTo == preset.dateTo
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onFiltersChanged(filters.copy(dateFrom = preset.dateFrom, dateTo = preset.dateTo))
                        },
                        label = { Text(preset.label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Attachment toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filters.hasAttachments,
                    onClick = { onFiltersChanged(filters.copy(hasAttachments = !filters.hasAttachments)) },
                    label = { Text("Has attachments") },
                    leadingIcon = if (filters.hasAttachments) {
                        { Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.padding(4.dp)) }
                    } else null
                )

                Spacer(Modifier.weight(1f))

                if (!filters.isEmpty) {
                    IconButton(onClick = { onFiltersChanged(SearchFilters()) }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear filters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
}
}

private fun todayStart(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun todayEnd(): Long = todayStart() + 86_400_000L

private fun sevenDaysAgo(): Long = todayStart() - 7 * 86_400_000L

private fun thirtyDaysAgo(): Long = todayStart() - 30 * 86_400_000L
