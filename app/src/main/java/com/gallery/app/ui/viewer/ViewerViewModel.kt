package com.gallery.app.ui.viewer

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.usecase.DeletePhotosUseCase
import com.gallery.app.domain.usecase.SharePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    private val sharePhotosUseCase: SharePhotosUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase
) : ViewModel() {

    val photosState: Flow<PagingData<PhotoItem>> = mediaRepository.getPhotos()
        .cachedIn(viewModelScope)

    fun sharePhoto(photo: PhotoItem): Intent? {
        return sharePhotosUseCase(listOf(photo.uri))
    }

    fun deletePhoto(
        photo: PhotoItem,
        onIntentSenderReady: (IntentSender) -> Unit,
        onDeleteSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val intentSender = deletePhotosUseCase(listOf(photo.uri))
            if (intentSender != null) {
                onIntentSenderReady(intentSender)
            } else {
                onDeleteSuccess()
            }
        }
    }
}

