package com.gallery.app.domain.usecase

import androidx.paging.PagingData
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhotosByBucketUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(bucketId: Long): Flow<PagingData<PhotoItem>> {
        return repository.getPhotosByBucket(bucketId)
    }
}
