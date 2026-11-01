package com.tolstoy.mitablaturas.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LibrarySortTest {

    private fun entry(
        fileName: String,
        openedAt: Long,
        title: String = "",
        artist: String = "",
        openCount: Int = 1,
    ) = LibraryEntry(
        document = TabDocument("content://$fileName", fileName, TabFormat.fromFileName(fileName)!!, 100),
        lastOpenedAt = openedAt,
        title = title,
        artist = artist,
        openCount = openCount,
    )

    private val solea = entry("solea.gp5", openedAt = 30, title = "Soleá por Bulerías", artist = "Paco de Lucía", openCount = 2)
    private val blues = entry("blues en la.gp", openedAt = 10, openCount = 7)
    private val arbol = entry("track01.gp5", openedAt = 20, title = "Árbol de la vida", artist = "Elías", openCount = 2)
    private val cancionero = entry("Cancionero.pdf", openedAt = 40)
    private val all = listOf(solea, blues, arbol, cancionero)

    @Test
    fun `el nombre es el titulo del archivo o, sin titulo, el nombre sin extension`() {
        assertEquals("Soleá por Bulerías", solea.displayTitle)
        assertEquals("blues en la", blues.displayTitle)
        assertEquals("Cancionero", cancionero.displayTitle)
    }

    @Test
    fun `recientes primero`() {
        assertEquals(listOf(cancionero, solea, arbol, blues), all.ordered(LibrarySort.RECENT))
    }

    @Test
    fun `por nombre sin distinguir mayusculas ni tildes`() {
        assertEquals(listOf(arbol, blues, cancionero, solea), all.ordered(LibrarySort.NAME, Locale.forLanguageTag("es")))
    }

    @Test
    fun `las mas abiertas primero y, empatadas, la mas reciente`() {
        assertEquals(listOf(blues, solea, arbol, cancionero), all.ordered(LibrarySort.MOST_OPENED))
    }

    @Test
    fun `un orden guardado desconocido vuelve al de siempre`() {
        assertEquals(LibrarySort.NAME, LibrarySort.fromKey("NAME"))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromKey("ALFABETICO"))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromKey(null))
    }

    @Test
    fun `busca en titulo, artista y nombre de archivo sin tildes ni mayusculas`() {
        assertEquals(listOf(solea), all.matching("PACO"))
        assertEquals(listOf(solea), all.matching("solea"))
        assertEquals(listOf(arbol), all.matching("arbol vida"))
        assertEquals(listOf(arbol), all.matching("track01"))
        assertEquals(listOf(solea, arbol), all.matching("  ÍA  "))
    }

    @Test
    fun `todas las palabras deben aparecer`() {
        assertEquals(listOf(solea), all.matching("bulerias paco"))
        assertEquals(emptyList<LibraryEntry>(), all.matching("bulerias blues"))
    }

    @Test
    fun `una busqueda vacia devuelve todo en el mismo orden`() {
        assertEquals(all, all.matching(""))
        assertEquals(all, all.matching("   "))
    }
}
