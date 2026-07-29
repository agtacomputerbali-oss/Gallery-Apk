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

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val list = mutableListOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
            list.toTypedArray()
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkPermissionStatus(context: Context) {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasSelected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            (hasImages && hasVideo) || hasSelected
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            hasImages || hasVideo
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (isGranted) {
            _permissionState.value = PermissionState.Granted
        } else {
            _permissionState.value = PermissionState.Denied(shouldShowRationale = false)
        }
    }

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean = false) {
        if (isGranted) {
            _permissionState.value = PermissionState.Granted
        } else {
            _permissionState.value = PermissionState.Denied(shouldShowRationale = shouldShowRationale)
        }
    }
}
