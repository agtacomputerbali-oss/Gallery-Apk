package com.gallery.app.domain.model

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "image/*",
    val orientation: Int = 0,
    val isTrashed: Boolean = false
)
