package com.gallery.app.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gallery.app.domain.model.VaultItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

class RestoreFromVaultUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(items: List<VaultItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext false

        val vaultDir = File(context.filesDir, "vault")
        val indexFile = File(vaultDir, "vault_index.json")

        val restoredIds = mutableSetOf<String>()

        for (item in items) {
            val vaultFile = File(item.vaultFilePath)
            if (!vaultFile.exists()) continue

            val filename = item.originalName.ifBlank { "restored_${System.currentTimeMillis()}.jpg" }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GalleryApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        vaultFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    vaultFile.delete()
                    restoredIds.add(item.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }
            }
        }

        if (indexFile.exists() && restoredIds.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(indexFile.readText())
                val newJsonArray = JSONArray()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (!restoredIds.contains(obj.optString("id"))) {
                        newJsonArray.put(obj)
                    }
                }
                indexFile.writeText(newJsonArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        restoredIds.isNotEmpty()
    }

    suspend fun getVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault")
        val indexFile = File(vaultDir, "vault_index.json")

        if (!indexFile.exists()) return@withContext emptyList()

        try {
            val jsonArray = JSONArray(indexFile.readText())
            val list = mutableListOf<VaultItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    VaultItem(
                        id = obj.getString("id"),
                        originalName = obj.getString("originalName"),
                        mimeType = obj.getString("mimeType"),
                        vaultFilePath = obj.getString("vaultFilePath"),
                        dateAdded = obj.getLong("dateAdded")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteFromVaultPermanently(items: List<VaultItem>): Boolean = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault")
        val indexFile = File(vaultDir, "vault_index.json")

        val deletedIds = mutableSetOf<String>()

        for (item in items) {
            val vaultFile = File(item.vaultFilePath)
            if (vaultFile.exists()) {
                vaultFile.delete()
            }
            deletedIds.add(item.id)
        }

        if (indexFile.exists() && deletedIds.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(indexFile.readText())
                val newJsonArray = JSONArray()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (!deletedIds.contains(obj.optString("id"))) {
                        newJsonArray.put(obj)
                    }
                }
                indexFile.writeText(newJsonArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        true
    }
}
