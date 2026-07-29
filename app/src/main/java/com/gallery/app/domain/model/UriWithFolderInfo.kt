package com.gallery.app.domain.model

import android.net.Uri

data class UriWithFolderInfo(
    val uri: Uri,
    val folderName: String,
    val relativePath: String? = null
)
