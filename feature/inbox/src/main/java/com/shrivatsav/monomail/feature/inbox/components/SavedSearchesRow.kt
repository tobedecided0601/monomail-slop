package com.shrivatsav.monomail.feature.inbox.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shrivatsav.monomail.core.data.settings.SavedSearch
import com.shrivatsav.monomail.feature.inbox.SearchFilters

/**
 * One-tap shortcuts for searches the user saved. Tap a chip to run it, tap its ×
 * to remove it, or "Save search" to keep whatever is currently typed.
 */
@Composable
internal fun SavedSearchesRow(
    saved: List<SavedSearch>,
    currentFilters: SearchFilters,
    visible: Boolean,
    onApply: (SavedSearch) -> Unit,
    onSave: () -> Unit,
    onDelete: (SavedSearch) -> Unit,
    modifier: Modifier = Modifier
) {
    val canSave = currentFilters.query.isNotBlank() && saved.none {
        it.query.equals(currentFilters.query.trim(), ignoreCase = true) &&
            it.searchField == currentFilters.searchField.name &&
            it.hasAttachments == currentFilters.hasAttachments
    }
    AnimatedVisibility(
        visible = visible && (saved.isNotEmpty() || canSave),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canSave) {
                AssistChip(
                    onClick = onSave,
                    label = { Text("Save search") },
                    leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            saved.forEach { search ->
                InputChip(
                    selected = false,
                    onClick = { onApply(search) },
                    label = { Text(search.name, maxLines = 1) },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove saved search ${search.name}",
                            modifier = Modifier
                                .size(18.dp)
                                .clickableNoRipple { onDelete(search) }
                        )
                    }
                )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
