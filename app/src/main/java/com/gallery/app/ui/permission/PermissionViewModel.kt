package com.gallery.app.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Idle)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun checkPermissionStatus(context: Context) {
        val permission = getRequiredPermission()
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            _permissionState.value = PermissionState.Granted
        } else {
            _permissionState.value = PermissionState.Denied(shouldShowRationale = false)
        }
    }

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        if (isGranted) {
            _permissionState.value = PermissionState.Granted
        } else {
            _permissionState.value = PermissionState.Denied(shouldShowRationale = shouldShowRationale)
        }
    }
}
