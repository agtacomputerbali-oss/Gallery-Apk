package com.gallery.app.domain.model

data class FolderItem(
    val name: String,
    val relativePath: String?,
    val photoCount: Int,
    val bucketId: Long? = null
)
