package com.gallery.app.domain.usecase

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.gallery.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.gallery.app.ui.editor.model.ExportFormat
import com.gallery.app.ui.editor.model.ExportOptions

class SaveEditedPhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        exportOptions: ExportOptions = ExportOptions(),
        sourceUri: Uri? = null
    ): Uri? = withContext(ioDispatcher) {
        val format = exportOptions.format
        val quality = exportOptions.quality.coerceIn(50, 100)
        val filename = "IMG_${System.currentTimeMillis()}.${format.extension}"
        val relativePath = getRelativePathFromSource(sourceUri)

        val currentTime = System.currentTimeMillis()
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
            put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000L)
            put(MediaStore.Images.Media.DATE_MODIFIED, currentTime / 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val compressFormat = when (format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }

        val resolver = context.contentResolver
        val uri = try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Primary path insert failed, falling back to Pictures/GalleryApp/", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GalleryApp/")
            }
            try {
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback insert also failed", fallbackException)
                null
            }
        }

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(compressFormat, quality, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                            val exif = android.media.ExifInterface(pfd.fileDescriptor)
                            val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                            val dateStr = sdf.format(java.util.Date(currentTime))
                            exif.setAttribute(android.media.ExifInterface.TAG_DATETIME, dateStr)
                            exif.setAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
                            exif.setAttribute(android.media.ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)
                            exif.saveAttributes()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to write EXIF date", e)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentValues.put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
                    contentValues.put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000L)
                    contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, currentTime / 1000L)
                    resolver.update(uri, contentValues, null, null)
                }
                resolver.notifyChange(uri, null)
                val filePath = queryFilePath(resolver, uri)
                if (!filePath.isNullOrBlank()) {
                    kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(filePath),
                                arrayOf(format.mimeType)
                            ) { path, scannedUri ->
                                Log.d(TAG, "MediaScanner scan completed for path: $path, uri: $scannedUri")
                                if (continuation.isActive) {
                                    continuation.resumeWith(Result.success(Unit))
                                }
                            }
                        }
                    }
                }
                uri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save edited photo bytes", e)
                resolver.delete(uri, null, null)
                null
            }
        } else {
            null
        }
    }

    private fun queryFilePath(resolver: android.content.ContentResolver, uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            resolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (idx != -1) c.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getRelativePathFromSource(sourceUri: Uri?): String {
        if (sourceUri == null) return Environment.DIRECTORY_PICTURES + "/GalleryApp/"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DATA
                )
                context.contentResolver.query(sourceUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val relIdx = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (relIdx != -1) {
                            val relPath = cursor.getString(relIdx)
                            if (!relPath.isNullOrBlank()) {
                                return sanitizeRelativePath(relPath)
                            }
                        }
                        val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataIdx != -1) {
                            val dataPath = cursor.getString(dataIdx)
                            if (!dataPath.isNullOrBlank()) {
                                val relFromData = extractRelativePathFromFullPath(dataPath)
                                if (!relFromData.isNullOrBlank()) {
                                    return sanitizeRelativePath(relFromData)
                                }
                            }
                        }
                    }
                }

                val id = try { ContentUris.parseId(sourceUri) } catch (e: Exception) { -1L }
                if (id != -1L) {
                    val filesUri = MediaStore.Files.getContentUri("external")
                    context.contentResolver.query(
                        filesUri,
                        arrayOf(MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.DATA),
                        "${MediaStore.Files.FileColumns._ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val relIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                            if (relIdx != -1) {
                                val relPath = cursor.getString(relIdx)
                                if (!relPath.isNullOrBlank()) {
                                    return sanitizeRelativePath(relPath)
                                }
                            }
                            val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                            if (dataIdx != -1) {
                                val dataPath = cursor.getString(dataIdx)
                                if (!dataPath.isNullOrBlank()) {
                                    val relFromData = extractRelativePathFromFullPath(dataPath)
                                    if (!relFromData.isNullOrBlank()) {
                                        return sanitizeRelativePath(relFromData)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal membaca RELATIVE_PATH foto asal", e)
            }
        }
        return Environment.DIRECTORY_PICTURES + "/GalleryApp/"
    }

    private fun sanitizeRelativePath(rawPath: String): String {
        val cleanPath = rawPath.trim().trimStart('/')
        if (cleanPath.isBlank()) {
            return Environment.DIRECTORY_PICTURES + "/GalleryApp/"
        }

        if (cleanPath.startsWith("Android/", ignoreCase = true)) {
            val subPath = cleanPath
                .removePrefix("Android/media/")
                .removePrefix("Android/data/")
                .removePrefix("Android/")
                .trimStart('/')

            return if (subPath.isNotBlank()) {
                "${Environment.DIRECTORY_PICTURES}/$subPath".ensureEndingSlash()
            } else {
                "${Environment.DIRECTORY_PICTURES}/GalleryApp/"
            }
        }

        val allowedPrefixes = listOf(
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOCUMENTS
        )

        val hasAllowedPrefix = allowedPrefixes.any { cleanPath.startsWith(it, ignoreCase = true) }
        return if (hasAllowedPrefix) {
            cleanPath.ensureEndingSlash()
        } else {
            "${Environment.DIRECTORY_PICTURES}/$cleanPath".ensureEndingSlash()
        }
    }

    private fun String.ensureEndingSlash(): String = if (endsWith("/")) this else "$this/"

    private fun extractRelativePathFromFullPath(fullPath: String): String? {
        val storagePrefix = "/storage/emulated/0/"
        val path = if (fullPath.startsWith(storagePrefix)) {
            fullPath.substring(storagePrefix.length)
        } else {
            val dcimIdx = fullPath.indexOf("/DCIM/")
            val picIdx = fullPath.indexOf("/Pictures/")
            val dlIdx = fullPath.indexOf("/Download/")
            val idx = when {
                dcimIdx != -1 -> dcimIdx
                picIdx != -1 -> picIdx
                dlIdx != -1 -> dlIdx
                else -> -1
            }
            if (idx != -1) fullPath.substring(idx + 1) else null
        }
        if (path != null) {
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash != -1) {
                val dir = path.substring(0, lastSlash + 1)
                if (dir.isNotBlank()) return dir
            }
        }
        return null
    }

    companion object {
        private const val TAG = "SaveEditedPhotoUseCase"
    }
}


