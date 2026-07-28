package com.gallery.app.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.gallery.app.data.paging.MediaPagingSource
import com.gallery.app.domain.model.Album
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val context: Context
) : MediaRepository {

    override fun getPhotos(): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2
            ),
            pagingSourceFactory = { MediaPagingSource(context) }
        ).flow
    }

    override fun getAlbums(): Flow<List<Album>> = flow {
        val albums = queryAlbumsFromMediaStore()
        emit(albums)
    }.flowOn(Dispatchers.IO)

    override fun getPhotosByBucket(bucketId: Long): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2
            ),
            pagingSourceFactory = { MediaPagingSource(context, bucketId) }
        ).flow
    }

    override fun getTrashedPhotos(): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2
            ),
            pagingSourceFactory = { MediaPagingSource(context, isTrashed = true) }
        ).flow
    }

    override suspend fun createDeleteIntentSender(uris: List<Uri>): IntentSender? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                for (uri in uris) {
                    context.contentResolver.delete(uri, null, null)
                }
                null
            } catch (securityException: SecurityException) {
                val recoverableSecurityException = securityException as? RecoverableSecurityException
                recoverableSecurityException?.userAction?.actionIntent?.intentSender
            }
        } else {
            for (uri in uris) {
                context.contentResolver.delete(uri, null, null)
            }
            null
        }
    }

    override suspend fun restorePhotos(uris: List<Uri>): IntentSender? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(context.contentResolver, uris, false).intentSender
        } else {
            null
        }
    }

    override suspend fun permanentDeletePhotos(uris: List<Uri>): IntentSender? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } else {
            for (uri in uris) {
                context.contentResolver.delete(uri, null, null)
            }
            null
        }
    }


    private fun queryAlbumsFromMediaStore(): List<Album> {
        val albumMap = LinkedHashMap<Long, AlbumInfo>()

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else null

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val bucketId = c.getLong(bucketIdCol)
                val bucketName = c.getString(bucketNameCol) ?: "Umum"

                val albumInfo = albumMap.getOrPut(bucketId) {
                    val coverUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    AlbumInfo(id = bucketId, name = bucketName, coverUri = coverUri, count = 0)
                }
                albumInfo.count++
            }
        }

        return albumMap.values.map { info ->
            Album(
                id = info.id,
                name = info.name,
                coverUri = info.coverUri,
                photoCount = info.count
            )
        }
    }

    private class AlbumInfo(
        val id: Long,
        val name: String,
        val coverUri: Uri?,
        var count: Int
    )

    companion object {
        private const val PAGE_SIZE = 30
    }
}
