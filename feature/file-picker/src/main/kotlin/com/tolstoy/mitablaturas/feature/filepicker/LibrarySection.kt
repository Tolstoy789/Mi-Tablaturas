package com.tolstoy.mitablaturas.feature.filepicker

import android.text.format.DateUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import kotlin.math.roundToInt

/** Acciones de la biblioteca, agrupadas en una clase estable. */
data class LibraryActions(
    val onOpen: (LibraryEntry) -> Unit = {},
    val onFilterSelected: (LibraryFilter) -> Unit = {},
    val onFavoriteToggled: (LibraryEntry) -> Unit = {},
    val onRemove: (LibraryEntry) -> Unit = {},
)

/** Cabecera con filtros y lista de tablaturas recientes. No muestra nada si la biblioteca está vacía. */
internal fun LazyListScope.librarySection(
    state: LibraryUiState,
    actions: LibraryActions,
    topSpacing: Boolean = false,
) {
    if (!state.hasEntries) return
    item(key = "library-header") {
        Column(
            modifier = Modifier.padding(top = if (topSpacing) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FilterChips(state.filter, actions.onFilterSelected)
        }
    }
    if (state.entries.isEmpty()) {
        item(key = "library-empty") {
            val message = if (state.filter == LibraryFilter.FAVORITES) {
                R.string.library_empty_favorites
            } else {
                R.string.library_empty_filter
            }
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
    items(state.entries, key = { it.document.uri }) { entry ->
        LibraryRow(entry, actions)
    }
}

@Composable
private fun FilterChips(selected: LibraryFilter, onSelected: (LibraryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(filter.labelRes())) },
            )
        }
    }
}

@Composable
private fun LibraryRow(entry: LibraryEntry, actions: LibraryActions) {
    val document = entry.document
    Surface(
        onClick = { actions.onOpen(entry) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormatBadge(document.format)
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.displayName.substringBeforeLast('.'),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.details(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FavoriteToggle(entry.isFavorite) { actions.onFavoriteToggled(entry) }
            RowMenu(onRemove = { actions.onRemove(entry) })
        }
    }
}

@Composable
private fun FavoriteToggle(isFavorite: Boolean, onToggle: () -> Unit) {
    val description = stringResource(if (isFavorite) R.string.library_favorite_remove else R.string.library_favorite_add)
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onToggle() },
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            text = if (isFavorite) "★" else "☆",
            style = MaterialTheme.typography.titleLarge,
            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowMenu(onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.library_more)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            Text("⋮", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_remove)) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}

/** «Guitar Pro · hace 2 horas · 75 %»: formato, última apertura y velocidad si no es la normal. */
@Composable
private fun LibraryEntry.details(): String {
    val opened = DateUtils.getRelativeTimeSpanString(
        lastOpenedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val speed = practice.speed.takeIf { it != 1f }?.let { stringResource(R.string.library_speed, (it * 100).roundToInt()) }
    return listOfNotNull(document.format.label(), opened, speed).joinToString(" · ")
}

private fun LibraryFilter.labelRes(): Int = when (this) {
    LibraryFilter.ALL -> R.string.library_filter_all
    LibraryFilter.GUITAR_PRO -> R.string.library_filter_guitar_pro
    LibraryFilter.PDF -> R.string.library_filter_pdf
    LibraryFilter.FAVORITES -> R.string.library_filter_favorites
}
