package com.tolstoy.mitablaturas.feature.tabviewer.bridge

/** Eventos que AlphaTab envía desde JavaScript (ver `assets/alphatab/bridge.js`). */
sealed interface AlphaTabEvent {
    data class ScoreLoaded(val title: String, val artist: String, val trackNames: List<String>) : AlphaTabEvent
    data object RenderFinished : AlphaTabEvent
    data object PlayerReady : AlphaTabEvent
    data class PlayerStateChanged(val isPlaying: Boolean) : AlphaTabEvent
    data class PositionChanged(val currentMs: Long, val endMs: Long, val tempo: Int) : AlphaTabEvent
    data class Error(val message: String) : AlphaTabEvent
}
