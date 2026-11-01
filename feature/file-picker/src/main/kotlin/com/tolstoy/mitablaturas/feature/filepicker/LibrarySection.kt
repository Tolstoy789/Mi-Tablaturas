package com.tolstoy.mitablaturas.feature.filepicker

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import com.tolstoy.mitablaturas.core.model.LibrarySort
import kotlin.math.roundToInt

/** Acciones de la biblioteca, agrupadas en una clase estable. */
data class LibraryActions(
    val onOpen: (LibraryEntry) -> Unit = {},
    val onFilterSelected: (LibraryFilter) -> Unit = {},
    val onFavoriteToggled: (LibraryEntry) -> Unit = {},
    val onRemove: (LibraryEntry) -> Unit = {},
    val onQueryChanged: (String) -> Unit = {},
    val onSortSelected: (LibrarySort) -> Unit = {},
)

/**
 * Cabecera con orden, búsqueda y filtros, y la lista de tablaturas. No muestra nada si la
 * biblioteca está vacía.
 *
 * @param query texto del campo de búsqueda, que guarda la pantalla.
 */
internal fun LazyListScope.librarySection(
    state: LibraryUiState,
    query: String,
    actions: LibraryActions,
    topSpacing: Boolean = false,
) {
    if (!state.hasEntries) return
    item(key = "library-header") {
        Column(
            modifier = Modifier.padding(top = if (topSpacing) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.showTools) SortMenu(state.sort, actions.onSortSelected)
            }
            if (state.showTools) SearchField(query, actions.onQueryChanged)
            FilterChips(state.filter, actions.onFilterSelected)
        }
    }
    if (state.entries.isEmpty()) {
        item(key = "library-empty") {
            val message = when {
                state.query.isNotBlank() -> stringResource(R.string.library_empty_search, state.query.trim())
                state.filter == LibraryFilter.FAVORITES -> stringResource(R.string.library_empty_favorites)
                else -> stringResource(R.string.library_empty_filter)
            }
            Text(
                text = message,
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

/** Botón «Orden: Recientes» con un menú de los tres órdenes. */
@Composable
private fun SortMenu(selected: LibrarySort, onSelected: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.library_sort_button, stringResource(selected.labelRes())))
            ChevronDown(Modifier.padding(start = 4.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySort.entries.forEach { sort ->
                val isSelected = sort == selected
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelected(sort)
                    },
                    trailingIcon = if (isSelected) {
                        { Text("✓", modifier = Modifier.clearAndSetSemantics {}) }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { this.selected = isSelected },
                )
            }
        }
    }
}

/** Busca por título, artista o nombre de archivo; la lista se filtra mientras se escribe. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val clearDescription = stringResource(R.string.library_search_clear)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.library_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { SearchIcon() },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.semantics { contentDescription = clearDescription },
                ) {
                    Text("✕", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = MaterialTheme.shapes.medium,
    )
}

/** Lupa dibujada a mano, como los demás iconos de la app. */
@Composable
private fun SearchIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val radius = size.minDimension * 0.3f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))
        drawLine(
            color = color,
            start = center + Offset(radius * 0.75f, radius * 0.75f),
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Flecha hacia abajo del botón de orden. */
@Composable
private fun ChevronDown(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(10.dp)) {
        val stroke = 1.8.dp.toPx()
        drawLine(color, Offset(0f, size.height * 0.3f), Offset(size.width / 2, size.height * 0.75f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width / 2, size.height * 0.75f), Offset(size.width, size.height * 0.3f), stroke, StrokeCap.Round)
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
                    text = entry.displayTitle,
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

/**
 * «Paco de Lucía · Guitar Pro · hace 2 horas · 75 %»: artista si se conoce, formato, última
 * apertura y velocidad si no es la normal.
 */
@Composable
private fun LibraryEntry.details(): String {
    val opened = DateUtils.getRelativeTimeSpanString(
        lastOpenedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val speed = practice.speed.takeIf { it != 1f }?.let { stringResource(R.string.library_speed, (it * 100).roundToInt()) }
    return listOfNotNull(artist.takeIf { it.isNotBlank() }, document.format.label(), opened, speed).joinToString(" · ")
}

private fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.RECENT -> R.string.library_sort_recent
    LibrarySort.NAME -> R.string.library_sort_name
    LibrarySort.MOST_OPENED -> R.string.library_sort_most_opened
}

private fun LibraryFilter.labelRes(): Int = when (this) {
    LibraryFilter.ALL -> R.string.library_filter_all
    LibraryFilter.GUITAR_PRO -> R.string.library_filter_guitar_pro
    LibraryFilter.PDF -> R.string.library_filter_pdf
    LibraryFilter.FAVORITES -> R.string.library_filter_favorites
}
