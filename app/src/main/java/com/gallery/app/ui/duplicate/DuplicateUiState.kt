package com.gallery.app.ui.duplicate

import com.gallery.app.data.local.entity.CachedPhotoEntity
import com.gallery.app.domain.model.PhotoItem

data class DuplicateGroup(
    val id: String,
    val representativePhoto: PhotoItem,
    val duplicates: List<PhotoItem>,
    val selectedPhotoIds: Set<Long> = emptySet()
)

sealed interface DuplicateUiState {
    data object Loading : DuplicateUiState
    data object Analyzing : DuplicateUiState
    data object Empty : DuplicateUiState
    data class Success(
        val groups: List<DuplicateGroup>,
        val totalDuplicateCount: Int
    ) : DuplicateUiState
    data class Error(val message: String) : DuplicateUiState
}
