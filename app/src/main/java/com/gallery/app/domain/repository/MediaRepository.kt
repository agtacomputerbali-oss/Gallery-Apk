package com.gallery.app.domain.repository

import android.content.IntentSender
import android.net.Uri
import androidx.paging.PagingData
import com.gallery.app.domain.model.Album
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.model.SortOption
import com.gallery.app.domain.model.UriWithFolderInfo
import kotlinx.coroutines.flow.Flow

import com.gallery.app.domain.model.CopyMoveResult
import com.gallery.app.domain.model.FolderItem

import com.gallery.app.domain.model.MediaTypeFilter

interface MediaRepository {
    fun getPhotos(
        sortOption: SortOption = SortOption.DATE_TAKEN_DESC,
        mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL
    ): Flow<PagingData<PhotoItem>>
    fun getAlbums(): Flow<List<Album>>
    fun getPhotosByBucket(
        bucketId: Long,
        sortOption: SortOption = SortOption.DATE_TAKEN_DESC,
        mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL
    ): Flow<PagingData<PhotoItem>>
    fun getTrashedPhotos(): Flow<PagingData<PhotoItem>>
    suspend fun createDeleteIntentSender(uris: List<Uri>): IntentSender?
    suspend fun restorePhotos(uris: List<Uri>): IntentSender?
    suspend fun permanentDeletePhotos(uris: List<Uri>): IntentSender?
    suspend fun createMediaDeleteRequest(uris: List<Uri>): IntentSender?
    suspend fun getPhotoUrisByBuckets(bucketIds: List<Long>): List<Uri>
    suspend fun getPhotoUrisWithBucketInfo(bucketIds: List<Long>): List<UriWithFolderInfo>
    suspend fun copyPhotosToFolder(uris: List<Uri>, targetFolderName: String): CopyMoveResult
    suspend fun movePhotosToFolder(uris: List<Uri>, targetFolderName: String): CopyMoveResult
    suspend fun createFolder(folderName: String): Boolean
    suspend fun getFolderList(): List<FolderItem>
}


