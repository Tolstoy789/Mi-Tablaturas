package com.tolstoy.mitablaturas

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolstoy.mitablaturas.core.designsystem.MiTablaturasTheme
import com.tolstoy.mitablaturas.core.designsystem.isDark
import com.tolstoy.mitablaturas.ui.MiTablaturasRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Mantiene la pantalla de inicio hasta leer el diseño guardado y, en Android 12+,
        // hasta que termine la animación del icono (las cuerdas vibrando), solo en arranque en frío.
        val splashStart = SystemClock.uptimeMillis()
        val minSplashMs = if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SPLASH_ANIMATION_MS else 0L
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.design.value == null || SystemClock.uptimeMillis() - splashStart < minSplashMs
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Tras rotar o recrear la actividad, el archivo externo ya se abrió.
        if (savedInstanceState == null) handleExternalDocument(intent)
        setContent {
            val design by viewModel.design.collectAsStateWithLifecycle()
            design?.let { current ->
                val dark = current.isDark()
                // Iconos de la barra de estado claros u oscuros según el diseño, no según el sistema.
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                MiTablaturasTheme(design = current) {
                    val pendingDocument by viewModel.pendingDocument.collectAsStateWithLifecycle()
                    MiTablaturasRoot(
                        pendingDocument = pendingDocument,
                        onPendingDocumentOpened = viewModel::onPendingDocumentOpened,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalDocument(intent)
    }

    private fun handleExternalDocument(intent: Intent) {
        intent.externalDocumentUri()?.let { viewModel.onExternalDocument(it.toString()) }
    }

    private companion object {
        /** Debe coincidir con `windowSplashScreenAnimationDuration` en themes.xml. */
        const val SPLASH_ANIMATION_MS = 900L
    }
}
