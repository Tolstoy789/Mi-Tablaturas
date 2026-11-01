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
    /** Muestra solo la tablatura ([enabled]) o los pentagramas que trae el archivo. */
    data class SetTabOnly(val enabled: Boolean) : PlayerCommand
    data class SetTrackMute(val index: Int, val muted: Boolean) : PlayerCommand
    data class SetTrackSolo(val index: Int, val solo: Boolean) : PlayerCommand
    /** Volumen de la pista, de 0 a 1. */
    data class SetTrackVolume(val index: Int, val volume: Float) : PlayerCommand
    /** Volumen del metrónomo cuando está encendido, de 0 a 1. */
    data class SetMetronomeVolume(val volume: Float) : PlayerCommand
    /** Sube (positivo) o baja el sonido en semitonos, sin cambiar la partitura. */
    data class SetTransposition(val semitones: Int) : PlayerCommand
    /** Mueve el cursor al compás [bar], contado desde 1. */
    data class JumpToBar(val bar: Int) : PlayerCommand
    /** Repite los compases [firstBar] a [lastBar], contados desde 1. */
    data class LoopBars(val firstBar: Int, val lastBar: Int) : PlayerCommand

    /** Script listo para `WebView.evaluateJavascript`. Si la página aún no cargó, no hace nada. */
    fun toJavaScript(): String {
        val call = when (this) {
            PlayPause -> "playPause()"
            Pause -> "pause()"
            Stop -> "stop()"
            is SelectTrack -> "selectTrack($index)"
            is SetSpeed -> "setSpeed(${decimal(speed)})"
            is SetLooping -> "setLooping($enabled)"
            is SetMetronome -> "setMetronome($enabled)"
            is Seek -> "seek($positionMs)"
            is SetCountIn -> "setCountIn($enabled)"
            MarkLoopStart -> "markLoopStart()"
            MarkLoopEnd -> "markLoopEnd()"
            ClearLoopRange -> "clearLoopRange()"
            is ScrollPage -> "scrollPage(${direction.coerceIn(-1, 1)})"
            is SetScale -> "setScale(${decimal(scale)})"
            is SetTabOnly -> "setTabOnly($enabled)"
            is SetTrackMute -> "setTrackMute($index,$muted)"
            is SetTrackSolo -> "setTrackSolo($index,$solo)"
            is SetTrackVolume -> "setTrackVolume($index,${decimal(volume)})"
            is SetMetronomeVolume -> "setMetronomeVolume(${decimal(volume)})"
            is SetTransposition -> "setTransposition($semitones)"
            is JumpToBar -> "jumpToBar($bar)"
            is LoopBars -> "loopBars($firstBar,$lastBar)"
        }
        return "window.tab && window.tab.$call;"
    }
}

/** Número con punto decimal, sea cual sea el idioma del teléfono. */
private fun decimal(value: Float): String = String.format(Locale.US, "%.2f", value)
