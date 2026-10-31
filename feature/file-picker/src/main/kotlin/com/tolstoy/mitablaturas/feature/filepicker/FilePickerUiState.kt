package com.tolstoy.mitablaturas.feature.filepicker

import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
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
 * @property entries entradas que cumplen [filter], de la más reciente a la más antigua.
 * @property hasEntries `true` si la biblioteca tiene alguna entrada, aunque el filtro la oculte.
 */
data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hasEntries: Boolean = false,
)
