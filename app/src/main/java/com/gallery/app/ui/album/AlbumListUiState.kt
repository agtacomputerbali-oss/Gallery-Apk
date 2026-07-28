package com.gallery.app.ui.album

import com.gallery.app.domain.model.Album

sealed interface AlbumListUiState {
    data object Loading : AlbumListUiState
    data class Success(val albums: List<Album>) : AlbumListUiState
    data class Error(val message: String) : AlbumListUiState
}
