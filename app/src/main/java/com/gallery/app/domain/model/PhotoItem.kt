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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoItem) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
