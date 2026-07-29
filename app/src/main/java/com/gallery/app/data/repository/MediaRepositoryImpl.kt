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
import com.gallery.app.domain.model.SortOption
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.gallery.app.domain.model.CopyMoveResult
import com.gallery.app.domain.model.FolderItem
import com.gallery.app.domain.model.UriWithFolderInfo
import com.gallery.app.domain.model.MediaTypeFilter

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {

    override fun getPhotos(
        sortOption: SortOption,
        mediaTypeFilter: MediaTypeFilter
    ): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { MediaPagingSource(context = context, sortOption = sortOption, mediaTypeFilter = mediaTypeFilter) }
        ).flow
    }

    override fun getAlbums(): Flow<List<Album>> = flow {
        val albums = queryAlbumsFromMediaStore()
        emit(albums)
    }.flowOn(ioDispatcher)

    override fun getPhotosByBucket(
        bucketId: Long,
        sortOption: SortOption,
        mediaTypeFilter: MediaTypeFilter
    ): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { MediaPagingSource(context = context, bucketId = bucketId, sortOption = sortOption, mediaTypeFilter = mediaTypeFilter) }
        ).flow
    }

    override fun getTrashedPhotos(): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { MediaPagingSource(context, isTrashed = true) }
        ).flow
    }

    override suspend fun createDeleteIntentSender(uris: List<Uri>): IntentSender? = withContext(ioDispatcher) {
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

    override suspend fun restorePhotos(uris: List<Uri>): IntentSender? = withContext(ioDispatcher) {
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(context.contentResolver, uris, false).intentSender
        } else {
            null
        }
    }

    override suspend fun permanentDeletePhotos(uris: List<Uri>): IntentSender? = withContext(ioDispatcher) {
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

    override suspend fun createMediaDeleteRequest(uris: List<Uri>): IntentSender? {
        return permanentDeletePhotos(uris)
    }

    override suspend fun getPhotoUrisByBuckets(bucketIds: List<Long>): List<Uri> = withContext(ioDispatcher) {
        getPhotoUrisWithBucketInfo(bucketIds).map { it.uri }
    }

    override suspend fun getPhotoUrisWithBucketInfo(bucketIds: List<Long>): List<UriWithFolderInfo> = withContext(ioDispatcher) {
        if (bucketIds.isEmpty()) return@withContext emptyList()
        val list = mutableListOf<UriWithFolderInfo>()
        val projectionList = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }

        val bucketPlaceholders = bucketIds.joinToString(",") { "?" }
        val bucketArgs = bucketIds.map { it.toString() }.toMutableList().apply {
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            add("video/%")
        }.toTypedArray()

        val selectionConditions = mutableListOf<String>()
        selectionConditions.add("${MediaStore.Files.FileColumns.BUCKET_ID} IN ($bucketPlaceholders)")
        selectionConditions.add("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionConditions.add("(${MediaStore.MediaColumns.IS_TRASHED} IS NULL OR ${MediaStore.MediaColumns.IS_TRASHED} = 0)")
        }

        val selection = selectionConditions.joinToString(" AND ")

        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projectionList.toTypedArray(),
            selection,
            bucketArgs,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val bucketNameCol = c.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val mediaTypeCol = c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeTypeCol = c.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else -1

            if (idCol != -1) {
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val mediaType = if (mediaTypeCol != -1) c.getInt(mediaTypeCol) else 0
                    val mimeType = if (mimeTypeCol != -1) c.getString(mimeTypeCol) ?: "" else ""
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mimeType.startsWith("video/")
                    val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val uri = ContentUris.withAppendedId(contentUri, id)
                    val folderName = if (bucketNameCol != -1) c.getString(bucketNameCol) ?: "Umum" else "Umum"
                    val relativePath = if (relativePathCol != -1) c.getString(relativePathCol) else null
                    list.add(UriWithFolderInfo(uri = uri, folderName = folderName, relativePath = relativePath))
                }
            }
        }
        list
    }

    override suspend fun copyPhotosToFolder(uris: List<Uri>, targetFolderName: String): CopyMoveResult = withContext(ioDispatcher) {
        if (uris.isEmpty()) return@withContext CopyMoveResult.Error("Tidak ada foto yang dipilih")
        val cleanFolderName = targetFolderName.trim()
        if (cleanFolderName.isBlank()) return@withContext CopyMoveResult.Error("Nama folder tujuan tidak valid")

        var successCount = 0
        var failCount = 0

        for (sourceUri in uris) {
            try {
                // Get filename & mimeType of source image
                var fileName = "IMG_${System.currentTimeMillis()}_$successCount.jpg"
                var mimeType = "image/jpeg"

                val cursor = context.contentResolver.query(
                    sourceUri,
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE),
                    null, null, null
                )
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        val mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                        if (nameCol != -1) c.getString(nameCol)?.let { fileName = it }
                        if (mimeCol != -1) c.getString(mimeCol)?.let { mimeType = it }
                    }
                }

                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_PICTURES}/$cleanFolderName/")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (targetUri != null) {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        context.contentResolver.openOutputStream(targetUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(targetUri, contentValues, null, null)
                    }
                    successCount++
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                failCount++
            }
        }

        when {
            failCount == 0 -> CopyMoveResult.Success(successCount, cleanFolderName)
            successCount > 0 -> CopyMoveResult.PartialFailure(successCount, failCount, cleanFolderName)
            else -> CopyMoveResult.Error("Gagal menyalin foto ke folder $cleanFolderName")
        }
    }

    override suspend fun movePhotosToFolder(uris: List<Uri>, targetFolderName: String): CopyMoveResult = withContext(ioDispatcher) {
        val copyResult = copyPhotosToFolder(uris, targetFolderName)
        if (copyResult is CopyMoveResult.Success) {
            val deleteIntentSender = createDeleteIntentSender(uris)
            if (deleteIntentSender != null) {
                CopyMoveResult.NeedsDeleteConfirmation(deleteIntentSender, copyResult.count, copyResult.targetFolderName)
            } else {
                copyResult
            }
        } else if (copyResult is CopyMoveResult.PartialFailure) {
            val deleteIntentSender = createDeleteIntentSender(uris)
            if (deleteIntentSender != null) {
                CopyMoveResult.NeedsDeleteConfirmation(deleteIntentSender, copyResult.successCount, copyResult.targetFolderName)
            } else {
                copyResult
            }
        } else {
            copyResult
        }
    }

    override suspend fun createFolder(folderName: String): Boolean = withContext(ioDispatcher) {
        val cleanName = folderName.trim()
        if (cleanName.isBlank()) return@withContext false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, ".temp_create_${System.currentTimeMillis()}")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_PICTURES}/$cleanName/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.delete(uri, null, null)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        } else {
            try {
                val dir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    cleanName
                )
                if (!dir.exists()) dir.mkdirs() else true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getFolderList(): List<com.gallery.app.domain.model.FolderItem> = withContext(ioDispatcher) {
        val folderMap = LinkedHashMap<String, Int>()

        val projection = mutableListOf(
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()

        val selectionConditions = mutableListOf(
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionConditions.add("(${MediaStore.MediaColumns.IS_TRASHED} IS NULL OR ${MediaStore.MediaColumns.IS_TRASHED} = 0)")
        }

        val selection = selectionConditions.joinToString(" AND ")
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "video/%"
        )

        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"
        )

        cursor?.use { c ->
            val bucketNameCol = c.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                if (bucketNameCol != -1) {
                    val name = c.getString(bucketNameCol) ?: "Umum"
                    folderMap[name] = (folderMap[name] ?: 0) + 1
                }
            }
        }

        folderMap.map { (name, count) ->
            com.gallery.app.domain.model.FolderItem(
                name = name,
                relativePath = "${android.os.Environment.DIRECTORY_PICTURES}/$name/",
                photoCount = count
            )
        }
    }

    private fun queryAlbumsFromMediaStore(): List<Album> {
        val albumMap = LinkedHashMap<Long, AlbumInfo>()

        val projection = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()

        val selectionConditions = mutableListOf(
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionConditions.add("(${MediaStore.MediaColumns.IS_TRASHED} IS NULL OR ${MediaStore.MediaColumns.IS_TRASHED} = 0)")
        }

        val selection = selectionConditions.joinToString(" AND ")
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "video/%"
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val bucketIdCol = c.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = c.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val mediaTypeCol = c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeTypeCol = c.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

            while (c.moveToNext()) {
                if (idCol == -1 || bucketIdCol == -1) continue
                val id = c.getLong(idCol)
                val bucketId = c.getLong(bucketIdCol)
                val bucketName = if (bucketNameCol != -1) c.getString(bucketNameCol) ?: "Umum" else "Umum"
                val mediaType = if (mediaTypeCol != -1) c.getInt(mediaTypeCol) else 0
                val mimeType = if (mimeTypeCol != -1) c.getString(mimeTypeCol) ?: "" else ""

                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mimeType.startsWith("video/")
                val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val albumInfo = albumMap.getOrPut(bucketId) {
                    val coverUri = ContentUris.withAppendedId(contentUri, id)
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
