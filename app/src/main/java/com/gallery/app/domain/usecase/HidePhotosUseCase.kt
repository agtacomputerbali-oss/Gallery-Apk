package com.gallery.app.domain.usecase

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.gallery.app.di.IoDispatcher
import com.gallery.app.domain.model.VaultItem
import com.gallery.app.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

import com.gallery.app.domain.model.UriWithFolderInfo
import android.os.Build

sealed class HideResult {
    data class NeedsConfirmation(
        val intentSender: IntentSender,
        val copiedVaultPaths: List<String>
    ) : HideResult()

    data class Success(val count: Int) : HideResult()
    data class PartialFailure(val successCount: Int, val failCount: Int) : HideResult()
    data class Error(val message: String) : HideResult()
}

class HidePhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(uris: List<Uri>): HideResult = withContext(ioDispatcher) {
        val items = uris.map { uri ->
            val meta = getUriMetadata(uri)
            UriWithFolderInfo(uri = uri, folderName = meta.folderName ?: "Umum", relativePath = meta.relativePath)
        }
        hideItems(items)
    }

    suspend fun hideItems(items: List<UriWithFolderInfo>): HideResult = withContext(ioDispatcher) {
        if (items.isEmpty()) return@withContext HideResult.Success(0)

        val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
        val indexFile = File(vaultDir, "vault_index.json")

        val existingIndexJson = if (indexFile.exists()) {
            try { JSONArray(indexFile.readText()) } catch (e: Exception) { JSONArray() }
        } else JSONArray()

        val copiedVaultPaths = mutableListOf<String>()
        val uris = mutableListOf<Uri>()
        var successCount = 0
        var failCount = 0

        for (itemInfo in items) {
            val uri = itemInfo.uri
            try {
                val meta = getUriMetadata(uri)
                val folderName = itemInfo.folderName.ifBlank { meta.folderName ?: "Umum" }
                val relativePath = itemInfo.relativePath ?: meta.relativePath

                val itemId = UUID.randomUUID().toString()
                val targetFile = File(vaultDir, "vault_$itemId.bin")

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    failCount++
                    continue
                }

                inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val item = VaultItem(
                    id = itemId,
                    originalName = meta.displayName,
                    mimeType = meta.mimeType,
                    vaultFilePath = targetFile.absolutePath,
                    dateAdded = System.currentTimeMillis(),
                    folderName = folderName,
                    relativePath = relativePath
                )

                val jsonObj = JSONObject().apply {
                    put("id", item.id)
                    put("originalName", item.originalName)
                    put("mimeType", item.mimeType)
                    put("vaultFilePath", item.vaultFilePath)
                    put("dateAdded", item.dateAdded)
                    item.folderName?.let { put("folderName", it) }
                    item.relativePath?.let { put("relativePath", it) }
                }
                existingIndexJson.put(jsonObj)
                copiedVaultPaths.add(targetFile.absolutePath)
                uris.add(uri)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy photo to vault", e)
                failCount++
            }
        }

        if (successCount > 0) {
            indexFile.writeText(existingIndexJson.toString())
        }

        if (successCount == 0) {
            return@withContext HideResult.Error("Gagal menyalin berkas ke Vault.")
        }

        val deleteIntentSender = mediaRepository.createMediaDeleteRequest(uris)
        if (deleteIntentSender != null) {
            HideResult.NeedsConfirmation(
                intentSender = deleteIntentSender,
                copiedVaultPaths = copiedVaultPaths
            )
        } else {
            if (failCount > 0) {
                HideResult.PartialFailure(successCount = successCount, failCount = failCount)
            } else {
                HideResult.Success(count = successCount)
            }
        }
    }

    suspend fun rollbackVaultItems(vaultPaths: List<String>) = withContext(ioDispatcher) {
        if (vaultPaths.isEmpty()) return@withContext
        val vaultDir = File(context.filesDir, "vault")
        val indexFile = File(vaultDir, "vault_index.json")

        for (path in vaultPaths) {
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete file during vault rollback: $path", e)
            }
        }

        if (indexFile.exists()) {
            try {
                val array = JSONArray(indexFile.readText())
                val newArray = JSONArray()
                val targetPathsSet = vaultPaths.toSet()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (!targetPathsSet.contains(obj.optString("vaultFilePath"))) {
                        newArray.put(obj)
                    }
                }
                indexFile.writeText(newArray.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update vault index during rollback", e)
            }
        }
    }

    private data class UriFullMetadata(
        val displayName: String,
        val mimeType: String,
        val folderName: String?,
        val relativePath: String?
    )

    private fun getUriMetadata(uri: Uri): UriFullMetadata {
        var displayName = "hidden_${System.currentTimeMillis()}.jpg"
        var mimeType = "image/jpeg"
        var folderName: String? = null
        var relativePath: String? = null

        try {
            val projection = mutableListOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.MediaColumns.RELATIVE_PATH)
                }
            }.toTypedArray()

            val cursor = context.contentResolver.query(
                uri, projection, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameCol = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val bucketCol = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val relativeCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        it.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    } else -1

                    if (nameCol != -1) {
                        it.getString(nameCol)?.let { name -> if (name.isNotBlank()) displayName = name }
                    }
                    if (mimeCol != -1) {
                        it.getString(mimeCol)?.let { mime -> if (mime.isNotBlank()) mimeType = mime }
                    }
                    if (bucketCol != -1) {
                        it.getString(bucketCol)?.let { bName -> if (bName.isNotBlank()) folderName = bName }
                    }
                    if (relativeCol != -1) {
                        it.getString(relativeCol)?.let { rPath -> if (rPath.isNotBlank()) relativePath = rPath }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read uri metadata", e)
        }
        return UriFullMetadata(displayName, mimeType, folderName, relativePath)
    }

    companion object {
        private const val TAG = "HidePhotosUseCase"
    }
}
