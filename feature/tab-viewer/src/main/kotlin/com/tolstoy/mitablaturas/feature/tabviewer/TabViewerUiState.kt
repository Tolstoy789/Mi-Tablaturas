package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.runtime.Immutable

@Immutable
data class TabViewerUiState(
    val fileName: String = "",
    val title: String = "",
    val artist: String = "",
    val trackNames: List<String> = emptyList(),
    val selectedTrack: Int = 0,
    /** Bytes del archivo; cuando no es `null` se crea el WebView. */
    val score: ScoreBytes? = null,
    val isRendering: Boolean = true,
    val isPlayerReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val isMetronomeOn: Boolean = false,
    val speed: Float = 1f,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val tempo: Int = 0,
    val error: TabViewerError? = null,
) {
    val displayTitle: String get() = title.ifBlank { fileName.substringBeforeLast('.') }
}

/** Envoltorio con identidad para no comparar arrays byte a byte en cada recomposición. */
class ScoreBytes(val bytes: ByteArray)

sealed interface TabViewerError {
    /** No se pudo leer el archivo desde el almacenamiento. */
    data object FileUnreadable : TabViewerError
    /** El archivo no es un formato interactivo (p. ej. un PDF). */
    data object NotInteractive : TabViewerError
    /** AlphaTab no pudo interpretar el archivo, o falló el audio. */
    data class Engine(val detail: String) : TabViewerError
}
