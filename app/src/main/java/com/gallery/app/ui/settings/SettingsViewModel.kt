package com.gallery.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.data.repository.ThemeRepository
import com.gallery.app.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val themeModeState: StateFlow<AppTheme> = themeRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setThemeMode(theme)
        }
    }
}
