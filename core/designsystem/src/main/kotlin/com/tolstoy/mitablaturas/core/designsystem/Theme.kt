package com.tolstoy.mitablaturas.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tolstoy.mitablaturas.core.model.AppDesign

/** Colores de la hoja de la partitura y nombre del tema que entiende `bridge.js`. */
@Immutable
data class ScoreSheetStyle(
    val background: Color,
    val webTheme: String,
)

val LocalScoreSheetStyle = staticCompositionLocalOf {
    ScoreSheetStyle(background = White, webTheme = AppDesign.DEFAULT.key)
}

/** Indica si el diseño activo usa colores oscuros (para iconos de la barra de estado). */
@Composable
fun AppDesign.isDark(): Boolean = when (this) {
    AppDesign.ATRIL -> false
    AppDesign.ESCENARIO -> true
    AppDesign.TALLER -> isSystemInDarkTheme()
}

@Composable
fun MiTablaturasTheme(
    design: AppDesign = AppDesign.DEFAULT,
    content: @Composable () -> Unit,
) {
    val dark = design.isDark()
    val colorScheme = when (design) {
        AppDesign.ATRIL -> AtrilColors
        AppDesign.ESCENARIO -> EscenarioColors
        AppDesign.TALLER -> if (dark) TallerDarkColors else TallerLightColors
    }
    val sheet = when (design) {
        AppDesign.ESCENARIO -> ScoreSheetStyle(EscenarioPalette.Background, design.key)
        AppDesign.ATRIL -> ScoreSheetStyle(AtrilPalette.Paper, design.key)
        AppDesign.TALLER -> ScoreSheetStyle(White, design.key)
    }
    CompositionLocalProvider(LocalScoreSheetStyle provides sheet) {
        MaterialTheme(colorScheme = colorScheme, shapes = design.shapes(), content = content)
    }
}

private fun AppDesign.shapes(): Shapes = when (this) {
    // Atril: esquinas contenidas, como papel.
    AppDesign.ATRIL -> Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(14.dp))
    // Escenario: botones grandes y redondeados.
    AppDesign.ESCENARIO -> Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp))
    AppDesign.TALLER -> Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp))
}

private val AtrilColors: ColorScheme = with(AtrilPalette) {
    lightColorScheme(
        primary = Cobalt,
        onPrimary = White,
        primaryContainer = CobaltContainer,
        onPrimaryContainer = OnCobaltContainer,
        secondary = Slate,
        onSecondary = White,
        secondaryContainer = SlateContainer,
        onSecondaryContainer = Ink,
        tertiary = Crimson,
        onTertiary = White,
        tertiaryContainer = CrimsonContainer,
        onTertiaryContainer = OnCrimsonContainer,
        background = Paper,
        onBackground = Ink,
        surface = White,
        onSurface = Ink,
        surfaceVariant = Line,
        onSurfaceVariant = InkMuted,
        outline = Color(0xFFC9CED8),
    )
}

private val EscenarioColors: ColorScheme = with(EscenarioPalette) {
    darkColorScheme(
        primary = Amber,
        onPrimary = OnAmber,
        primaryContainer = AmberContainer,
        onPrimaryContainer = OnAmberContainer,
        secondary = Teal,
        onSecondary = Background,
        secondaryContainer = TealContainer,
        onSecondaryContainer = Teal,
        tertiary = Coral,
        onTertiary = Background,
        tertiaryContainer = CoralContainer,
        onTertiaryContainer = Coral,
        background = Background,
        onBackground = Text,
        surface = Surface,
        onSurface = Text,
        surfaceVariant = SurfaceHigh,
        onSurfaceVariant = TextMuted,
    )
}

private val TallerLightColors: ColorScheme = with(TallerPalette) {
    lightColorScheme(
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
}

private val TallerDarkColors: ColorScheme = with(TallerPalette) {
    darkColorScheme(
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
}
