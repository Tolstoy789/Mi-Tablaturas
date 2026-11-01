package com.tolstoy.mitablaturas.core.data.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabFormat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Base de datos real de Room sobre SQLite (Robolectric): la migración debe dejar el esquema que
 * Room espera, o la app se cerraría al abrir la biblioteca tras actualizarse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryDatabaseTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val name = "library-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    /** Tabla tal como la creaba la versión 1 de la base de datos, con [rows] ya guardadas. */
    private fun createVersion1(vararg rows: String) {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recent_documents` (`uri` TEXT NOT NULL, `display_name` TEXT NOT NULL, " +
                    "`format` TEXT NOT NULL, `size_bytes` INTEGER, `last_opened_at` INTEGER NOT NULL, " +
                    "`is_favorite` INTEGER NOT NULL, `track_index` INTEGER NOT NULL, `speed` REAL NOT NULL, " +
                    "`position_ms` INTEGER NOT NULL, PRIMARY KEY(`uri`))",
            )
            rows.forEach { db.execSQL("INSERT INTO recent_documents VALUES ($it)") }
            db.version = 1
        }
    }

    private fun open(): LibraryDatabase =
        Room.databaseBuilder(context, LibraryDatabase::class.java, name)
            .addMigrations(*LibraryDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `la version 1 pasa a la 2 sin perder canciones ni ajustes`() = runBlocking {
        createVersion1(
            "'content://1', 'Blues en La.gp5', 'GUITAR_PRO_5', 300, 1000, 1, 2, 0.75, 5000",
            "'content://2', 'Cancionero.pdf', 'PDF', NULL, 2000, 0, 0, 1.0, 0",
        )
        val db = open()
        try {
            val dao = db.recentDocumentDao()
            val blues = checkNotNull(dao.get("content://1")?.toModel())
            assertEquals("Blues en La.gp5", blues.document.displayName)
            assertTrue(blues.isFavorite)
            assertEquals(PracticeSettings(trackIndex = 2, speed = 0.75f, positionMs = 5000, page = 0), blues.practice)
            assertEquals("", blues.title)
            assertEquals("", blues.artist)
            assertEquals(1, blues.openCount)

            val songbook = checkNotNull(dao.get("content://2")?.toModel())
            assertEquals(TabFormat.PDF, songbook.document.format)
            assertNull(songbook.document.sizeBytes)
            assertEquals(0, songbook.practice.page)
            // Las favoritas no caducan: solo el PDF queda fuera de las 0 más recientes.
            assertEquals(listOf("content://2"), dao.staleUris(keep = 0))
        } finally {
            db.close()
        }
    }

    @Test
    fun `cada apertura suma una y se guardan titulo, artista y pagina`() = runBlocking {
        val db = open()
        try {
            val dao = db.recentDocumentDao()
            val row = RecentDocumentEntity("content://1", "Blues en La.gp5", "GUITAR_PRO_5", 300, lastOpenedAt = 1000)
            dao.recordOpened(row)
            dao.updatePractice("content://1", trackIndex = 1, speed = 0.8f, positionMs = 1200)
            dao.recordOpened(row.copy(lastOpenedAt = 2000))
            dao.updateScoreInfo("content://1", "Blues en La", "Elías")
            dao.updatePdfPage("content://1", 3)

            val saved = checkNotNull(dao.get("content://1"))
            assertEquals(2, saved.openCount)
            assertEquals(2000L, saved.lastOpenedAt)
            assertEquals("Blues en La", saved.title)
            assertEquals("Elías", saved.artist)
            assertEquals(PracticeSettings(trackIndex = 1, speed = 0.8f, positionMs = 1200, page = 3), saved.toPracticeSettings())
        } finally {
            db.close()
        }
    }
}
