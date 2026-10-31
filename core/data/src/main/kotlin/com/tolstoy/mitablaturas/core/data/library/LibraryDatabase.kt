package com.tolstoy.mitablaturas.core.data.library

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos local de la biblioteca.
 *
 * `exportSchema = false` mientras haya una sola versión; al añadir la primera migración,
 * activar la exportación (`room.schemaLocation`) y guardar los esquemas en el repositorio.
 */
@Database(entities = [RecentDocumentEntity::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun recentDocumentDao(): RecentDocumentDao
}
