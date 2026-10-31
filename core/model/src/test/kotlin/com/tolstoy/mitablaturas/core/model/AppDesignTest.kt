package com.tolstoy.mitablaturas.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDesignTest {

    @Test
    fun `cada clave vuelve a su diseno`() {
        AppDesign.entries.forEach { assertEquals(it, AppDesign.fromKey(it.key)) }
    }

    @Test
    fun `una clave desconocida o nula usa Taller`() {
        assertEquals(AppDesign.TALLER, AppDesign.fromKey(null))
        assertEquals(AppDesign.TALLER, AppDesign.fromKey("neon"))
    }
}
