package com.gallery.app.ui.gallery.model

import com.gallery.app.domain.model.PhotoItem

sealed class GalleryItemModel {
    data class HeaderModel(
        val id: String,
        val title: String,
        val count: Int,
        val dateGroupKey: String,
        val photos: List<PhotoItem>
    ) : GalleryItemModel()

    data class PhotoModel(
        val photo: PhotoItem
    ) : GalleryItemModel()
}
