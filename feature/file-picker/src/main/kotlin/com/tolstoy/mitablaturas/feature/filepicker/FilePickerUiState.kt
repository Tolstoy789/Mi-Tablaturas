package com.tolstoy.mitablaturas.feature.filepicker

import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import com.tolstoy.mitablaturas.core.model.LibrarySort
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException

sealed interface FilePickerUiState {
    data object Idle : FilePickerUiState
    data object Loading : FilePickerUiState
    data class Ready(val document: TabDocument) : FilePickerUiState
    data class Error(val error: FilePickerError) : FilePickerUiState
}

/** Error listo para mostrarse; se traduce a texto en la UI. */
sealed interface FilePickerError {
    data class UnsupportedFormat(val fileName: String) : FilePickerError
    data class EmptyFile(val fileName: String) : FilePickerError
    data class TooLarge(val fileName: String, val maxBytes: Long) : FilePickerError
    data object Unreadable : FilePickerError
}

internal fun Throwable.toFilePickerError(): FilePickerError = when (this) {
    is TabFileException.UnsupportedFormat -> FilePickerError.UnsupportedFormat(fileName)
    is TabFileException.EmptyFile -> FilePickerError.EmptyFile(fileName)
    is TabFileException.TooLarge -> FilePickerError.TooLarge(fileName, maxBytes)
    else -> FilePickerError.Unreadable
}

/**
 * @property entries entradas que cumplen [filter] y la búsqueda [query], en el orden [sort].
 * @property totalCount canciones de la biblioteca, aunque el filtro o la búsqueda las oculten.
 */
data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.DEFAULT,
    val query: String = "",
    val totalCount: Int = 0,
) {
    /** `true` si la biblioteca tiene alguna entrada. */
    val hasEntries: Boolean get() = totalCount > 0

    /** Buscar y ordenar aparecen con varias canciones, o si ya hay algo escrito en la búsqueda. */
    val showTools: Boolean get() = totalCount >= TOOLS_MIN_ENTRIES || query.isNotBlank()

    companion object {
        const val TOOLS_MIN_ENTRIES = 4
    }
}
