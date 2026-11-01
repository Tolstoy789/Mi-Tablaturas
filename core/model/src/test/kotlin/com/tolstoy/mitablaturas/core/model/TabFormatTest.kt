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
    fun `detecta MusicXML comprimido y sin comprimir`() {
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromFileName("cancion.musicxml"))
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromFileName("cancion.MXL"))
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromFileName("cancion.xml"))
        assertTrue(TabFormat.MUSIC_XML.isInteractive)
        assertFalse(TabFormat.MUSIC_XML.isGuitarPro)
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

    /** Cabecera local de un ZIP cuyo primer archivo se llama [name]. */
    private fun zip(name: String): ByteArray {
        val local = ByteArray(30)
        "PK\u0003\u0004".toByteArray(Charsets.ISO_8859_1).copyInto(local)
        local[26] = name.length.toByte()
        return local + name.toByteArray(Charsets.ISO_8859_1) + ByteArray(8)
    }

    @Test
    fun `distingue un MXL de un Guitar Pro 7 por el primer archivo del ZIP`() {
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromHeader(zip("mimetype")))
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromHeader(zip("META-INF/container.xml")))
        assertEquals(TabFormat.GUITAR_PRO_7, TabFormat.fromHeader(zip("VERSION")))
        assertEquals(TabFormat.GUITAR_PRO_7, TabFormat.fromHeader(zip("Content/score.gpif")))
        // Cabecera cortada antes del nombre: un ZIP sin más datos se trata como Guitar Pro 7.
        assertEquals(TabFormat.GUITAR_PRO_7, TabFormat.fromHeader(zip("mimetype").copyOf(20)))
    }

    @Test
    fun `detecta MusicXML sin comprimir por su raiz`() {
        val declaration = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
        val doctype = "<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" " +
            "\"http://www.musicxml.org/dtds/partwise.dtd\">\n"
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromHeader((declaration + doctype).toByteArray()))
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromHeader("\uFEFF<score-timewise version=\"3.1\">".toByteArray()))
        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "<score-partwise>".toByteArray(Charsets.UTF_16LE)
        assertEquals(TabFormat.MUSIC_XML, TabFormat.fromHeader(utf16))
        // Otro XML cualquiera no es una partitura.
        assertNull(TabFormat.fromHeader("$declaration<svg xmlns=\"http://www.w3.org/2000/svg\">".toByteArray()))
    }

    @Test
    fun `no reconoce cabeceras ajenas o incompletas`() {
        assertNull(TabFormat.fromHeader(ByteArray(0)))
        assertNull(TabFormat.fromHeader(header("ID3\u0004")))
        assertNull(TabFormat.fromHeader(header("FICHIER GUITAR PRO v1.04", lengthPrefix = true)))
        assertNull(TabFormat.fromHeader(header("FICHIER GUITAR PRO v5.00")))
    }
}
