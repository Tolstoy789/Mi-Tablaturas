package com.tolstoy.mitablaturas.feature.tabviewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tolstoy.mitablaturas.core.model.ScoreScale
import kotlin.math.roundToInt

/**
 * Oculta las barras de estado y de navegación mientras está en pantalla. Al deslizar desde el
 * borde se ven un momento. Se vuelven a ocultar al regresar a la app.
 */
@Composable
internal fun HideSystemBarsEffect() {
    val view = LocalView.current
    LifecycleResumeEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onPauseOrDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Barra flotante de la pantalla completa: reproducir, tamaño de la partitura y salir. */
@Composable
internal fun FocusBar(uiState: TabViewerUiState, actions: TabViewerActions, modifier: Modifier = Modifier) {
    val canPlay = uiState.isPlayerReady && uiState.error == null
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            FocusBarButton(
                description = stringResource(
                    if (uiState.isPlaying) R.string.tab_viewer_pause_description else R.string.tab_viewer_play_description,
                ),
                enabled = canPlay,
                onClick = actions.onPlayPause,
            ) {
                SymbolLabel(stringResource(if (uiState.isPlaying) R.string.tab_viewer_pause_symbol else R.string.tab_viewer_play_symbol))
            }
            FocusBarButton(
                description = stringResource(R.string.tab_viewer_zoom_out),
                enabled = uiState.canZoom && uiState.scoreScale > ScoreScale.MIN,
                onClick = { actions.onZoom(-1) },
            ) { SymbolLabel(stringResource(R.string.tab_viewer_zoom_out_symbol)) }
            Text(
                text = stringResource(R.string.tab_viewer_zoom_level, (uiState.scoreScale * 100).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            FocusBarButton(
                description = stringResource(R.string.tab_viewer_zoom_in),
                enabled = uiState.canZoom && uiState.scoreScale < ScoreScale.MAX,
                onClick = { actions.onZoom(1) },
            ) { SymbolLabel(stringResource(R.string.tab_viewer_zoom_in_symbol)) }
            FocusBarButton(
                description = stringResource(R.string.tab_viewer_focus_exit),
                enabled = true,
                onClick = actions.onFocusModeToggled,
            ) { FullscreenIcon(expand = false) }
        }
    }
}

/** Botón redondo de 48 dp; TalkBack lee [description] en lugar del símbolo. */
@Composable
private fun FocusBarButton(description: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val contentColor = LocalContentColor.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics {}) { content() }
    }
}

@Composable
private fun SymbolLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

/**
 * Cuatro esquinas: hacia fuera para entrar en pantalla completa ([expand]) y hacia dentro
 * para salir. Dibujado a mano para no depender de una biblioteca de iconos.
 */
@Composable
internal fun FullscreenIcon(expand: Boolean, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        val side = size.minDimension
        val arm = side * 0.34f
        val edge = stroke / 2
        val corners = listOf(
            Offset(edge, edge) to Offset(1f, 1f),
            Offset(side - edge, edge) to Offset(-1f, 1f),
            Offset(edge, side - edge) to Offset(1f, -1f),
            Offset(side - edge, side - edge) to Offset(-1f, -1f),
        )
        corners.forEach { (corner, inward) ->
            // El codo de cada esquina está en el borde (expandir) o desplazado hacia el centro (salir).
            val elbow = if (expand) corner else corner + Offset(inward.x * arm, inward.y * arm)
            val armX = if (expand) Offset(inward.x * arm, 0f) else Offset(-inward.x * arm, 0f)
            val armY = if (expand) Offset(0f, inward.y * arm) else Offset(0f, -inward.y * arm)
            drawLine(color, elbow, elbow + armX, stroke, StrokeCap.Round)
            drawLine(color, elbow, elbow + armY, stroke, StrokeCap.Round)
        }
    }
}
