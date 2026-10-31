package com.tolstoy.mitablaturas.feature.tabviewer.bridge

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PlayerCommandTest {

    @Test
    fun `genera llamadas protegidas a window tab`() {
        assertEquals("window.tab && window.tab.playPause();", PlayerCommand.PlayPause.toJavaScript())
        assertEquals("window.tab && window.tab.selectTrack(2);", PlayerCommand.SelectTrack(2).toJavaScript())
        assertEquals("window.tab && window.tab.setLooping(true);", PlayerCommand.SetLooping(true).toJavaScript())
    }

    @Test
    fun `la velocidad usa punto decimal aunque el idioma use coma`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("window.tab && window.tab.setSpeed(0.75);", PlayerCommand.SetSpeed(0.75f).toJavaScript())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `separa los nombres de pista e ignora vacios`() {
        assertEquals(
            listOf("Guitarra 1", "Bajo"),
            AlphaTabBridge.parseTrackNames("Guitarra 1\u001F Bajo \u001F"),
        )
        assertEquals(emptyList<String>(), AlphaTabBridge.parseTrackNames(null))
    }
}
