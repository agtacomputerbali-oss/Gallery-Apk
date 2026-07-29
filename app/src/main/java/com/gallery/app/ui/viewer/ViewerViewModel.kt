package com.gallery.app.ui.viewer

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.usecase.DeletePhotosUseCase
import com.gallery.app.domain.usecase.SharePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ViewerUiEvent {
    data object RefreshMedia : ViewerUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ViewerViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle,
    private val sharePhotosUseCase: SharePhotosUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase
) : ViewModel() {

    private val bucketId: Long? = savedStateHandle.get<Long>("bucketId")?.takeIf { it != -1L }

    private val _uiEvent = MutableSharedFlow<ViewerUiEvent>()
    val uiEvent: SharedFlow<ViewerUiEvent> = _uiEvent.asSharedFlow()

    private val _activePhotoId = MutableStateFlow<Long?>(null)
    val activePhotoId: StateFlow<Long?> = _activePhotoId.asStateFlow()

    fun setActivePhotoId(id: Long) {
        _activePhotoId.value = id
    }

    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("savedPhotoUri", null).collect { uriString ->
                if (!uriString.isNullOrEmpty()) {
                    val newPhotoId = try {
                        android.content.ContentUris.parseId(android.net.Uri.parse(uriString))
                    } catch (e: Exception) {
                        -1L
                    }
                    if (newPhotoId != -1L) {
                        _activePhotoId.value = newPhotoId
                    }
                }
            }
        }
        viewModelScope.launch {
            com.gallery.app.data.worker.MediaStoreObserver.immediateRefreshFlow
                .collect {
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(ViewerUiEvent.RefreshMedia)
                }
        }
        viewModelScope.launch {
            com.gallery.app.data.worker.MediaStoreObserver.mediaStoreChanges
                .debounce(500L)
                .collect {
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(ViewerUiEvent.RefreshMedia)
                }
        }
    }

    val photosState: Flow<PagingData<PhotoItem>> = _refreshTrigger.flatMapLatest {
        if (bucketId != null) {
            mediaRepository.getPhotosByBucket(bucketId)
        } else {
            mediaRepository.getPhotos()
        }
    }.cachedIn(viewModelScope)

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

