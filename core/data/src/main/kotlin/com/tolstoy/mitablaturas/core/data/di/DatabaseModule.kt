package com.tolstoy.mitablaturas.core.data.di

import android.content.Context
import androidx.room.Room
import com.tolstoy.mitablaturas.core.data.library.LibraryDatabase
import com.tolstoy.mitablaturas.core.data.library.RecentDocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLibraryDatabase(@ApplicationContext context: Context): LibraryDatabase =
        Room.databaseBuilder(context, LibraryDatabase::class.java, "library.db").build()

    @Provides
    fun provideRecentDocumentDao(database: LibraryDatabase): RecentDocumentDao = database.recentDocumentDao()
}
