package com.tolstoy.mitablaturas.core.data.file

import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabDocumentValidatorTest {

    private val uri = "content://docs/1"

    @Test
    fun `acepta un gp5 valido`() {
        val doc = TabDocumentValidator.validate(uri, "Blues en La.gp5", 48_000)
        assertEquals(TabFormat.GUITAR_PRO_5, doc.format)
        assertEquals("Blues en La.gp5", doc.displayName)
        assertEquals(48_000L, doc.sizeBytes)
    }

    @Test
    fun `acepta tamano desconocido`() {
        val doc = TabDocumentValidator.validate(uri, "riff.gp", null)
        assertNull(doc.sizeBytes)
    }

    @Test(expected = TabFileException.UnsupportedFormat::class)
    fun `rechaza extensiones no soportadas`() {
        TabDocumentValidator.validate(uri, "audio.mp3", 1_000)
    }

    @Test(expected = TabFileException.EmptyFile::class)
    fun `rechaza archivos vacios`() {
        TabDocumentValidator.validate(uri, "vacio.gp5", 0)
    }

    @Test(expected = TabFileException.TooLarge::class)
    fun `rechaza archivos demasiado grandes`() {
        TabDocumentValidator.validate(uri, "enorme.pdf", TabDocumentValidator.MAX_SIZE_BYTES + 1)
    }
    @Test
    fun `usa el formato detectado cuando el nombre no tiene extension`() {
        val document = TabDocumentValidator.validate(uri, "1000123", 2048, TabFormat.GUITAR_PRO_5)
        assertEquals(TabFormat.GUITAR_PRO_5, document.format)
        assertEquals("1000123", document.displayName)
    }

    @Test(expected = TabFileException.UnsupportedFormat::class)
    fun `rechaza un archivo sin extension ni cabecera reconocible`() {
        TabDocumentValidator.validate(uri, "1000123", 2048, format = null)
    }
}
