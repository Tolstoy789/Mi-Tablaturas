package com.tolstoy.mitablaturas.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// Dirección de diseño «Taller»: Material You expresivo, violeta como color de marca
// y un color por pista (ver TrackColors).

private val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = White,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    secondary = Green40,
    onSecondary = White,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Orange40,
    onTertiary = White,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange10,
    background = Lavender98,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Lavender92,
    onSurfaceVariant = InkMuted,
)

private val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    secondary = Green80,
    onSecondary = Green10,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = Orange80,
    onTertiary = Orange10,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    background = Night,
    onBackground = Lavender92,
    surface = NightSurface,
    onSurface = Lavender92,
    surfaceVariant = NightVariant,
    onSurfaceVariant = Lavender70,
)

private val TallerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/**
 * @param dynamicColor usa el color del fondo de pantalla en Android 12+. Desactivado por
 * defecto para mantener la identidad violeta de la app.
 */
@Composable
fun MiTablaturasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, shapes = TallerShapes, content = content)
}
