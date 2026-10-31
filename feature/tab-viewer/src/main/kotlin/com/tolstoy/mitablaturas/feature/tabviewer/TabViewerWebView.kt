package com.tolstoy.mitablaturas.feature.tabviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.tolstoy.mitablaturas.core.designsystem.ScoreSheetStyle
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabBridge
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayInputStream
import java.util.Locale

private const val LOG_TAG = "AlphaTab"
private const val VIEWER_URL = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/alphatab/viewer.html"

/** URL del visor con los colores de la hoja, que `bridge.js` aplica a AlphaTab. */
internal fun viewerUrl(style: ScoreSheetStyle): String {
    val params = linkedMapOf(
        "bg" to style.background.hex(),
        "accent" to style.accent.hex(),
        "accentAlpha" to String.format(Locale.US, "%.2f", style.accentAlpha),
        "staff" to style.staffLine.hex(),
        "bar" to style.barSeparator.hex(),
        "glyph" to style.mainGlyph.hex(),
        "secondary" to style.secondaryGlyph.hex(),
        "info" to style.scoreInfo.hex(),
        "barNumber" to style.barNumber.hex(),
    )
    return VIEWER_URL + params.entries.joinToString("&", prefix = "?") { (key, value) -> "$key=$value" }
}

/** `RRGGBB` sin almohadilla ni transparencia. */
private fun Color.hex(): String = String.format(Locale.US, "%06X", toArgb() and 0xFFFFFF)

/**
 * WebView con AlphaTab servido desde `assets/` mediante [WebViewAssetLoader] (origen https,
 * sin acceso a `file://`). La partitura se sirve en `/score/current` desde memoria.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TabViewerWebView(
    score: ScoreBytes,
    sheetStyle: ScoreSheetStyle,
    commands: Flow<PlayerCommand>,
    onEvent: (AlphaTabEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentScore by rememberUpdatedState(score)
    val bridge = remember { AlphaTabBridge { event -> currentOnEvent(event) } }

    val holder = remember { WebViewHolder() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            createWebView(context, bridge, sheetStyle.background.toArgb(), scoreProvider = { currentScore.bytes }) { message ->
                currentOnEvent(AlphaTabEvent.Error(message))
            }.also {
                holder.webView = it
                it.loadUrl(viewerUrl(sheetStyle))
            }
        },
        onRelease = { webView ->
            holder.webView = null
            webView.stopLoading()
            webView.removeJavascriptInterface(AlphaTabBridge.JS_NAME)
            webView.destroy()
        },
    )

    LaunchedEffect(commands) {
        commands.collect { command ->
            holder.webView?.evaluateJavascript(command.toJavaScript(), null)
        }
    }
}

/** Referencia estable al WebView creado por `AndroidView`; se limpia al liberarlo. */
private class WebViewHolder {
    var webView: WebView? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    bridge: AlphaTabBridge,
    backgroundColor: Int,
    scoreProvider: () -> ByteArray,
    onLoadError: (String) -> Unit,
): WebView {
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    WebView.setWebContentsDebuggingEnabled(isDebuggable)

    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler("/score/") { path ->
            if (path == "current") {
                WebResourceResponse("application/octet-stream", null, ByteArrayInputStream(scoreProvider()))
            } else {
                null
            }
        }
        .build()

    return WebView(context).apply {
        // Sin MATCH_PARENT, AndroidView deja el WebView en WRAP_CONTENT y el WebView toma la
        // altura de su contenido: la página mide 0 px de alto y la partitura no se ve, aunque
        // AlphaTab la dibuje y suene.
        // CONTROL: sin layoutParams
        setBackgroundColor(backgroundColor)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            builtInZoomControls = false
        }
        addJavascriptInterface(bridge, AlphaTabBridge.JS_NAME)
        if (isDebuggable) {
            // En debug, la consola de la página (avisos de AlphaTab incluidos) va a Logcat.
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d(LOG_TAG, "${message.messageLevel()}: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                    return true
                }
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) onLoadError(error.description?.toString().orEmpty())
            }

            // Ningún enlace debe sacar al usuario del visor.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != WebViewAssetLoader.DEFAULT_DOMAIN
        }
    }
}
