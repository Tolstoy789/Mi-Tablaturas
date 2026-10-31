package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.ui.graphics.Color
import com.tolstoy.mitablaturas.core.designsystem.ScoreSheetStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerUrlTest {

    @Test
    fun `pasa los colores de la hoja en la url`() {
        val style = ScoreSheetStyle(
            background = Color(0xFF10141C),
            accent = Color(0xFFF5A524),
            accentAlpha = 0.18f,
            staffLine = Color(0xFF3A4357),
            barSeparator = Color(0xFF5A6478),
            mainGlyph = Color(0xFFE9ECF2),
            secondaryGlyph = Color(0xFF8D96A8),
            scoreInfo = Color(0xFFE9ECF2),
            barNumber = Color(0xFFF5A524),
        )
        assertEquals(
            "https://appassets.androidplatform.net/assets/alphatab/viewer.html" +
                "?bg=10141C&accent=F5A524&accentAlpha=0.18&staff=3A4357&bar=5A6478" +
                "&glyph=E9ECF2&secondary=8D96A8&info=E9ECF2&barNumber=F5A524",
            viewerUrl(style),
        )
    }
}
