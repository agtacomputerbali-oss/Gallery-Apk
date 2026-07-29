package com.gallery.app.data.worker

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MediaStoreObserver(
    private val context: Context,
    handler: Handler
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        _mediaStoreChanges.tryEmit(System.currentTimeMillis())
        scheduleDebouncedIndexing()
    }

    private fun scheduleDebouncedIndexing() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val indexingRequest = OneTimeWorkRequestBuilder<IndexingWorker>()
            .setConstraints(constraints)
            .setInitialDelay(DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IndexingWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            indexingRequest
        )
    }

    companion object {
        private const val TAG = "MediaStoreObserver"
        private const val DEBOUNCE_DELAY_SECONDS = 3L

        private val _mediaStoreChanges = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 16)
        val mediaStoreChanges: SharedFlow<Long> = _mediaStoreChanges.asSharedFlow()

        private val _immediateRefreshFlow = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 16)
        val immediateRefreshFlow: SharedFlow<Long> = _immediateRefreshFlow.asSharedFlow()

        fun triggerManualRefresh() {
            _mediaStoreChanges.tryEmit(System.currentTimeMillis())
        }

        fun triggerImmediateRefresh() {
            _immediateRefreshFlow.tryEmit(System.currentTimeMillis())
            _mediaStoreChanges.tryEmit(System.currentTimeMillis())
        }
    }
}

