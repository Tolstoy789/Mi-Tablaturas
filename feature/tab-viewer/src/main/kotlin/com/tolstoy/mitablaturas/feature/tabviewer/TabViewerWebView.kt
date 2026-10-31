package com.tolstoy.mitablaturas.feature.tabviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
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
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabBridge
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayInputStream

private const val VIEWER_URL = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/alphatab/viewer.html"

/** URL del visor con el tema de colores que aplica `bridge.js` (atril, escenario o taller). */
internal fun viewerUrl(webTheme: String): String = "$VIEWER_URL?theme=${Uri.encode(webTheme)}"

/**
 * WebView con AlphaTab servido desde `assets/` mediante [WebViewAssetLoader] (origen https,
 * sin acceso a `file://`). La partitura se sirve en `/score/current` desde memoria.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TabViewerWebView(
    score: ScoreBytes,
    webTheme: String,
    background: Color,
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
            createWebView(context, bridge, background.toArgb(), scoreProvider = { currentScore.bytes }) { message ->
                currentOnEvent(AlphaTabEvent.Error(message))
            }.also {
                holder.webView = it
                it.loadUrl(viewerUrl(webTheme))
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
