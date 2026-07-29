package com.gallery.app.domain.usecase

import com.gallery.app.domain.repository.MediaRepository
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(folderName: String): Result<Boolean> {
        val trimmed = folderName.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama folder tidak boleh kosong"))
        }
        val illegalCharsRegex = Regex("[/\\\\:*?\"<>|]")
        if (illegalCharsRegex.containsMatchIn(trimmed)) {
            return Result.failure(IllegalArgumentException("Nama folder mengandung karakter yang tidak diizinkan"))
        }
        val success = repository.createFolder(trimmed)
        return if (success) {
            Result.success(true)
        } else {
            Result.failure(Exception("Gagal membuat folder"))
        }
    }
}
