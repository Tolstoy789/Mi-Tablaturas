package com.tolstoy.mitablaturas.core.data.library

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local de la biblioteca.
 *
 * Las migraciones se escriben a mano (sin esquemas exportados) y `LibraryDatabaseTest` comprueba
 * que una base de datos de la versión anterior pasa la validación de Room sin perder datos.
 */
@Database(entities = [RecentDocumentEntity::class], version = 2, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun recentDocumentDao(): RecentDocumentDao

    companion object {
        /** Todas las migraciones, en orden, para `Room.databaseBuilder(...).addMigrations(...)`. */
        val MIGRATIONS: Array<Migration> get() = arrayOf(MIGRATION_1_2)
    }
}

/**
 * Versión 2: título y artista de la partitura (búsqueda), veces que se abrió (orden) y página del
 * PDF. Las canciones guardadas cuentan como abiertas una vez.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recent_documents ADD COLUMN title TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE recent_documents ADD COLUMN artist TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE recent_documents ADD COLUMN open_count INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE recent_documents ADD COLUMN pdf_page INTEGER NOT NULL DEFAULT 0")
    }
}
