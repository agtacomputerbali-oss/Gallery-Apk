package com.gallery.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharePhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(uris: List<Uri>): Intent? {
        if (uris.isEmpty()) return null

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(intent, "Bagikan Foto").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
