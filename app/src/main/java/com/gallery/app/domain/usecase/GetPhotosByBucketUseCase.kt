package com.gallery.app.domain.usecase

import androidx.paging.PagingData
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.model.SortOption
import com.gallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.gallery.app.domain.model.MediaTypeFilter

class GetPhotosByBucketUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(
        bucketId: Long,
        sortOption: SortOption = SortOption.DATE_TAKEN_DESC,
        mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL
    ): Flow<PagingData<PhotoItem>> {
        return repository.getPhotosByBucket(bucketId, sortOption, mediaTypeFilter)
    }
}
