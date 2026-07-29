package com.gallery.app.domain.model

data class VaultItem(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val vaultFilePath: String,
    val dateAdded: Long,
    val folderName: String? = null,
    val relativePath: String? = null
)
