package com.tolstoy.mitablaturas

import android.graphics.Color
import android.os.Bundle
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
        // Mantiene la pantalla de inicio hasta leer el diseño guardado (suele ser < 50 ms).
        installSplashScreen().setKeepOnScreenCondition { viewModel.design.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    MiTablaturasRoot()
                }
            }
        }
    }
}
