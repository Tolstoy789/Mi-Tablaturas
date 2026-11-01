package com.tolstoy.mitablaturas.feature.pdfviewer

import androidx.compose.runtime.Immutable

@Immutable
data class PdfViewerUiState(
    val fileName: String = "",
    val pageSizes: List<PageSize> = emptyList(),
    val isLoading: Boolean = true,
    val error: PdfViewerError? = null,
    /** Página en la que se dejó el PDF (desde 0), hasta que la pantalla vuelve a ella. */
    val pageToRestore: Int? = null,
) {
    val title: String get() = fileName.substringBeforeLast('.')
}

enum class PdfViewerError {
    /** No se pudo leer el archivo (borrado, sin permiso...). */
    UNREADABLE,
    /** No es un PDF. */
    UNSUPPORTED,
    /** Protegido con contraseña. */
    PROTECTED,
    /** Dañado o con un formato que Android no sabe dibujar. */
    DAMAGED,
}
