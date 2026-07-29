package com.gallery.app.data.worker

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gallery.app.data.local.dao.PhotoDao
import com.gallery.app.util.PHashCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PHashIndexingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PHashWorkerEntryPoint {
        fun photoDao(): PhotoDao
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PHashWorkerEntryPoint::class.java
            )
            val photoDao = entryPoint.photoDao()

            val unhashedPhotos = photoDao.getPhotosWithoutHash()

            var processedCount = 0
            for (photo in unhashedPhotos) {
                try {
                    val uri = Uri.parse(photo.uriString)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // hemat memori saat decode
                    }

                    applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        if (bitmap != null) {
                            try {
                                val hash = PHashCalculator.calculatePHash(bitmap)
                                photoDao.updatePhotoHash(photo.id, hash)
                                processedCount++
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fail silently for individual photo hash computation error
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "PHashIndexingWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "PHashIndexingWorkerJob"
        private const val TAG = "PHashIndexingWorker"
    }
}
