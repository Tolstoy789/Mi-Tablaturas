package com.tolstoy.mitablaturas.core.designsystem

import androidx.compose.ui.graphics.Color
import com.tolstoy.mitablaturas.core.model.AppDesign

/** Colores representativos de cada diseño, para mostrarlos en el selector. */
val AppDesign.swatches: List<Color>
    get() = when (this) {
        AppDesign.ATRIL -> listOf(AtrilPalette.Paper, AtrilPalette.Ink, AtrilPalette.Cobalt)
        AppDesign.ESCENARIO -> listOf(EscenarioPalette.Background, EscenarioPalette.Surface, EscenarioPalette.Amber)
        AppDesign.TALLER -> listOf(TallerPalette.Lavender98, TallerPalette.Violet40, TallerPalette.Green40)
    }
