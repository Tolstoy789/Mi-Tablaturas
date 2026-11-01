package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Interruptor «TAB»: solo la tablatura. Relleno cuando está activo; TalkBack lo lee como
 * «Ver solo la tablatura» y anuncia si está activado.
 */
@Composable
internal fun TabOnlyToggle(checked: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.tab_viewer_tab_only)
    val colors = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.38f
    Surface(
        checked = checked,
        onCheckedChange = { onToggle() },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (checked) colors.onSurface.copy(alpha = alpha) else Color.Transparent,
        contentColor = (if (checked) colors.surface else colors.onSurfaceVariant).copy(alpha = if (checked) 1f else alpha),
        border = if (checked) null else BorderStroke(1.dp, colors.outline.copy(alpha = alpha)),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics {}) {
            Text(
                text = stringResource(R.string.tab_viewer_tab_symbol),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Altavoz con dos ondas, dibujado a mano como [FullscreenIcon]. */
@Composable
internal fun SpeakerIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(20.dp)) {
        val side = size.minDimension
        val body = Path().apply {
            moveTo(side * 0.08f, side * 0.38f)
            lineTo(side * 0.28f, side * 0.38f)
            lineTo(side * 0.52f, side * 0.16f)
            lineTo(side * 0.52f, side * 0.84f)
            lineTo(side * 0.28f, side * 0.62f)
            lineTo(side * 0.08f, side * 0.62f)
            close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Dos ondas con centro en el frente del altavoz.
        val center = Offset(side * 0.52f, side * 0.5f)
        listOf(0.18f, 0.34f).forEach { radius ->
            val r = side * radius
            drawArc(
                color = color,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(2 * r, 2 * r),
                style = stroke,
            )
        }
    }
}
