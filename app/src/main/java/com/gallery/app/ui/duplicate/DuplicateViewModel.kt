package com.gallery.app.ui.duplicate

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.data.local.entity.CachedPhotoEntity
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.repository.MediaRepository
import com.gallery.app.domain.repository.PhotoCacheRepository
import com.gallery.app.util.PHashCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val photoCacheRepository: PhotoCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DuplicateUiState>(DuplicateUiState.Loading)
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        loadDuplicates()
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.value = DuplicateUiState.Loading
            try {
                val hashedPhotos = photoCacheRepository.getAllHashedPhotos()
                if (hashedPhotos.isEmpty()) {
                    photoCacheRepository.triggerPHashIndexing()
                    _uiState.value = DuplicateUiState.Empty
                    return@launch
                }

                val groups = withContext(Dispatchers.Default) {
                    groupSimilarPhotos(hashedPhotos)
                }

                if (groups.isEmpty()) {
                    _uiState.value = DuplicateUiState.Empty
                } else {
                    val totalDuplicates = groups.sumOf { it.duplicates.size }
                    _uiState.value = DuplicateUiState.Success(
                        groups = groups,
                        totalDuplicateCount = totalDuplicates
                    )
                    refreshSelectedState()
                }
            } catch (e: Exception) {
                _uiState.value = DuplicateUiState.Error(e.localizedMessage ?: "Gagal memproses duplikat")
            }
        }
    }

    fun triggerAnalysis() {
        photoCacheRepository.triggerPHashIndexing()
        _uiState.value = DuplicateUiState.Analyzing
    }

    fun togglePhotoSelection(photoId: Long) {
        _selectedIds.update { current ->
            if (current.contains(photoId)) {
                current - photoId
            } else {
                current + photoId
            }
        }
        refreshSelectedState()
    }

    private fun refreshSelectedState() {
        val selected = _selectedIds.value
        _uiState.update { currentState ->
            if (currentState is DuplicateUiState.Success) {
                val updatedGroups = currentState.groups.map { group ->
                    val groupSelected = group.duplicates.map { it.id }.filter { selected.contains(it) }.toSet()
                    group.copy(selectedPhotoIds = groupSelected)
                }
                currentState.copy(groups = updatedGroups)
            } else {
                currentState
            }
        }
    }

    fun bulkCleanup(
        onIntentSenderReady: (IntentSender) -> Unit,
        onDeleteSuccess: () -> Unit
    ) {
        val currentState = _uiState.value
        if (currentState is DuplicateUiState.Success) {
            val duplicateUris = currentState.groups.flatMap { group ->
                group.duplicates.map { it.uri }
            }

            if (duplicateUris.isEmpty()) return

            viewModelScope.launch {
                val intentSender = mediaRepository.createDeleteIntentSender(duplicateUris)
                if (intentSender != null) {
                    onIntentSenderReady(intentSender)
                } else {
                    onDeleteSuccess()
                    loadDuplicates()
                }
            }
        }
    }

    fun deleteSelected(
        onIntentSenderReady: (IntentSender) -> Unit,
        onDeleteSuccess: () -> Unit
    ) {
        val currentState = _uiState.value
        val selected = _selectedIds.value
        if (currentState is DuplicateUiState.Success && selected.isNotEmpty()) {
            val allPhotosMap = currentState.groups.flatMap { it.duplicates + it.representativePhoto }
                .associateBy { it.id }

            val selectedUris = selected.mapNotNull { allPhotosMap[it]?.uri }
            if (selectedUris.isEmpty()) return

            viewModelScope.launch {
                val intentSender = mediaRepository.createDeleteIntentSender(selectedUris)
                if (intentSender != null) {
                    onIntentSenderReady(intentSender)
                } else {
                    _selectedIds.value = emptySet()
                    onDeleteSuccess()
                    loadDuplicates()
                }
            }
        }
    }

    fun onActionCompleted(isSuccess: Boolean) {
        if (isSuccess) {
            _selectedIds.value = emptySet()
            loadDuplicates()
        }
    }

    private fun groupSimilarPhotos(photos: List<CachedPhotoEntity>): List<DuplicateGroup> {
        val targetPhotos = if (photos.size > MAX_DUPLICATE_SCAN_LIMIT) {
            photos.take(MAX_DUPLICATE_SCAN_LIMIT)
        } else {
            photos
        }

        val visited = BooleanArray(targetPhotos.size)
        val result = mutableListOf<DuplicateGroup>()

        for (i in targetPhotos.indices) {
            if (visited[i]) continue
            val current = targetPhotos[i]
            val currentHash = current.pHash ?: continue

            val cluster = mutableListOf<CachedPhotoEntity>()
            cluster.add(current)
            visited[i] = true

            for (j in i + 1 until targetPhotos.size) {
                if (visited[j]) continue
                val next = targetPhotos[j]
                val nextHash = next.pHash ?: continue

                if (PHashCalculator.hammingDistance(currentHash, nextHash) <= 10) {
                    cluster.add(next)
                    visited[j] = true
                }
            }

            if (cluster.size > 1) {
                // Urutkan berdasarkan ukuran file terbesar -> index 0 adalah representative (yang paling berkualitas/besar)
                val sorted = cluster.sortedByDescending { it.size }
                val rep = sorted[0].toPhotoItem()
                val dups = sorted.drop(1).map { it.toPhotoItem() }

                result.add(
                    DuplicateGroup(
                        id = "group_${rep.id}",
                        representativePhoto = rep,
                        duplicates = dups
                    )
                )
            }
        }

        return result
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
        private const val MAX_DUPLICATE_SCAN_LIMIT = 1000
    }
}
