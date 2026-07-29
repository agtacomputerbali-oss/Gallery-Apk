package com.gallery.app.ui.album

import com.gallery.app.domain.model.Album
import com.gallery.app.domain.model.SmartAlbum

sealed interface AlbumListUiState {
    data object Loading : AlbumListUiState
    data class Success(
        val albums: List<Album>,
        val smartAlbums: List<SmartAlbum> = emptyList()
    ) : AlbumListUiState
    data class Error(val message: String) : AlbumListUiState
}
