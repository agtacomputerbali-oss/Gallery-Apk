package com.gallery.app.ui.album

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.repository.PhotoCacheRepository
import com.gallery.app.domain.usecase.GetAlbumsUseCase
import com.gallery.app.domain.usecase.HidePhotosUseCase
import com.gallery.app.domain.usecase.HideResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.gallery.app.domain.usecase.CreateFolderUseCase
import javax.inject.Inject

sealed interface AlbumListUiEvent {
    data class ShowSnackbar(val message: String) : AlbumListUiEvent
    data class LaunchHideConfirmation(val intentSender: IntentSender) : AlbumListUiEvent
    data class LaunchDeleteConfirmation(val intentSender: IntentSender) : AlbumListUiEvent
    data object RefreshAlbums : AlbumListUiEvent
}

data class AlbumMultiSelectState(
    val isSelectionMode: Boolean = false,
    val selectedAlbumIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedAlbumIds.size
}

@HiltViewModel
class AlbumListViewModel @Inject constructor(
    getAlbumsUseCase: GetAlbumsUseCase,
    photoCacheRepository: PhotoCacheRepository,
    private val mediaRepository: MediaRepository,
    private val hidePhotosUseCase: HidePhotosUseCase,
    private val createFolderUseCase: CreateFolderUseCase
) : ViewModel() {

    val uiState: StateFlow<AlbumListUiState> = combine(
        getAlbumsUseCase(),
        photoCacheRepository.getSmartAlbums()
    ) { manualAlbums, smartAlbums ->
        AlbumListUiState.Success(
            albums = manualAlbums,
            smartAlbums = smartAlbums
        ) as AlbumListUiState
    }
        .catch { e ->
            emit(AlbumListUiState.Error(e.localizedMessage ?: "Gagal memuat album"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlbumListUiState.Loading
        )

    private val _multiSelectState = MutableStateFlow(AlbumMultiSelectState())
    val multiSelectState: StateFlow<AlbumMultiSelectState> = _multiSelectState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AlbumListUiEvent>()
    val uiEvent: SharedFlow<AlbumListUiEvent> = _uiEvent.asSharedFlow()

    private var pendingRollbackVaultPaths: List<String> = emptyList()

    fun enterSelectionMode(albumId: Long) {
        _multiSelectState.update {
            AlbumMultiSelectState(
                isSelectionMode = true,
                selectedAlbumIds = setOf(albumId)
            )
        }
    }

    fun toggleAlbumSelection(albumId: Long) {
        _multiSelectState.update { currentState ->
            val updated = currentState.selectedAlbumIds.toMutableSet()
            if (updated.contains(albumId)) {
                updated.remove(albumId)
            } else {
                updated.add(albumId)
            }
            if (updated.isEmpty()) {
                AlbumMultiSelectState(isSelectionMode = false, selectedAlbumIds = emptySet())
            } else {
                currentState.copy(selectedAlbumIds = updated)
            }
        }
    }

    fun exitSelectionMode() {
        _multiSelectState.value = AlbumMultiSelectState()
    }

    fun hideSelectedAlbums() {
        viewModelScope.launch {
            val selectedIds = _multiSelectState.value.selectedAlbumIds.toList()
            if (selectedIds.isEmpty()) return@launch

            val items = mediaRepository.getPhotoUrisWithBucketInfo(selectedIds)
            if (items.isEmpty()) {
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Tidak ada foto untuk disembunyikan di album ini"))
                exitSelectionMode()
                return@launch
            }

            when (val result = hidePhotosUseCase.hideItems(items)) {
                is HideResult.NeedsConfirmation -> {
                    pendingRollbackVaultPaths = result.copiedVaultPaths
                    _uiEvent.emit(AlbumListUiEvent.LaunchHideConfirmation(result.intentSender))
                }
                is HideResult.Success -> {
                    val count = result.count
                    exitSelectionMode()
                    _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                    _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("$count foto dari album berhasil dipindahkan ke Vault"))
                }
                is HideResult.PartialFailure -> {
                    val sCount = result.successCount
                    val fCount = result.failCount
                    exitSelectionMode()
                    _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                    _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("$sCount foto dipindahkan ke Vault, $fCount gagal"))
                }
                is HideResult.Error -> {
                    _uiEvent.emit(AlbumListUiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun onHideCompleted(isSuccess: Boolean) {
        viewModelScope.launch {
            if (isSuccess) {
                pendingRollbackVaultPaths = emptyList()
                exitSelectionMode()
                _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Foto-foto album berhasil dipindahkan ke Vault"))
            } else {
                hidePhotosUseCase.rollbackVaultItems(pendingRollbackVaultPaths)
                pendingRollbackVaultPaths = emptyList()
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Penyembunyian album ke Vault dibatalkan"))
            }
        }
    }

    fun deleteSelectedAlbums() {
        viewModelScope.launch {
            val selectedIds = _multiSelectState.value.selectedAlbumIds.toList()
            if (selectedIds.isEmpty()) return@launch

            val uris = mediaRepository.getPhotoUrisByBuckets(selectedIds)
            if (uris.isEmpty()) {
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Tidak ada foto untuk dihapus di album ini"))
                exitSelectionMode()
                return@launch
            }

            val intentSender = mediaRepository.createDeleteIntentSender(uris)
            if (intentSender != null) {
                _uiEvent.emit(AlbumListUiEvent.LaunchDeleteConfirmation(intentSender))
            } else {
                exitSelectionMode()
                _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("${selectedIds.size} album berhasil dihapus"))
            }
        }
    }

    fun onDeleteCompleted(isSuccess: Boolean) {
        viewModelScope.launch {
            if (isSuccess) {
                exitSelectionMode()
                _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Album berhasil dihapus"))
            } else {
                _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Penghapusan album dibatalkan"))
            }
        }
    }

    fun createNewFolder(folderName: String) {
        viewModelScope.launch {
            createFolderUseCase(folderName)
                .onSuccess {
                    _uiEvent.emit(AlbumListUiEvent.RefreshAlbums)
                    _uiEvent.emit(AlbumListUiEvent.ShowSnackbar("Folder '$folderName' berhasil dibuat"))
                }
                .onFailure { error ->
                    _uiEvent.emit(AlbumListUiEvent.ShowSnackbar(error.message ?: "Gagal membuat folder"))
                }
        }
    }
}
