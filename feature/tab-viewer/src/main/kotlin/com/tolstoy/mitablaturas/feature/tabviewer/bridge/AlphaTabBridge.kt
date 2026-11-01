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

    /**
     * Secciones del archivo: nombres separados por U+001F y compás de inicio de cada una (desde 1)
     * separados por comas, en el mismo orden. [barCount] cierra la última.
     */
    @JavascriptInterface
    fun onSectionsLoaded(names: String?, startBars: String?, barCount: Int) =
        onEvent(AlphaTabEvent.SectionsLoaded(parseSections(names, startBars, barCount)))

    @JavascriptInterface
    fun onError(message: String?) = onEvent(AlphaTabEvent.Error(message.orEmpty()))

    companion object {
        const val JS_NAME = "AndroidBridge"
        private const val TRACK_SEPARATOR = '\u001F'

        internal fun parseTrackNames(raw: String?): List<String> =
            raw.orEmpty().split(TRACK_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

        /** Cada sección termina donde empieza la siguiente; se descartan inicios repetidos o fuera de rango. */
        internal fun parseSections(names: String?, startBars: String?, barCount: Int): List<ScoreSection> {
            val labels = names.orEmpty().split(TRACK_SEPARATOR)
            val starts = startBars.orEmpty().split(',').map { it.trim().toIntOrNull() }
            val valid = labels.zip(starts)
                .mapNotNull { (label, start) -> if (start != null && start in 1..barCount) label.trim() to start else null }
                .distinctBy { it.second }
                .sortedBy { it.second }
            return valid.mapIndexed { index, (label, start) ->
                val end = valid.getOrNull(index + 1)?.second?.minus(1) ?: barCount
                ScoreSection(name = label, startBar = start, endBar = end)
            }
        }
    }
}
