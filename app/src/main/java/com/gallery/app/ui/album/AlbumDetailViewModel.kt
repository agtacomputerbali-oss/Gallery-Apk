package com.gallery.app.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.usecase.GetPhotosByBucketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPhotosByBucketUseCase: GetPhotosByBucketUseCase
) : ViewModel() {

    val bucketId: Long = checkNotNull(savedStateHandle["bucketId"])
    val bucketName: String = savedStateHandle["bucketName"] ?: "Album"

    val photosState: Flow<PagingData<PhotoItem>> = getPhotosByBucketUseCase(bucketId)
        .cachedIn(viewModelScope)
}
