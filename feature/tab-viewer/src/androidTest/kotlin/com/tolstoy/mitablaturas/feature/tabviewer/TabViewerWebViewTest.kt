package com.tolstoy.mitablaturas.feature.tabviewer

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tolstoy.mitablaturas.core.designsystem.LocalScoreSheetStyle
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Abre una tablatura en el WebView real, dentro de un diseño como el del visor, y comprueba
 * que la partitura se dibuja y queda a la vista (no solo que AlphaTab avise de que terminó).
 */
@RunWith(AndroidJUnit4::class)
class TabViewerWebViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun laPartituraSeDibujaYOcupaElVisor() {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open("escala.gp").use { it.readBytes() }
        val events = CopyOnWriteArrayList<AlphaTabEvent>()

        compose.setContent {
            Column(Modifier.fillMaxSize()) {
                Text("Cabecera", Modifier.height(80.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = LocalScoreSheetStyle.current.background,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        TabViewerWebView(
                            score = ScoreBytes(bytes),
                            sheetStyle = LocalScoreSheetStyle.current,
                            commands = emptyFlow(),
                            onEvent = { events += it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        compose.waitUntil(timeoutMillis = 90_000) { events.any { it is AlphaTabEvent.RenderFinished } }

        val webView = checkNotNull(findWebView(compose.activity.window.decorView)) { "No hay WebView" }
        var state = ViewerState(0, 0, 0)
        val visible = runCatching {
            compose.waitUntil(timeoutMillis = 20_000) {
                state = viewerState(webView)
                state.height > 0 && state.svgCount > 0
            }
        }.isSuccess
        val errors = events.filterIsInstance<AlphaTabEvent.Error>().map { it.message }
        assertTrue(
            "Visor ${state.width}x${state.height} px, ${state.svgCount} SVG, " +
                "WebView ${webView.width}x${webView.height} px, errores: $errors",
            visible,
        )
    }

    private data class ViewerState(val width: Int, val height: Int, val svgCount: Int)

    /** Lee el tamaño de `#viewport` y los SVG dibujados, desde el hilo principal. */
    private fun viewerState(webView: WebView): ViewerState {
        val latch = CountDownLatch(1)
        var raw = ""
        compose.runOnUiThread {
            webView.evaluateJavascript(
                "(function(){var v=document.getElementById('viewport');" +
                    "return v.clientWidth+','+v.clientHeight+','+document.querySelectorAll('#score svg').length;})()",
            ) { result ->
                raw = result.orEmpty().trim('"')
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.SECONDS)
        val parts = raw.split(',').mapNotNull { it.toIntOrNull() }
        return if (parts.size == 3) ViewerState(parts[0], parts[1], parts[2]) else ViewerState(0, 0, 0)
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }
}
