package com.gallery.app.domain.usecase

import android.content.IntentSender
import android.net.Uri
import com.gallery.app.domain.repository.MediaRepository
import javax.inject.Inject

class RestorePhotosUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(uris: List<Uri>): IntentSender? {
        return repository.restorePhotos(uris)
    }
}
