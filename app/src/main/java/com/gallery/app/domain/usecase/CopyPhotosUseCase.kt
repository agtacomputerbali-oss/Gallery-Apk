package com.gallery.app.domain.usecase

import android.net.Uri
import com.gallery.app.domain.model.CopyMoveResult
import com.gallery.app.domain.repository.MediaRepository
import javax.inject.Inject

class CopyPhotosUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(uris: List<Uri>, targetFolderName: String): CopyMoveResult {
        if (uris.isEmpty()) return CopyMoveResult.Error("Tidak ada foto yang dipilih")
        if (targetFolderName.isBlank()) return CopyMoveResult.Error("Nama folder tujuan tidak valid")
        return repository.copyPhotosToFolder(uris, targetFolderName)
    }
}
