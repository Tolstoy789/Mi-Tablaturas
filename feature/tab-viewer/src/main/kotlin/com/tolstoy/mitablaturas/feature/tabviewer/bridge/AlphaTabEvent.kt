package com.tolstoy.mitablaturas.feature.tabviewer.bridge

/** Eventos que AlphaTab envía desde JavaScript (ver `assets/alphatab/bridge.js`). */
sealed interface AlphaTabEvent {
    data class ScoreLoaded(val title: String, val artist: String, val trackNames: List<String>) : AlphaTabEvent
    data object RenderFinished : AlphaTabEvent
    data object PlayerReady : AlphaTabEvent
    data class PlayerStateChanged(val isPlaying: Boolean) : AlphaTabEvent
    data class PositionChanged(val currentMs: Long, val endMs: Long, val tempo: Int) : AlphaTabEvent
    /** Terminó una vuelta con «Repetir» activo (de la canción o del bucle A-B). */
    data object LoopCompleted : AlphaTabEvent
    /** Compases del bucle A-B desde 1; `endBar` es 0 mientras solo está marcado A, y ambos 0 sin bucle. */
    data class LoopRangeChanged(val startBar: Int, val endBar: Int) : AlphaTabEvent
    /** Secciones que marca el archivo, en orden; vacía si no trae ninguna. */
    data class SectionsLoaded(val sections: List<ScoreSection>) : AlphaTabEvent
    data class Error(val message: String) : AlphaTabEvent
}

/** Sección marcada en el archivo (Intro, Estribillo…), con su primer y su último compás desde 1. */
data class ScoreSection(val name: String, val startBar: Int, val endBar: Int)
