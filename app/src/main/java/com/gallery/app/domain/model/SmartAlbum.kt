package com.gallery.app.domain.model

import android.net.Uri

data class SmartAlbum(
    val type: SmartAlbumType,
    val displayName: String,
    val coverUri: Uri?,
    val photoCount: Int
)

enum class SmartAlbumType {
    VIDEOS,
    SCREENSHOTS,
    HAS_LOCATION,
    SELFIES
}
