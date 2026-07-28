package com.gallery.app.ui.permission

sealed interface PermissionState {
    data object Idle : PermissionState
    data object Granted : PermissionState
    data class Denied(val shouldShowRationale: Boolean) : PermissionState
}
