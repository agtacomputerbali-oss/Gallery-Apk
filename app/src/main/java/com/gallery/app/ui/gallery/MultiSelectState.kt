package com.gallery.app.ui.gallery

import com.gallery.app.domain.model.PhotoItem

data class MultiSelectState(
    val isSelectionMode: Boolean = false,
    val selectedPhotos: Set<PhotoItem> = emptySet()
) {
    val selectedCount: Int get() = selectedPhotos.size
}
