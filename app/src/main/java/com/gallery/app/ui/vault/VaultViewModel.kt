package com.gallery.app.ui.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.domain.model.VaultItem
import com.gallery.app.domain.usecase.RestoreFromVaultUseCase
import com.gallery.app.util.BiometricHelper
import com.gallery.app.util.PinEncryptionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isPinConfigured: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val vaultItems: List<VaultItem> = emptyList(),
    val selectedItems: Set<VaultItem> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCount: Int get() = selectedItems.size
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val pinHelper: PinEncryptionHelper,
    private val biometricHelper: BiometricHelper,
    private val restoreFromVaultUseCase: RestoreFromVaultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        checkSecurityState()
    }

    private fun checkSecurityState() {
        val isPinConfigured = pinHelper.isPinSet()
        val isBiometricAvailable = biometricHelper.isBiometricAvailable()
        _uiState.update {
            it.copy(
                isPinConfigured = isPinConfigured,
                isBiometricAvailable = isBiometricAvailable
            )
        }
    }

    fun submitPin(pin: String) {
        if (!_uiState.value.isPinConfigured) {
            pinHelper.savePin(pin)
            _uiState.update { it.copy(isPinConfigured = true, isUnlocked = true) }
            loadVaultItems()
        } else {
            if (pinHelper.verifyPin(pin)) {
                _uiState.update { it.copy(isUnlocked = true, errorMessage = null) }
                loadVaultItems()
            } else {
                _uiState.update { it.copy(errorMessage = "PIN Salah. Coba lagi.") }
            }
        }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        biometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                _uiState.update { it.copy(isUnlocked = true, errorMessage = null) }
                loadVaultItems()
            },
            onError = { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        )
    }

    fun loadVaultItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = restoreFromVaultUseCase.getVaultItems()
            _uiState.update { it.copy(vaultItems = items, isLoading = false) }
        }
    }

    fun toggleItemSelection(item: VaultItem) {
        _uiState.update { currentState ->
            val updated = currentState.selectedItems.toMutableSet()
            if (updated.contains(item)) updated.remove(item) else updated.add(item)
            val isMode = updated.isNotEmpty()
            currentState.copy(selectedItems = updated, isSelectionMode = isMode)
        }
    }

    fun enterSelectionMode(item: VaultItem) {
        _uiState.update {
            it.copy(isSelectionMode = true, selectedItems = setOf(item))
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedItems = emptySet()) }
    }

    fun restoreSelectedItems(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedItems.toList()
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
            val selected = _uiState.value.selectedItems.toList()
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
