package com.gallery.app.di

import android.content.Context
import com.gallery.app.data.repository.MediaRepositoryImpl
import com.gallery.app.domain.repository.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context
    ): MediaRepository {
        return MediaRepositoryImpl(context)
    }
}
