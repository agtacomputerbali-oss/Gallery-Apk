package com.gallery.app.di

import android.content.Context
import androidx.room.Room
import com.gallery.app.data.local.GalleryDatabase
import com.gallery.app.data.local.dao.PhotoDao
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
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase {
        return Room.databaseBuilder(
            context,
            GalleryDatabase::class.java,
            "gallery.db"
        )
            .addMigrations(GalleryDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(
        database: GalleryDatabase
    ): PhotoDao {
        return database.photoDao()
    }
}
