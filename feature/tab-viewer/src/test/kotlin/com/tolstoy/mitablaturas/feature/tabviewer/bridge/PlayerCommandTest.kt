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
        assertEquals("window.tab && window.tab.seek(42000);", PlayerCommand.Seek(42_000).toJavaScript())
        assertEquals("window.tab && window.tab.setCountIn(true);", PlayerCommand.SetCountIn(true).toJavaScript())
        assertEquals("window.tab && window.tab.markLoopStart();", PlayerCommand.MarkLoopStart.toJavaScript())
        assertEquals("window.tab && window.tab.markLoopEnd();", PlayerCommand.MarkLoopEnd.toJavaScript())
        assertEquals("window.tab && window.tab.clearLoopRange();", PlayerCommand.ClearLoopRange.toJavaScript())
        assertEquals("window.tab && window.tab.scrollPage(-1);", PlayerCommand.ScrollPage(-5).toJavaScript())
        assertEquals("window.tab && window.tab.setTabOnly(true);", PlayerCommand.SetTabOnly(true).toJavaScript())
        assertEquals("window.tab && window.tab.setTrackMute(1,true);", PlayerCommand.SetTrackMute(1, true).toJavaScript())
        assertEquals("window.tab && window.tab.setTrackSolo(0,false);", PlayerCommand.SetTrackSolo(0, false).toJavaScript())
        assertEquals("window.tab && window.tab.setTransposition(-2);", PlayerCommand.SetTransposition(-2).toJavaScript())
        assertEquals("window.tab && window.tab.jumpToBar(46);", PlayerCommand.JumpToBar(46).toJavaScript())
        assertEquals("window.tab && window.tab.loopBars(46,69);", PlayerCommand.LoopBars(46, 69).toJavaScript())
    }

    @Test
    fun `la velocidad usa punto decimal aunque el idioma use coma`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("window.tab && window.tab.setSpeed(0.75);", PlayerCommand.SetSpeed(0.75f).toJavaScript())
            assertEquals("window.tab && window.tab.setScale(1.10);", PlayerCommand.SetScale(1.1f).toJavaScript())
            assertEquals("window.tab && window.tab.setTrackVolume(2,0.50);", PlayerCommand.SetTrackVolume(2, 0.5f).toJavaScript())
            assertEquals("window.tab && window.tab.setMetronomeVolume(0.35);", PlayerCommand.SetMetronomeVolume(0.35f).toJavaScript())
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

    @Test
    fun `cada seccion termina donde empieza la siguiente`() {
        assertEquals(
            listOf(ScoreSection("Intro", 1, 45), ScoreSection("Chorus", 46, 69), ScoreSection("Final Chorus", 70, 96)),
            AlphaTabBridge.parseSections("  Intro\u001FChorus\u001FFinal Chorus", "1,46,70", 96),
        )
        // Inicios repetidos, fuera de rango o sin número se descartan, y se ordenan.
        assertEquals(
            listOf(ScoreSection("B", 3, 9), ScoreSection("", 10, 10)),
            AlphaTabBridge.parseSections("Y\u001FB\u001F\u001FX", "x,3,10,99", 10),
        )
        assertEquals(emptyList<ScoreSection>(), AlphaTabBridge.parseSections("", "", 12))
    }
}
