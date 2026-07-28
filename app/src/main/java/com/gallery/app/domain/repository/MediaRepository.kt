package com.gallery.app.domain.repository

import android.content.IntentSender
import android.net.Uri
import androidx.paging.PagingData
import com.gallery.app.domain.model.Album
import com.gallery.app.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getPhotos(): Flow<PagingData<PhotoItem>>
    fun getAlbums(): Flow<List<Album>>
    fun getPhotosByBucket(bucketId: Long): Flow<PagingData<PhotoItem>>
    fun getTrashedPhotos(): Flow<PagingData<PhotoItem>>
    suspend fun createDeleteIntentSender(uris: List<Uri>): IntentSender?
    suspend fun restorePhotos(uris: List<Uri>): IntentSender?
    suspend fun permanentDeletePhotos(uris: List<Uri>): IntentSender?
}

