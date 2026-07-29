package com.gallery.app.di

import android.content.Context
import coil.ImageLoader
import com.gallery.app.data.repository.MediaRepositoryImpl
import com.gallery.app.data.repository.PhotoCacheRepositoryImpl
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.repository.PhotoCacheRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPhotoCacheRepository(
        impl: PhotoCacheRepositoryImpl
    ): PhotoCacheRepository

    companion object {
        @Provides
        @Singleton
        @VaultImageLoader
        fun provideVaultImageLoader(
            @ApplicationContext context: Context
        ): ImageLoader {
            return ImageLoader.Builder(context)
                .memoryCache(null)
                .diskCache(null)
                .build()
        }
    }
}


