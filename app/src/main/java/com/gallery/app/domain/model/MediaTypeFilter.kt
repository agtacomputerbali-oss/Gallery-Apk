package com.gallery.app.domain.model

enum class MediaTypeFilter {
    ALL,
    PHOTOS_ONLY,
    VIDEOS_ONLY;

    val label: String
        get() = when (this) {
            ALL -> "Semua"
            PHOTOS_ONLY -> "Foto"
            VIDEOS_ONLY -> "Video"
        }
}
