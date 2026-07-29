package com.gallery.app.domain.model

import android.content.IntentSender

sealed class CopyMoveResult {
    data class Success(val count: Int, val targetFolderName: String) : CopyMoveResult()
    data class NeedsDeleteConfirmation(val intentSender: IntentSender, val count: Int, val targetFolderName: String) : CopyMoveResult()
    data class PartialFailure(val successCount: Int, val failCount: Int, val targetFolderName: String) : CopyMoveResult()
    data class Error(val message: String) : CopyMoveResult()
    object SameFolderError : CopyMoveResult()
}
