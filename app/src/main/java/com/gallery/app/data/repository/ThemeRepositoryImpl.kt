package com.gallery.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gallery.app.domain.model.AccentColor
import com.gallery.app.domain.model.MediaTypeFilter
import com.gallery.app.domain.model.VaultLockDelay
import com.gallery.app.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    private val keyThemeMode = stringPreferencesKey("key_theme_mode")
    private val keyGalleryGridColumns = intPreferencesKey("key_gallery_grid_columns")
    private val keyAlbumGridColumns = intPreferencesKey("key_album_grid_columns")
    private val keyVaultGridColumns = intPreferencesKey("key_vault_grid_columns")
    private val keyDefaultMediaFilter = stringPreferencesKey("key_default_media_filter")
    private val keyAutoPlayVideo = booleanPreferencesKey("key_auto_play_video")
    private val keyMuteVideoByDefault = booleanPreferencesKey("key_mute_video_by_default")
    private val keyVaultLockDelay = stringPreferencesKey("key_vault_lock_delay")
    private val keyAccentColor = stringPreferencesKey("key_accent_color")

    override val themeMode: Flow<AppTheme> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val themeName = preferences[keyThemeMode] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }

    override suspend fun setThemeMode(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[keyThemeMode] = theme.name
        }
    }

    override val galleryGridColumns: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[keyGalleryGridColumns]?.coerceIn(3, 6) ?: 3 }

    override val albumGridColumns: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[keyAlbumGridColumns]?.coerceIn(3, 6) ?: 3 }

    override val vaultGridColumns: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[keyVaultGridColumns]?.coerceIn(3, 6) ?: 3 }

    override suspend fun setGalleryGridColumns(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyGalleryGridColumns] = count.coerceIn(3, 6)
        }
    }

    override suspend fun setAlbumGridColumns(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyAlbumGridColumns] = count.coerceIn(3, 6)
        }
    }

    override suspend fun setVaultGridColumns(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyVaultGridColumns] = count.coerceIn(3, 6)
        }
    }

    override val defaultMediaFilter: Flow<MediaTypeFilter> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val filterName = preferences[keyDefaultMediaFilter] ?: MediaTypeFilter.ALL.name
            try {
                MediaTypeFilter.valueOf(filterName)
            } catch (e: Exception) {
                MediaTypeFilter.ALL
            }
        }

    override suspend fun setDefaultMediaFilter(filter: MediaTypeFilter) {
        context.dataStore.edit { preferences ->
            preferences[keyDefaultMediaFilter] = filter.name
        }
    }

    override val isAutoPlayVideo: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[keyAutoPlayVideo] ?: true }

    override suspend fun setAutoPlayVideo(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyAutoPlayVideo] = enabled
        }
    }

    override val isMuteVideoByDefault: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[keyMuteVideoByDefault] ?: true }

    override suspend fun setMuteVideoByDefault(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyMuteVideoByDefault] = enabled
        }
    }

    override val vaultLockDelay: Flow<VaultLockDelay> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val delayName = preferences[keyVaultLockDelay] ?: VaultLockDelay.IMMEDIATELY.name
            try {
                VaultLockDelay.valueOf(delayName)
            } catch (e: Exception) {
                VaultLockDelay.IMMEDIATELY
            }
        }

    override suspend fun setVaultLockDelay(delay: VaultLockDelay) {
        context.dataStore.edit { preferences ->
            preferences[keyVaultLockDelay] = delay.name
        }
    }

    override val accentColor: Flow<AccentColor> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val colorName = preferences[keyAccentColor] ?: AccentColor.DEFAULT_EMERALD.name
            try {
                AccentColor.valueOf(colorName)
            } catch (e: Exception) {
                AccentColor.DEFAULT_EMERALD
            }
        }

    override suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { preferences ->
            preferences[keyAccentColor] = color.name
        }
    }
}
