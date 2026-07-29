package com.gallery.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gallery.app.data.local.dao.PhotoDao
import com.gallery.app.data.local.entity.CachedPhotoEntity
import com.gallery.app.data.worker.IndexingWorker
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.PhotoCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoDao: PhotoDao
) : PhotoCacheRepository {

    override fun getCachedPhotos(): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { photoDao.getPhotosPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toPhotoItem() }
        }
    }

    override fun getCachedPhotosByBucket(bucketId: Long): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { photoDao.getPhotosByBucketPagingSource(bucketId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toPhotoItem() }
        }
    }

    override fun getCachedTrashedPhotos(): Flow<PagingData<PhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = { photoDao.getTrashedPhotosPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toPhotoItem() }
        }
    }

    override fun getIndexedPhotoCount(): Flow<Int> {
        return photoDao.getPhotoCountFlow()
    }

    override suspend fun getPhotoCount(): Int {
        return photoDao.getPhotoCount()
    }

    override fun triggerManualIndexing() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val indexingRequest = OneTimeWorkRequestBuilder<IndexingWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IndexingWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            indexingRequest
        )
    }

    override fun triggerPHashIndexing() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val pHashRequest = OneTimeWorkRequestBuilder<com.gallery.app.data.worker.PHashIndexingWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            com.gallery.app.data.worker.PHashIndexingWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            pHashRequest
        )
    }

    override fun getSmartAlbums(): Flow<List<com.gallery.app.domain.model.SmartAlbum>> = kotlinx.coroutines.flow.flow {
        val albums = mutableListOf<com.gallery.app.domain.model.SmartAlbum>()

        val videoCount = photoDao.getVideoCount()
        if (videoCount > 0) {
            val cover = photoDao.getVideoCoverUri()?.let { Uri.parse(it) }
            albums.add(
                com.gallery.app.domain.model.SmartAlbum(
                    type = com.gallery.app.domain.model.SmartAlbumType.VIDEOS,
                    displayName = "Video",
                    coverUri = cover,
                    photoCount = videoCount
                )
            )
        }

        val screenshotCount = photoDao.getScreenshotCount()
        if (screenshotCount > 0) {
            val cover = photoDao.getScreenshotCoverUri()?.let { Uri.parse(it) }
            albums.add(
                com.gallery.app.domain.model.SmartAlbum(
                    type = com.gallery.app.domain.model.SmartAlbumType.SCREENSHOTS,
                    displayName = "Tangkap Layar",
                    coverUri = cover,
                    photoCount = screenshotCount
                )
            )
        }

        val geotaggedCount = photoDao.getGeotaggedCount()
        if (geotaggedCount > 0) {
            val cover = photoDao.getGeotaggedCoverUri()?.let { Uri.parse(it) }
            albums.add(
                com.gallery.app.domain.model.SmartAlbum(
                    type = com.gallery.app.domain.model.SmartAlbumType.HAS_LOCATION,
                    displayName = "Memiliki Lokasi",
                    coverUri = cover,
                    photoCount = geotaggedCount
                )
            )
        }

        val selfieCount = photoDao.getSelfieCount()
        if (selfieCount > 0) {
            val cover = photoDao.getSelfieCoverUri()?.let { Uri.parse(it) }
            albums.add(
                com.gallery.app.domain.model.SmartAlbum(
                    type = com.gallery.app.domain.model.SmartAlbumType.SELFIES,
                    displayName = "Selfie & Kamera Depan",
                    coverUri = cover,
                    photoCount = selfieCount
                )
            )
        }

        emit(albums)
    }.flowOn(Dispatchers.IO)

    override fun getCachedPhotosBySmartType(type: com.gallery.app.domain.model.SmartAlbumType): Flow<PagingData<PhotoItem>> {
        val pagingSourceFactory = when (type) {
            com.gallery.app.domain.model.SmartAlbumType.VIDEOS -> { { photoDao.getVideosPagingSource() } }
            com.gallery.app.domain.model.SmartAlbumType.SCREENSHOTS -> { { photoDao.getScreenshotsPagingSource() } }
            com.gallery.app.domain.model.SmartAlbumType.HAS_LOCATION -> { { photoDao.getGeotaggedPagingSource() } }
            com.gallery.app.domain.model.SmartAlbumType.SELFIES -> { { photoDao.getSelfiesPagingSource() } }
        }

        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = 15,
                maxSize = 200
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData ->
            pagingData.map { it.toPhotoItem() }
        }
    }

    override suspend fun getAllHashedPhotos(): List<CachedPhotoEntity> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        photoDao.getAllHashedPhotos()
    }

    private fun CachedPhotoEntity.toPhotoItem(): PhotoItem {
        return PhotoItem(
            id = id,
            uri = Uri.parse(uriString),
            displayName = displayName,
            dateTaken = dateTaken,
            size = size,
            width = width,
            height = height,
            mimeType = mimeType,
            orientation = orientation,
            isTrashed = isTrashed
        )
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
