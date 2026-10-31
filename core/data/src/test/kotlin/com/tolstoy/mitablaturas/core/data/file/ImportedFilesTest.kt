package com.tolstoy.mitablaturas.core.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportedFilesTest {

    @Test
    fun `conserva un nombre normal`() {
        assertEquals("Blues en La.gp5", ImportedFiles.safeName("Blues en La.gp5"))
    }

    @Test
    fun `no permite salir de la carpeta de copias`() {
        assertEquals("_.._etc_passwd", ImportedFiles.safeName("../../etc/passwd"))
    }

    @Test
    fun `sustituye caracteres no validos y nunca queda vacio`() {
        assertEquals("a_b_c_.gp", ImportedFiles.safeName("a:b*c?.gp"))
        assertEquals("tablatura", ImportedFiles.safeName("..."))
    }
}
