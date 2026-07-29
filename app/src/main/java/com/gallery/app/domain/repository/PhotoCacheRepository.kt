package com.gallery.app.domain.repository

import androidx.paging.PagingData
import com.gallery.app.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow

interface PhotoCacheRepository {
    fun getCachedPhotos(): Flow<PagingData<PhotoItem>>
    fun getCachedPhotosByBucket(bucketId: Long): Flow<PagingData<PhotoItem>>
    fun getCachedTrashedPhotos(): Flow<PagingData<PhotoItem>>
    fun getIndexedPhotoCount(): Flow<Int>
    suspend fun getPhotoCount(): Int
    fun triggerManualIndexing()
    fun triggerPHashIndexing()
    fun getSmartAlbums(): Flow<List<com.gallery.app.domain.model.SmartAlbum>>
    fun getCachedPhotosBySmartType(type: com.gallery.app.domain.model.SmartAlbumType): Flow<PagingData<PhotoItem>>
    suspend fun getAllHashedPhotos(): List<com.gallery.app.data.local.entity.CachedPhotoEntity>
}
