package com.gallery.app.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.domain.usecase.GetAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumListViewModel @Inject constructor(
    getAlbumsUseCase: GetAlbumsUseCase
) : ViewModel() {

    val uiState: StateFlow<AlbumListUiState> = getAlbumsUseCase()
        .map<_, AlbumListUiState> { albums ->
            AlbumListUiState.Success(albums)
        }
        .catch { e ->
            emit(AlbumListUiState.Error(e.localizedMessage ?: "Gagal memuat album"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlbumListUiState.Loading
        )
}
