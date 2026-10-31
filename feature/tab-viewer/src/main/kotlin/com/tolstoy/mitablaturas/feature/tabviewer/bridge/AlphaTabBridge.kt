package com.tolstoy.mitablaturas.feature.tabviewer.bridge

import android.webkit.JavascriptInterface

/**
 * Objeto expuesto a JavaScript como `window.AndroidBridge`.
 *
 * Los métodos se llaman desde un hilo del WebView (no el principal), por eso [onEvent]
 * debe ser seguro entre hilos (p. ej. `MutableStateFlow.update`).
 * Solo recibe tipos primitivos para no depender de un parser JSON.
 */
class AlphaTabBridge(private val onEvent: (AlphaTabEvent) -> Unit) {

    @JavascriptInterface
    fun onScoreLoaded(title: String?, artist: String?, trackNames: String?) {
        onEvent(
            AlphaTabEvent.ScoreLoaded(
                title = title.orEmpty(),
                artist = artist.orEmpty(),
                trackNames = parseTrackNames(trackNames),
            ),
        )
    }

    @JavascriptInterface
    fun onRenderFinished() = onEvent(AlphaTabEvent.RenderFinished)

    @JavascriptInterface
    fun onPlayerReady() = onEvent(AlphaTabEvent.PlayerReady)

    @JavascriptInterface
    fun onPlayerStateChanged(isPlaying: Boolean) = onEvent(AlphaTabEvent.PlayerStateChanged(isPlaying))

    @JavascriptInterface
    fun onPositionChanged(currentMs: Long, endMs: Long, tempo: Int) =
        onEvent(AlphaTabEvent.PositionChanged(currentMs.coerceAtLeast(0), endMs.coerceAtLeast(0), tempo))

    @JavascriptInterface
    fun onLoopCompleted() = onEvent(AlphaTabEvent.LoopCompleted)

    /** Compases del bucle A-B, contados desde 1; 0 si falta ese punto. */
    @JavascriptInterface
    fun onLoopRangeChanged(startBar: Int, endBar: Int) =
        onEvent(AlphaTabEvent.LoopRangeChanged(startBar.coerceAtLeast(0), endBar.coerceAtLeast(0)))

    @JavascriptInterface
    fun onError(message: String?) = onEvent(AlphaTabEvent.Error(message.orEmpty()))

    companion object {
        const val JS_NAME = "AndroidBridge"
        private const val TRACK_SEPARATOR = '\u001F'

        internal fun parseTrackNames(raw: String?): List<String> =
            raw.orEmpty().split(TRACK_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
