package com.gallery.app.data.repository

import com.gallery.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<AppTheme>
    suspend fun setThemeMode(theme: AppTheme)
}
