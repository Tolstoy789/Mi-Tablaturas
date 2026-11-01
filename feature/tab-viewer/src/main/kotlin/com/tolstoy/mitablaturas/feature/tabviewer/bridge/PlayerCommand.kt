package com.tolstoy.mitablaturas.feature.tabviewer.bridge

import java.util.Locale

/** Órdenes de Kotlin hacia AlphaTab; cada una se traduce a una llamada a `window.tab`. */
sealed interface PlayerCommand {
    data object PlayPause : PlayerCommand
    data object Pause : PlayerCommand
    data object Stop : PlayerCommand
    data class SelectTrack(val index: Int) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
    data class SetLooping(val enabled: Boolean) : PlayerCommand
    data class SetMetronome(val enabled: Boolean) : PlayerCommand
    /** Mueve el cursor a [positionMs] y desplaza la partitura hasta él. */
    data class Seek(val positionMs: Long) : PlayerCommand
    data class SetCountIn(val enabled: Boolean) : PlayerCommand
    /** Marca el punto A del bucle en el compás del cursor. */
    data object MarkLoopStart : PlayerCommand
    /** Marca el punto B en el compás del cursor y empieza a repetir de A a B. */
    data object MarkLoopEnd : PlayerCommand
    data object ClearLoopRange : PlayerCommand
    /** Desplaza la partitura una pantalla hacia abajo (1) o hacia arriba (-1). */
    data class ScrollPage(val direction: Int) : PlayerCommand
    /** Vuelve a dibujar la partitura con otro tamaño (escala de AlphaTab). */
    data class SetScale(val scale: Float) : PlayerCommand

    /** Script listo para `WebView.evaluateJavascript`. Si la página aún no cargó, no hace nada. */
    fun toJavaScript(): String {
        val call = when (this) {
            PlayPause -> "playPause()"
            Pause -> "pause()"
            Stop -> "stop()"
            is SelectTrack -> "selectTrack($index)"
            is SetSpeed -> "setSpeed(${String.format(Locale.US, "%.2f", speed)})"
            is SetLooping -> "setLooping($enabled)"
            is SetMetronome -> "setMetronome($enabled)"
            is Seek -> "seek($positionMs)"
            is SetCountIn -> "setCountIn($enabled)"
            MarkLoopStart -> "markLoopStart()"
            MarkLoopEnd -> "markLoopEnd()"
            ClearLoopRange -> "clearLoopRange()"
            is ScrollPage -> "scrollPage(${direction.coerceIn(-1, 1)})"
            is SetScale -> "setScale(${String.format(Locale.US, "%.2f", scale)})"
        }
        return "window.tab && window.tab.$call;"
    }
}
