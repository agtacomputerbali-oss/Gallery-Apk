package com.gallery.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_photos",
    indices = [
        Index(value = ["bucketId"]),
        Index(value = ["dateTaken"]),
        Index(value = ["isTrashed"]),
        Index(value = ["pHash"])
    ]
)
data class CachedPhotoEntity(
    @PrimaryKey
    val id: Long,
    val uriString: String,
    val displayName: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val orientation: Int,
    val bucketId: Long,
    val bucketName: String,
    val isTrashed: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pHash: String? = null
)
