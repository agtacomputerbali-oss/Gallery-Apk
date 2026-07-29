package com.gallery.app.domain.usecase

import com.gallery.app.domain.model.FolderItem
import com.gallery.app.domain.repository.MediaRepository
import javax.inject.Inject

class GetFolderListUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): List<FolderItem> {
        return repository.getFolderList()
    }
}
