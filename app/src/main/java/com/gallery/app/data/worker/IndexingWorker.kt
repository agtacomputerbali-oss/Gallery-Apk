package com.gallery.app.data.worker

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gallery.app.data.local.dao.PhotoDao
import com.gallery.app.data.local.entity.CachedPhotoEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IndexingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface IndexingWorkerEntryPoint {
        fun photoDao(): PhotoDao
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                IndexingWorkerEntryPoint::class.java
            )
            val photoDao = entryPoint.photoDao()

            val photosToCache = queryAllMediaFromMediaStore()

            if (photosToCache.isNotEmpty()) {
                photoDao.upsertPhotos(photosToCache)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "IndexingWorker failed", e)
            Result.retry()
        }
    }

    private fun queryAllMediaFromMediaStore(): List<CachedPhotoEntity> {
        val photos = mutableListOf<CachedPhotoEntity>()

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "(${MediaStore.MediaColumns.IS_TRASHED} IS NULL OR ${MediaStore.MediaColumns.IS_TRASHED} = 0)"
        } else null

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor: Cursor? = applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val sizeCol = c.getColumnIndex(MediaStore.Images.Media.SIZE)
            val widthCol = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val orientCol = c.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
            val bucketIdCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            } else -1

            while (c.moveToNext()) {
                if (idCol == -1) continue
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val displayName = if (nameCol != -1) c.getString(nameCol) ?: "" else ""
                val dateTaken = if (dateCol != -1) c.getLong(dateCol) else 0L
                val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                val width = if (widthCol != -1) c.getInt(widthCol) else 0
                val height = if (heightCol != -1) c.getInt(heightCol) else 0
                val mimeType = if (mimeCol != -1) c.getString(mimeCol) ?: "image/*" else "image/*"
                val orientation = if (orientCol != -1) c.getInt(orientCol) else 0
                val bucketId = if (bucketIdCol != -1) c.getLong(bucketIdCol) else 0L
                val bucketName = if (bucketNameCol != -1) c.getString(bucketNameCol) ?: "Umum" else "Umum"
                val isTrashed = if (trashedCol != -1) c.getInt(trashedCol) == 1 else false

                photos.add(
                    CachedPhotoEntity(
                        id = id,
                        uriString = uri.toString(),
                        displayName = displayName,
                        dateTaken = dateTaken,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        orientation = orientation,
                        bucketId = bucketId,
                        bucketName = bucketName,
                        isTrashed = isTrashed,
                        latitude = null,
                        longitude = null
                    )
                )
            }
        }

        return photos
    }

    companion object {
        const val WORK_NAME = "IndexingWorkerJob"
        private const val TAG = "IndexingWorker"
    }
}
