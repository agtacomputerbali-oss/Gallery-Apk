package com.gallery.app

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers

import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gallery.app.data.worker.IndexingWorker
import com.gallery.app.data.worker.MediaStoreObserver


import coil.decode.VideoFrameDecoder

@HiltAndroidApp
class GalleryApplication : Application() {

    private var mediaStoreObserver: MediaStoreObserver? = null

    override fun onCreate() {
        super.onCreate()
        setupCoilImageLoader()
        setupUncaughtExceptionHandler()
        setupMediaStoreObserver()
        enqueueInitialIndexingWorker()
    }

    private fun setupMediaStoreObserver() {
        try {
            val handler = Handler(Looper.getMainLooper())
            mediaStoreObserver = MediaStoreObserver(this, handler)
            mediaStoreObserver?.let { observer ->
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    MediaStore.Files.getContentUri("external"),
                    true,
                    observer
                )
            }
        } catch (e: Exception) {
            Log.e("GalleryApplication", "Failed to register MediaStoreObserver", e)
        }
    }

    private fun enqueueInitialIndexingWorker() {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val indexingRequest = OneTimeWorkRequestBuilder<IndexingWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                IndexingWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                indexingRequest
            )
        } catch (e: Exception) {
            Log.e("GalleryApplication", "Failed to enqueue initial IndexingWorker", e)
        }
    }


    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun setupCoilImageLoader() {
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(8)
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .allowHardware(true)
            .fetcherDispatcher(limitedDispatcher)
            .decoderDispatcher(limitedDispatcher)
            .interceptorDispatcher(limitedDispatcher)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(50)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GalleryApplication", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
