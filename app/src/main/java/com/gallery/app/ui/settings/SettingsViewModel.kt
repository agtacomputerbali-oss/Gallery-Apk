package com.gallery.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.gallery.app.data.repository.ThemeRepository
import com.gallery.app.domain.model.AccentColor
import com.gallery.app.domain.model.MediaTypeFilter
import com.gallery.app.domain.model.VaultLockDelay
import com.gallery.app.domain.repository.PhotoCacheRepository
import com.gallery.app.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
    private val photoCacheRepository: PhotoCacheRepository
) : ViewModel() {

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    val themeModeState: StateFlow<AppTheme> = themeRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val indexedCountState: StateFlow<Int> = photoCacheRepository.getIndexedPhotoCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val galleryGridColumns: StateFlow<Int> = themeRepository.galleryGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val albumGridColumns: StateFlow<Int> = themeRepository.albumGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val vaultGridColumns: StateFlow<Int> = themeRepository.vaultGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val defaultFilterState: StateFlow<MediaTypeFilter> = themeRepository.defaultMediaFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaTypeFilter.ALL)

    val autoPlayVideoState: StateFlow<Boolean> = themeRepository.isAutoPlayVideo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val muteVideoState: StateFlow<Boolean> = themeRepository.isMuteVideoByDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vaultLockDelayState: StateFlow<VaultLockDelay> = themeRepository.vaultLockDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultLockDelay.IMMEDIATELY)

    val accentColorState: StateFlow<AccentColor> = themeRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.DEFAULT_EMERALD)

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch { themeRepository.setThemeMode(theme) }
    }

    fun setGalleryGridColumns(count: Int) {
        viewModelScope.launch { themeRepository.setGalleryGridColumns(count) }
    }

    fun setAlbumGridColumns(count: Int) {
        viewModelScope.launch { themeRepository.setAlbumGridColumns(count) }
    }

    fun setVaultGridColumns(count: Int) {
        viewModelScope.launch { themeRepository.setVaultGridColumns(count) }
    }

    fun setDefaultMediaFilter(filter: MediaTypeFilter) {
        viewModelScope.launch { themeRepository.setDefaultMediaFilter(filter) }
    }

    fun setAutoPlayVideo(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setAutoPlayVideo(enabled) }
    }

    fun setMuteVideoByDefault(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setMuteVideoByDefault(enabled) }
    }

    fun setVaultLockDelay(delay: VaultLockDelay) {
        viewModelScope.launch { themeRepository.setVaultLockDelay(delay) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { themeRepository.setAccentColor(color) }
    }

    fun triggerManualReindex() {
        photoCacheRepository.triggerManualIndexing()
        viewModelScope.launch { _messageEvent.emit("Pembersihan & pembaruan indeks metadata dipicu") }
    }

    fun clearCoilThumbnailCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
                _messageEvent.emit("Cache thumbnail berhasil dibersihkan")
            } catch (e: Exception) {
                _messageEvent.emit("Pembersihan cache selesai")
            }
        }
    }
}
