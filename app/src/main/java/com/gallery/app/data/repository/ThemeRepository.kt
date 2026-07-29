package com.gallery.app.data.repository

import com.gallery.app.domain.model.AccentColor
import com.gallery.app.domain.model.MediaTypeFilter
import com.gallery.app.domain.model.VaultLockDelay
import com.gallery.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<AppTheme>
    suspend fun setThemeMode(theme: AppTheme)

    val galleryGridColumns: Flow<Int>
    val albumGridColumns: Flow<Int>
    val vaultGridColumns: Flow<Int>

    suspend fun setGalleryGridColumns(count: Int)
    suspend fun setAlbumGridColumns(count: Int)
    suspend fun setVaultGridColumns(count: Int)

    val defaultMediaFilter: Flow<MediaTypeFilter>
    suspend fun setDefaultMediaFilter(filter: MediaTypeFilter)

    val isAutoPlayVideo: Flow<Boolean>
    suspend fun setAutoPlayVideo(enabled: Boolean)

    val isMuteVideoByDefault: Flow<Boolean>
    suspend fun setMuteVideoByDefault(enabled: Boolean)

    val vaultLockDelay: Flow<VaultLockDelay>
    suspend fun setVaultLockDelay(delay: VaultLockDelay)

    val accentColor: Flow<AccentColor>
    suspend fun setAccentColor(color: AccentColor)
}
