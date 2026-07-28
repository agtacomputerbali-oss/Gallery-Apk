package com.gallery.app.domain.usecase

import com.gallery.app.domain.model.Album
import com.gallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<Album>> {
        return repository.getAlbums()
    }
}
