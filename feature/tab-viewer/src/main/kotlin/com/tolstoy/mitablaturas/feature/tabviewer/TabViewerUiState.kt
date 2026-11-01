package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.runtime.Immutable
import com.tolstoy.mitablaturas.core.model.ScoreScale
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.ScoreSection

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
    /** URI de un PDF que debe abrirse en el visor de PDF en lugar de aquí. */
    val openAsPdf: String? = null,
    /** Pantalla completa: la partitura ocupa todo y se ocultan cabecera, herramientas y barras del sistema. */
    val isFocusMode: Boolean = false,
    /** Tamaño de la partitura; ver [ScoreScale]. */
    val scoreScale: Float = ScoreScale.DEFAULT,
    /** Solo la tablatura, sin pentagrama (preferencia general de la app). */
    val isTabOnly: Boolean = false,
    /** Silencio, solo y volumen de cada pista, en el orden de [trackNames]. */
    val trackMix: List<TrackMix> = emptyList(),
    /** Volumen del metrónomo cuando está encendido, de 0 a 1. */
    val metronomeVolume: Float = 1f,
    /** Semitonos que se sube (positivo) o se baja el sonido; la partitura no cambia. */
    val transposition: Int = 0,
    /** Secciones que marca el archivo (Intro, Estribillo…). */
    val sections: List<ScoreSection> = emptyList(),
    /** Tras saltar a una sección se recuerda que una pulsación larga la repite. */
    val showSectionHint: Boolean = false,
) {
    val displayTitle: String get() = title.ifBlank { fileName.substringBeforeLast('.') }

    /** Los botones que vuelven a dibujar (tamaño, solo tablatura) esperan a que termine el dibujo anterior. */
    val canRedraw: Boolean get() = score != null && error == null && !isRendering

    /** Los controles de sonido actúan sobre el reproductor, que debe estar listo. */
    val canControlPlayer: Boolean get() = isPlayerReady && error == null

    val loopRange: LoopRangeState get() = when {
        loopStartBar > 0 && loopEndBar > 0 -> LoopRangeState.ACTIVE
        loopStartBar > 0 -> LoopRangeState.START_MARKED
        else -> LoopRangeState.NONE
    }
}

/** Mezcla de una pista: silenciada, sola (solo suenan las pistas en solo) y volumen de 0 a 1. */
@Immutable
data class TrackMix(val isMuted: Boolean = false, val isSolo: Boolean = false, val volume: Float = 1f)

/** Pasos del bucle A-B: sin bucle, punto A marcado, o repitiendo de A a B. */
enum class LoopRangeState { NONE, START_MARKED, ACTIVE }

/** Envoltorio con identidad para no comparar arrays byte a byte en cada recomposición. */
class ScoreBytes(val bytes: ByteArray)

sealed interface TabViewerError {
    /** No se pudo leer el archivo desde el almacenamiento. */
    data object FileUnreadable : TabViewerError
    /** Otra app compartió un archivo que no es una tablatura compatible. */
    data object Unsupported : TabViewerError
    /** AlphaTab no pudo interpretar el archivo, o falló el audio. */
    data class Engine(val detail: String) : TabViewerError
}
