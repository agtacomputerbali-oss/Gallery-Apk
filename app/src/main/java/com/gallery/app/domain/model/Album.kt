package com.gallery.app.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    val photoCount: Int
)
