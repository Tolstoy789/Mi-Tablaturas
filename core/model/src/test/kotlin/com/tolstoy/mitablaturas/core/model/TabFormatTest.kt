package com.tolstoy.mitablaturas.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabFormatTest {

    @Test
    fun `detecta todos los formatos Guitar Pro`() {
        assertEquals(TabFormat.GUITAR_PRO_3, TabFormat.fromFileName("cancion.gp3"))
        assertEquals(TabFormat.GUITAR_PRO_4, TabFormat.fromFileName("cancion.gp4"))
        assertEquals(TabFormat.GUITAR_PRO_5, TabFormat.fromFileName("cancion.gp5"))
        assertEquals(TabFormat.GUITAR_PRO_6, TabFormat.fromFileName("cancion.gpx"))
        assertEquals(TabFormat.GUITAR_PRO_7, TabFormat.fromFileName("cancion.gp"))
    }

    @Test
    fun `ignora mayusculas y usa la ultima extension`() {
        assertEquals(TabFormat.GUITAR_PRO_5, TabFormat.fromFileName("Mi.Cancion.GP5"))
        assertEquals(TabFormat.PDF, TabFormat.fromFileName("partitura.final.PDF"))
    }

    @Test
    fun `devuelve null sin extension o con extension desconocida`() {
        assertNull(TabFormat.fromFileName("cancion"))
        assertNull(TabFormat.fromFileName("cancion."))
        assertNull(TabFormat.fromFileName("cancion.mp3"))
    }

    @Test
    fun `solo PDF es estatico`() {
        assertFalse(TabFormat.PDF.isInteractive)
        assertTrue(TabFormat.entries.filter { it != TabFormat.PDF }.all { it.isInteractive })
    }
}
