package com.gallery.app.ui.trash

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.usecase.PermanentDeleteUseCase
import com.gallery.app.domain.usecase.RestorePhotosUseCase
import com.gallery.app.ui.gallery.MultiSelectState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    private val restorePhotosUseCase: RestorePhotosUseCase,
    private val permanentDeleteUseCase: PermanentDeleteUseCase
) : ViewModel() {

    val trashedPhotosState: Flow<PagingData<PhotoItem>> = mediaRepository.getTrashedPhotos()
        .cachedIn(viewModelScope)

    private val _multiSelectState = MutableStateFlow(MultiSelectState())
    val multiSelectState: StateFlow<MultiSelectState> = _multiSelectState.asStateFlow()

    fun enterSelectionMode(photo: PhotoItem) {
        _multiSelectState.update {
            MultiSelectState(
                isSelectionMode = true,
                selectedPhotos = setOf(photo)
            )
        }
    }

    fun togglePhotoSelection(photo: PhotoItem) {
        _multiSelectState.update { currentState ->
            val updatedPhotos = currentState.selectedPhotos.toMutableSet()
            if (updatedPhotos.contains(photo)) {
                updatedPhotos.remove(photo)
            } else {
                updatedPhotos.add(photo)
            }
            if (updatedPhotos.isEmpty()) {
                MultiSelectState(isSelectionMode = false, selectedPhotos = emptySet())
            } else {
                currentState.copy(selectedPhotos = updatedPhotos)
            }
        }
    }

    fun exitSelectionMode() {
        _multiSelectState.value = MultiSelectState()
    }

    fun restoreSelectedPhotos(
        onIntentSenderReady: (IntentSender) -> Unit,
        onRestoreSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            val intentSender = restorePhotosUseCase(uris)
            if (intentSender != null) {
                onIntentSenderReady(intentSender)
            } else {
                exitSelectionMode()
                onRestoreSuccess()
            }
        }
    }

    fun permanentDeleteSelectedPhotos(
        onIntentSenderReady: (IntentSender) -> Unit,
        onDeleteSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            val intentSender = permanentDeleteUseCase(uris)
            if (intentSender != null) {
                onIntentSenderReady(intentSender)
            } else {
                exitSelectionMode()
                onDeleteSuccess()
            }
        }
    }

    fun onActionCompleted(isSuccess: Boolean) {
        if (isSuccess) {
            exitSelectionMode()
        }
    }
}
