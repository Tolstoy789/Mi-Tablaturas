package com.tolstoy.mitablaturas.feature.tabviewer

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PedalActionTest {

    @Test
    fun `las teclas de pagina y multimedia funcionan siempre`() {
        assertEquals(PedalAction.NEXT_PAGE, pedalActionFor(KeyEvent.KEYCODE_PAGE_DOWN, viewerFocused = false))
        assertEquals(PedalAction.PREVIOUS_PAGE, pedalActionFor(KeyEvent.KEYCODE_PAGE_UP, viewerFocused = false))
        assertEquals(PedalAction.PLAY_PAUSE, pedalActionFor(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, viewerFocused = false))
    }

    @Test
    fun `flechas, espacio e intro solo sin un boton enfocado`() {
        assertEquals(PedalAction.NEXT_PAGE, pedalActionFor(KeyEvent.KEYCODE_DPAD_RIGHT, viewerFocused = true))
        assertEquals(PedalAction.PLAY_PAUSE, pedalActionFor(KeyEvent.KEYCODE_SPACE, viewerFocused = true))
        assertNull(pedalActionFor(KeyEvent.KEYCODE_DPAD_RIGHT, viewerFocused = false))
        assertNull(pedalActionFor(KeyEvent.KEYCODE_ENTER, viewerFocused = false))
    }

    @Test
    fun `otras teclas se ignoran`() {
        assertNull(pedalActionFor(KeyEvent.KEYCODE_A, viewerFocused = true))
    }
}
