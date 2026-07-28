package com.gallery.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.gallery.app.domain.model.VaultItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

class HidePhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(uris: List<Uri>): List<VaultItem> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext emptyList()

        val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
        val indexFile = File(vaultDir, "vault_index.json")

        val existingIndexJson = if (indexFile.exists()) {
            try { JSONArray(indexFile.readText()) } catch (e: Exception) { JSONArray() }
        } else JSONArray()

        val hiddenItems = mutableListOf<VaultItem>()

        for (uri in uris) {
            try {
                val itemId = UUID.randomUUID().toString()
                val targetFile = File(vaultDir, "vault_$itemId.bin")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val item = VaultItem(
                    id = itemId,
                    originalName = "hidden_${System.currentTimeMillis()}.jpg",
                    mimeType = "image/jpeg",
                    vaultFilePath = targetFile.absolutePath,
                    dateAdded = System.currentTimeMillis()
                )

                val jsonObj = JSONObject().apply {
                    put("id", item.id)
                    put("originalName", item.originalName)
                    put("mimeType", item.mimeType)
                    put("vaultFilePath", item.vaultFilePath)
                    put("dateAdded", item.dateAdded)
                }
                existingIndexJson.put(jsonObj)
                hiddenItems.add(item)

                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        indexFile.writeText(existingIndexJson.toString())
        hiddenItems
    }
}
