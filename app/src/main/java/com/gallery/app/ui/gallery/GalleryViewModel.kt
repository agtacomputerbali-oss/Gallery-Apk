package com.gallery.app.ui.gallery

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gallery.app.data.repository.ThemeRepository
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.model.SortOption
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.usecase.DeletePhotosUseCase
import com.gallery.app.domain.usecase.HidePhotosUseCase
import com.gallery.app.domain.usecase.HideResult
import com.gallery.app.domain.usecase.SharePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import com.gallery.app.domain.model.MediaTypeFilter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

import com.gallery.app.domain.model.CopyMoveResult
import com.gallery.app.domain.model.FolderItem
import com.gallery.app.domain.usecase.CopyPhotosUseCase
import com.gallery.app.domain.usecase.CreateFolderUseCase
import com.gallery.app.domain.usecase.GetFolderListUseCase
import com.gallery.app.domain.usecase.MovePhotosUseCase

import androidx.paging.insertSeparators
import androidx.paging.map
import com.gallery.app.ui.gallery.model.GalleryItemModel
import kotlinx.coroutines.flow.map
import java.util.Calendar

sealed class GalleryUiEvent {
    data class ShowSnackbar(val message: String) : GalleryUiEvent()
    data class LaunchHideConfirmation(val intentSender: IntentSender) : GalleryUiEvent()
    data class LaunchDeleteConfirmation(val intentSender: IntentSender) : GalleryUiEvent()
    data object RefreshMedia : GalleryUiEvent()
}

fun getSectionTitle(photo: PhotoItem, sortOption: SortOption): String {
    val dateMs = photo.dateTaken
    if (dateMs <= 0) return "Lainnya"
    val epochMs = if (dateMs < 10_000_000_000L) dateMs * 1000L else dateMs
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    return when (sortOption) {
        SortOption.MONTH_DESC -> {
            val m = monthNames[cal.get(Calendar.MONTH)]
            val y = cal.get(Calendar.YEAR)
            "$m $y"
        }
        SortOption.YEAR_DESC -> "Tahun ${cal.get(Calendar.YEAR)}"
        SortOption.DATE_TAKEN_DESC, SortOption.DATE_TAKEN_ASC -> {
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val m = monthNames[cal.get(Calendar.MONTH)]
            val y = cal.get(Calendar.YEAR)
            "$d $m $y"
        }
        SortOption.DISPLAY_NAME_ASC, SortOption.DISPLAY_NAME_DESC -> {
            val firstChar = photo.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) "$firstChar" else "#"
        }
        SortOption.SIZE_DESC, SortOption.SIZE_ASC -> {
            val sizeMb = photo.size / (1024 * 1024)
            when {
                sizeMb >= 10 -> "> 10 MB"
                sizeMb >= 5 -> "5 MB - 10 MB"
                sizeMb >= 1 -> "1 MB - 5 MB"
                else -> "< 1 MB"
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val sharePhotosUseCase: SharePhotosUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val hidePhotosUseCase: HidePhotosUseCase,
    private val copyPhotosUseCase: CopyPhotosUseCase,
    private val movePhotosUseCase: MovePhotosUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFolderListUseCase: GetFolderListUseCase,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            com.gallery.app.data.worker.MediaStoreObserver.immediateRefreshFlow
                .collect {
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
        }
        viewModelScope.launch {
            com.gallery.app.data.worker.MediaStoreObserver.mediaStoreChanges
                .debounce(500L)
                .collect {
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
        }
    }

    private val _folderList = MutableStateFlow<List<FolderItem>>(emptyList())
    val folderList: StateFlow<List<FolderItem>> = _folderList.asStateFlow()

    fun loadFolderList() {
        viewModelScope.launch {
            _folderList.value = getFolderListUseCase()
        }
    }

    fun copySelectedPhotos(targetFolderName: String) {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            if (uris.isEmpty()) return@launch
            when (val result = copyPhotosUseCase(uris, targetFolderName)) {
                is CopyMoveResult.Success -> {
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.count} foto berhasil disalin ke '${result.targetFolderName}'"))
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
                is CopyMoveResult.NeedsDeleteConfirmation -> {
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.count} foto berhasil disalin ke '${result.targetFolderName}'"))
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
                is CopyMoveResult.PartialFailure -> {
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.successCount} foto disalin, ${result.failCount} gagal"))
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
                is CopyMoveResult.Error -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar(result.message))
                }
                is CopyMoveResult.SameFolderError -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("Tidak dapat menyalin ke folder yang sama"))
                }
            }
        }
    }

    fun moveSelectedPhotos(targetFolderName: String) {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            if (uris.isEmpty()) return@launch
            when (val result = movePhotosUseCase(uris, targetFolderName)) {
                is CopyMoveResult.Success -> {
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.count} foto dipindahkan ke '${result.targetFolderName}'"))
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
                is CopyMoveResult.NeedsDeleteConfirmation -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.count} foto disalin. Meminta konfirmasi hapus foto asal..."))
                    _uiEvent.emit(GalleryUiEvent.LaunchDeleteConfirmation(result.intentSender))
                }
                is CopyMoveResult.PartialFailure -> {
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("${result.successCount} foto dipindahkan, ${result.failCount} gagal"))
                    _refreshTrigger.value = System.currentTimeMillis()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                }
                is CopyMoveResult.Error -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar(result.message))
                }
                is CopyMoveResult.SameFolderError -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("Tidak dapat memindahkan ke folder yang sama"))
                }
            }
        }
    }

    fun createNewFolder(folderName: String) {
        viewModelScope.launch {
            createFolderUseCase(folderName)
                .onSuccess {
                    loadFolderList()
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("Folder '$folderName' berhasil dibuat"))
                }
                .onFailure { error ->
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar(error.message ?: "Gagal membuat folder"))
                }
        }
    }

    val galleryGridColumns: StateFlow<Int> = themeRepository.galleryGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun setGridColumns(count: Int) {
        viewModelScope.launch {
            themeRepository.setGalleryGridColumns(count)
        }
    }

    fun cycleGridColumns() {
        val current = galleryGridColumns.value
        val next = if (current >= 6) 3 else current + 1
        setGridColumns(next)
    }

    private val _sortOption = MutableStateFlow(SortOption.DATE_TAKEN_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _mediaTypeFilter = MutableStateFlow(MediaTypeFilter.ALL)
    val mediaTypeFilter: StateFlow<MediaTypeFilter> = _mediaTypeFilter.asStateFlow()

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        _mediaTypeFilter.value = filter
    }

    val photosState: Flow<PagingData<GalleryItemModel>> = combine(_sortOption, _mediaTypeFilter, _refreshTrigger) { option, filter, trigger ->
        Triple(option, filter, trigger)
    }.flatMapLatest { (option, filter, _) ->
        mediaRepository.getPhotos(option, filter)
            .map { pagingData ->
                pagingData.map { photo -> GalleryItemModel.PhotoModel(photo) }
                    .insertSeparators { before: GalleryItemModel.PhotoModel?, after: GalleryItemModel.PhotoModel? ->
                        val beforePhoto = before?.photo
                        val afterPhoto = after?.photo

                        if (afterPhoto != null) {
                            val afterGroupKey = getSectionTitle(afterPhoto, option)
                            val beforeGroupKey = beforePhoto?.let { getSectionTitle(it, option) }

                            if (beforeGroupKey != afterGroupKey) {
                                GalleryItemModel.HeaderModel(
                                    id = "header_$afterGroupKey",
                                    title = afterGroupKey,
                                    count = 0,
                                    dateGroupKey = afterGroupKey,
                                    photos = emptyList()
                                )
                            } else null
                        } else null
                    }
            }
    }.cachedIn(viewModelScope)

    private val _multiSelectState = MutableStateFlow(MultiSelectState())
    val multiSelectState: StateFlow<MultiSelectState> = _multiSelectState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<GalleryUiEvent>()
    val uiEvent: SharedFlow<GalleryUiEvent> = _uiEvent.asSharedFlow()

    private var pendingRollbackVaultPaths: List<String> = emptyList()

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun enterSelectionModeDirect() {
        _multiSelectState.update {
            it.copy(isSelectionMode = true)
        }
    }

    fun toggleSelectAll(loadedPhotos: List<PhotoItem>) {
        _multiSelectState.update { currentState ->
            val currentSelected = currentState.selectedPhotos
            if (currentSelected.containsAll(loadedPhotos) && loadedPhotos.isNotEmpty()) {
                currentState.copy(selectedPhotos = emptySet())
            } else {
                currentState.copy(isSelectionMode = true, selectedPhotos = loadedPhotos.toSet())
            }
        }
    }

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

    fun toggleSectionSelection(photos: List<PhotoItem>) {
        _multiSelectState.update { currentState ->
            val updated = currentState.selectedPhotos.toMutableSet()
            val allSelected = updated.containsAll(photos) && photos.isNotEmpty()
            if (allSelected) {
                updated.removeAll(photos.toSet())
            } else {
                updated.addAll(photos)
            }
            val isMode = updated.isNotEmpty()
            currentState.copy(isSelectionMode = isMode, selectedPhotos = updated)
        }
    }

    fun addPhotosToSelection(photos: List<PhotoItem>) {
        if (photos.isEmpty()) return
        _multiSelectState.update { currentState ->
            val updated = currentState.selectedPhotos.toMutableSet()
            updated.addAll(photos)
            currentState.copy(isSelectionMode = true, selectedPhotos = updated)
        }
    }

    fun exitSelectionMode() {
        _multiSelectState.value = MultiSelectState()
    }

    fun getShareIntent(): Intent? {
        val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
        return sharePhotosUseCase(uris)
    }

    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            val intentSender = deletePhotosUseCase(uris)
            if (intentSender != null) {
                _uiEvent.emit(GalleryUiEvent.LaunchDeleteConfirmation(intentSender))
            } else {
                exitSelectionMode()
                _uiEvent.emit(GalleryUiEvent.RefreshMedia)
            }
        }
    }

    fun onDeleteCompleted(isSuccess: Boolean) {
        if (isSuccess) {
            exitSelectionMode()
        }
    }

    fun hideSelectedPhotos() {
        viewModelScope.launch {
            val uris = _multiSelectState.value.selectedPhotos.map { it.uri }
            if (uris.isEmpty()) return@launch

            when (val result = hidePhotosUseCase(uris)) {
                is HideResult.NeedsConfirmation -> {
                    pendingRollbackVaultPaths = result.copiedVaultPaths
                    _uiEvent.emit(GalleryUiEvent.LaunchHideConfirmation(result.intentSender))
                }
                is HideResult.Success -> {
                    val count = result.count
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("$count foto berhasil dipindahkan ke Vault"))
                }
                is HideResult.PartialFailure -> {
                    val sCount = result.successCount
                    val fCount = result.failCount
                    exitSelectionMode()
                    _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar("$sCount foto dipindahkan ke Vault, $fCount gagal"))
                }
                is HideResult.Error -> {
                    _uiEvent.emit(GalleryUiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun onHideCompleted(isSuccess: Boolean) {
        viewModelScope.launch {
            if (isSuccess) {
                pendingRollbackVaultPaths = emptyList()
                val count = _multiSelectState.value.selectedCount
                exitSelectionMode()
                _uiEvent.emit(GalleryUiEvent.RefreshMedia)
                _uiEvent.emit(GalleryUiEvent.ShowSnackbar("$count foto berhasil dipindahkan ke Vault"))
            } else {
                hidePhotosUseCase.rollbackVaultItems(pendingRollbackVaultPaths)
                pendingRollbackVaultPaths = emptyList()
                _uiEvent.emit(GalleryUiEvent.ShowSnackbar("Operasi pemindahan ke Vault dibatalkan"))
            }
        }
    }
}
