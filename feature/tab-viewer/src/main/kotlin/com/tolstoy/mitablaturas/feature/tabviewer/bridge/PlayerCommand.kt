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
        }
        return "window.tab && window.tab.$call;"
    }
}
