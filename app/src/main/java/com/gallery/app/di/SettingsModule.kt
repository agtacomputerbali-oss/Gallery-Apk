package com.gallery.app.di

import com.gallery.app.data.repository.ThemeRepository
import com.gallery.app.data.repository.ThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): ThemeRepository
}
