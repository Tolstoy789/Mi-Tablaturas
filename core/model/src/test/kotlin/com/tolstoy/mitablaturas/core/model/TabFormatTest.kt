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
    private fun header(text: String, lengthPrefix: Boolean = false): ByteArray {
        val body = text.toByteArray(Charsets.ISO_8859_1)
        return if (lengthPrefix) byteArrayOf(body.size.toByte()) + body else body
    }

    @Test
    fun `detecta Guitar Pro 3 a 5 por la cabecera`() {
        assertEquals(TabFormat.GUITAR_PRO_3, TabFormat.fromHeader(header("FICHIER GUITAR PRO v3.00", lengthPrefix = true)))
        assertEquals(TabFormat.GUITAR_PRO_4, TabFormat.fromHeader(header("FICHIER GUITAR PRO v4.06", lengthPrefix = true)))
        assertEquals(TabFormat.GUITAR_PRO_5, TabFormat.fromHeader(header("FICHIER GUITAR PRO v5.00", lengthPrefix = true)))
        assertEquals(TabFormat.GUITAR_PRO_5, TabFormat.fromHeader(header("CLIPBOARD GUITAR PRO v5.10", lengthPrefix = true)))
    }

    @Test
    fun `detecta GPX, GP y PDF por la cabecera`() {
        assertEquals(TabFormat.GUITAR_PRO_6, TabFormat.fromHeader(header("BCFZ\u0000\u0001")))
        assertEquals(TabFormat.GUITAR_PRO_6, TabFormat.fromHeader(header("BCFS")))
        assertEquals(TabFormat.GUITAR_PRO_7, TabFormat.fromHeader(header("PK\u0003\u0004\u0014")))
        assertEquals(TabFormat.PDF, TabFormat.fromHeader(header("%PDF-1.7")))
    }

    @Test
    fun `no reconoce cabeceras ajenas o incompletas`() {
        assertNull(TabFormat.fromHeader(ByteArray(0)))
        assertNull(TabFormat.fromHeader(header("ID3\u0004")))
        assertNull(TabFormat.fromHeader(header("FICHIER GUITAR PRO v1.04", lengthPrefix = true)))
        assertNull(TabFormat.fromHeader(header("FICHIER GUITAR PRO v5.00")))
    }
}
