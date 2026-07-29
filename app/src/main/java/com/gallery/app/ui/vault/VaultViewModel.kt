package com.gallery.app.ui.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.gallery.app.di.IoDispatcher
import com.gallery.app.di.VaultImageLoader
import com.gallery.app.domain.model.VaultItem
import com.gallery.app.domain.usecase.RestoreFromVaultUseCase
import com.gallery.app.util.BiometricHelper
import com.gallery.app.util.PinEncryptionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.gallery.app.data.repository.ThemeRepository
import com.gallery.app.domain.model.SortOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.gallery.app.domain.model.MediaTypeFilter
import java.util.Calendar
enum class VaultTab {
    ALL_PHOTOS, FOLDERS
}

data class VaultFolder(
    val folderName: String,
    val items: List<VaultItem>
) {
    val count: Int get() = items.size
    val coverFilePath: String get() = items.firstOrNull()?.vaultFilePath ?: ""
}

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isPinConfigured: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val vaultItems: List<VaultItem> = emptyList(),
    val selectedItems: Set<VaultItem> = emptySet(),
    val selectedFolderNames: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentTab: VaultTab = VaultTab.ALL_PHOTOS,
    val selectedFolderName: String? = null,
    val sortOption: SortOption = SortOption.DATE_TAKEN_DESC,
    val mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL
) {
    val selectedCount: Int get() = selectedItems.size + selectedFolderNames.size

    val vaultFolders: List<VaultFolder> get() {
        return vaultItems
            .groupBy { it.folderName.takeIf { f -> !f.isNullOrBlank() } ?: "Vault Utama" }
            .map { (name, list) -> VaultFolder(folderName = name, items = list) }
            .sortedBy { it.folderName }
    }

    val currentFolderItems: List<VaultItem> get() {
        val baseItems = if (selectedFolderName == null) vaultItems else {
            vaultItems.filter { (it.folderName.takeIf { f -> !f.isNullOrBlank() } ?: "Vault Utama") == selectedFolderName }
        }

        val typeFiltered = when (mediaTypeFilter) {
            MediaTypeFilter.PHOTOS_ONLY -> baseItems.filter { it.mimeType.startsWith("image/") }
            MediaTypeFilter.VIDEOS_ONLY -> baseItems.filter { it.mimeType.startsWith("video/") }
            MediaTypeFilter.ALL -> baseItems
        }

        return when (sortOption) {
            SortOption.DATE_TAKEN_DESC, SortOption.MONTH_DESC, SortOption.YEAR_DESC -> typeFiltered.sortedByDescending { it.dateAdded }
            SortOption.DATE_TAKEN_ASC -> typeFiltered.sortedBy { it.dateAdded }
            SortOption.DISPLAY_NAME_ASC -> typeFiltered.sortedBy { it.originalName.lowercase() }
            SortOption.DISPLAY_NAME_DESC -> typeFiltered.sortedByDescending { it.originalName.lowercase() }
            SortOption.SIZE_DESC, SortOption.SIZE_ASC -> typeFiltered
        }
    }

    val groupedCurrentFolderItems: Map<String, List<VaultItem>> get() {
        val items = currentFolderItems
        return items.groupBy { item ->
            val dateMs = item.dateAdded
            if (dateMs <= 0) "Lainnya" else {
                val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
                val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                when (sortOption) {
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
                    else -> "Foto Tersembunyi"
                }
            }
        }
    }
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val pinHelper: PinEncryptionHelper,
    private val biometricHelper: BiometricHelper,
    private val restoreFromVaultUseCase: RestoreFromVaultUseCase,
    private val themeRepository: ThemeRepository,
    @VaultImageLoader val vaultImageLoader: ImageLoader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    val vaultGridColumns: StateFlow<Int> = themeRepository.vaultGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    init {
        checkSecurityState()
    }

    private fun checkSecurityState() {
        val isPinConfigured = try { pinHelper.isPinSet() } catch (e: Exception) { false }
        val isBiometricAvailable = try { biometricHelper.isBiometricAvailable() } catch (e: Exception) { false }
        _uiState.update {
            it.copy(
                isPinConfigured = isPinConfigured,
                isBiometricAvailable = isBiometricAvailable
            )
        }
    }

    fun submitPin(pin: String) {
        var shouldLoadItems = false
        _uiState.update { currentState ->
            val remainingLockout = try { pinHelper.getRemainingLockoutTimeSeconds() } catch (e: Exception) { 0L }
            if (remainingLockout > 0) {
                return@update currentState.copy(errorMessage = "Terlalu banyak percobaan. Coba lagi dalam $remainingLockout detik.")
            }

            if (!currentState.isPinConfigured) {
                val saved = try { pinHelper.savePin(pin) } catch (e: Exception) { false }
                if (saved) {
                    shouldLoadItems = true
                    currentState.copy(isPinConfigured = true, isUnlocked = true, errorMessage = null)
                } else {
                    currentState.copy(errorMessage = "Gagal menyimpan PIN. Coba lagi.")
                }
            } else {
                val isCorrect = try { pinHelper.verifyPin(pin) } catch (e: Exception) { false }
                if (isCorrect) {
                    shouldLoadItems = true
                    currentState.copy(isUnlocked = true, errorMessage = null)
                } else {
                    val updatedLockout = try { pinHelper.getRemainingLockoutTimeSeconds() } catch (e: Exception) { 0L }
                    val msg = if (updatedLockout > 0) {
                        "Terlalu banyak percobaan. Coba lagi dalam $updatedLockout detik."
                    } else {
                        "PIN Salah. Coba lagi."
                    }
                    currentState.copy(errorMessage = msg)
                }
            }
        }
        if (shouldLoadItems) {
            loadVaultItems()
        }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        biometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                viewModelScope.launch {
                    _uiState.update { it.copy(isUnlocked = true, errorMessage = null) }
                    loadVaultItems()
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _uiState.update { it.copy(errorMessage = error) }
                }
            }
        )
    }

    fun loadVaultItems() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            val items = restoreFromVaultUseCase.getVaultItems()
            _uiState.update { it.copy(vaultItems = items, isLoading = false) }
        }
    }

    fun setTab(tab: VaultTab) {
        _uiState.update { it.copy(currentTab = tab, selectedFolderName = null, isSelectionMode = false, selectedItems = emptySet(), selectedFolderNames = emptySet()) }
    }

    fun openFolder(folderName: String) {
        _uiState.update { it.copy(selectedFolderName = folderName, isSelectionMode = false, selectedItems = emptySet(), selectedFolderNames = emptySet()) }
    }

    fun closeFolder() {
        _uiState.update { it.copy(selectedFolderName = null, isSelectionMode = false, selectedItems = emptySet(), selectedFolderNames = emptySet()) }
    }

    fun toggleFolderSelection(folderName: String) {
        _uiState.update { currentState ->
            val updated = currentState.selectedFolderNames.toMutableSet()
            if (updated.contains(folderName)) updated.remove(folderName) else updated.add(folderName)
            val isMode = updated.isNotEmpty() || currentState.selectedItems.isNotEmpty()
            currentState.copy(selectedFolderNames = updated, isSelectionMode = isMode)
        }
    }

    fun enterFolderSelectionMode(folderName: String) {
        _uiState.update {
            it.copy(isSelectionMode = true, selectedFolderNames = setOf(folderName))
        }
    }

    fun toggleItemSelection(item: VaultItem) {
        _uiState.update { currentState ->
            val updated = currentState.selectedItems.toMutableSet()
            if (updated.contains(item)) updated.remove(item) else updated.add(item)
            val isMode = updated.isNotEmpty() || currentState.selectedFolderNames.isNotEmpty()
            currentState.copy(selectedItems = updated, isSelectionMode = isMode)
        }
    }

    fun enterSelectionMode(item: VaultItem) {
        _uiState.update {
            it.copy(isSelectionMode = true, selectedItems = setOf(item))
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        _uiState.update { it.copy(mediaTypeFilter = filter) }
    }

    fun setGridColumns(count: Int) {
        viewModelScope.launch {
            themeRepository.setVaultGridColumns(count)
        }
    }

    fun cycleGridColumns() {
        val current = vaultGridColumns.value
        val next = if (current >= 6) 3 else current + 1
        setGridColumns(next)
    }

    fun toggleSectionSelection(items: List<VaultItem>) {
        _uiState.update { currentState ->
            val updated = currentState.selectedItems.toMutableSet()
            val allSelected = updated.containsAll(items) && items.isNotEmpty()
            if (allSelected) {
                updated.removeAll(items.toSet())
            } else {
                updated.addAll(items)
            }
            val isMode = updated.isNotEmpty() || currentState.selectedFolderNames.isNotEmpty()
            currentState.copy(selectedItems = updated, isSelectionMode = isMode)
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedItems = emptySet(), selectedFolderNames = emptySet()) }
    }

    private fun getAllSelectedVaultItems(): List<VaultItem> {
        val state = _uiState.value
        val itemsFromSelectedFolders = if (state.selectedFolderNames.isNotEmpty()) {
            state.vaultItems.filter { item ->
                val fName = item.folderName.takeIf { f -> !f.isNullOrBlank() } ?: "Vault Utama"
                state.selectedFolderNames.contains(fName)
            }
        } else emptyList()

        return (state.selectedItems + itemsFromSelectedFolders).distinctBy { it.id }
    }

    fun restoreSelectedItems(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val selected = getAllSelectedVaultItems()
            if (selected.isEmpty()) return@launch
            val success = restoreFromVaultUseCase(selected)
            if (success) {
                exitSelectionMode()
                loadVaultItems()
                onSuccess()
            }
        }
    }

    fun deleteSelectedItemsPermanently(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val selected = getAllSelectedVaultItems()
            if (selected.isEmpty()) return@launch
            restoreFromVaultUseCase.deleteFromVaultPermanently(selected)
            exitSelectionMode()
            loadVaultItems()
            onSuccess()
        }
    }

    fun lockVault() {
        _uiState.update { VaultUiState(isPinConfigured = pinHelper.isPinSet(), isBiometricAvailable = biometricHelper.isBiometricAvailable()) }
    }
}
