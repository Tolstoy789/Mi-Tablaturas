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
    val isCountInOn: Boolean = false,
    /** Compases del bucle A-B desde 1 (0 = sin marcar). */
    val loopStartBar: Int = 0,
    val loopEndBar: Int = 0,
    /** Sube la velocidad un 5 % en cada vuelta hasta [speedTrainerTarget]. */
    val isSpeedTrainerOn: Boolean = false,
    val speedTrainerTarget: Float = 1f,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val tempo: Int = 0,
    val error: TabViewerError? = null,
) {
    val displayTitle: String get() = title.ifBlank { fileName.substringBeforeLast('.') }

    val loopRange: LoopRangeState get() = when {
        loopStartBar > 0 && loopEndBar > 0 -> LoopRangeState.ACTIVE
        loopStartBar > 0 -> LoopRangeState.START_MARKED
        else -> LoopRangeState.NONE
    }
}

/** Pasos del bucle A-B: sin bucle, punto A marcado, o repitiendo de A a B. */
enum class LoopRangeState { NONE, START_MARKED, ACTIVE }

/** Envoltorio con identidad para no comparar arrays byte a byte en cada recomposición. */
class ScoreBytes(val bytes: ByteArray)

sealed interface TabViewerError {
    /** No se pudo leer el archivo desde el almacenamiento. */
    data object FileUnreadable : TabViewerError
    /** Otra app compartió un archivo que no es una tablatura compatible. */
    data object Unsupported : TabViewerError
    /** El archivo no es un formato interactivo (p. ej. un PDF). */
    data object NotInteractive : TabViewerError
    /** AlphaTab no pudo interpretar el archivo, o falló el audio. */
    data class Engine(val detail: String) : TabViewerError
}
