package com.tolstoy.mitablaturas.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {

    private fun entry(name: String, format: TabFormat, favorite: Boolean = false) = LibraryEntry(
        document = TabDocument("content://$name", name, format, 100),
        lastOpenedAt = 0,
        isFavorite = favorite,
    )

    private val blues = entry("blues.gp5", TabFormat.GUITAR_PRO_5, favorite = true)
    private val rock = entry("rock.gp", TabFormat.GUITAR_PRO_7)
    private val cancionero = entry("cancionero.pdf", TabFormat.PDF, favorite = true)
    private val all = listOf(blues, rock, cancionero)

    @Test
    fun `todas devuelve la lista completa en el mismo orden`() {
        assertEquals(all, all.filteredBy(LibraryFilter.ALL))
    }

    @Test
    fun `guitar pro deja fuera los pdf`() {
        assertEquals(listOf(blues, rock), all.filteredBy(LibraryFilter.GUITAR_PRO))
    }

    @Test
    fun `pdf solo deja los pdf`() {
        assertEquals(listOf(cancionero), all.filteredBy(LibraryFilter.PDF))
    }

    @Test
    fun `favoritas solo deja las marcadas`() {
        assertEquals(listOf(blues, cancionero), all.filteredBy(LibraryFilter.FAVORITES))
    }
}
