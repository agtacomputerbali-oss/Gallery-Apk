package com.gallery.app.data.paging

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.domain.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.gallery.app.domain.model.MediaTypeFilter

class MediaPagingSource(
    private val context: Context,
    private val bucketId: Long? = null,
    private val isTrashed: Boolean = false,
    private val sortOption: SortOption = SortOption.DATE_TAKEN_DESC,
    private val mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL
) : PagingSource<Int, PhotoItem>() {

    override fun getRefreshKey(state: PagingState<Int, PhotoItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PhotoItem> {
        val page = params.key ?: 0
        val limit = params.loadSize
        val offset = page * limit

        val (sortColumn, isAscending) = when (sortOption) {
            SortOption.DATE_TAKEN_DESC, SortOption.MONTH_DESC, SortOption.YEAR_DESC -> MediaStore.Files.FileColumns.DATE_TAKEN to false
            SortOption.DATE_TAKEN_ASC -> MediaStore.Files.FileColumns.DATE_TAKEN to true
            SortOption.DISPLAY_NAME_ASC -> MediaStore.Files.FileColumns.DISPLAY_NAME to true
            SortOption.DISPLAY_NAME_DESC -> MediaStore.Files.FileColumns.DISPLAY_NAME to false
            SortOption.SIZE_DESC -> MediaStore.Files.FileColumns.SIZE to false
            SortOption.SIZE_ASC -> MediaStore.Files.FileColumns.SIZE to true
        }
        val sortDirectionStr = if (isAscending) "ASC" else "DESC"
        val sortDirectionInt = if (isAscending) {
            ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
        } else {
            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        }

        return withContext(Dispatchers.IO) {
            try {
                val photos = mutableListOf<PhotoItem>()
                val targetUri = when (mediaTypeFilter) {
                    MediaTypeFilter.PHOTOS_ONLY -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    MediaTypeFilter.VIDEOS_ONLY -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    MediaTypeFilter.ALL -> MediaStore.Files.getContentUri("external")
                }

                val projection = mutableListOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT,
                    MediaStore.MediaColumns.MIME_TYPE
                ).apply {
                    if (targetUri == MediaStore.Files.getContentUri("external")) {
                        add(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.MediaColumns.IS_TRASHED)
                    }
                }.toTypedArray()

                val selectionConditions = mutableListOf<String>()
                val selectionArgsList = mutableListOf<String>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (isTrashed) {
                        selectionConditions.add("${MediaStore.MediaColumns.IS_TRASHED} = 1")
                    } else {
                        selectionConditions.add("(${MediaStore.MediaColumns.IS_TRASHED} IS NULL OR ${MediaStore.MediaColumns.IS_TRASHED} = 0)")
                    }
                }
                if (bucketId != null) {
                    selectionConditions.add("${MediaStore.MediaColumns.BUCKET_ID} = ?")
                    selectionArgsList.add(bucketId.toString())
                }

                if (targetUri == MediaStore.Files.getContentUri("external")) {
                    when (mediaTypeFilter) {
                        MediaTypeFilter.PHOTOS_ONLY -> {
                            selectionConditions.add("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)")
                            selectionArgsList.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                            selectionArgsList.add("image/%")
                        }
                        MediaTypeFilter.VIDEOS_ONLY -> {
                            selectionConditions.add("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)")
                            selectionArgsList.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                            selectionArgsList.add("video/%")
                        }
                        MediaTypeFilter.ALL -> {
                            selectionConditions.add("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)")
                            selectionArgsList.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                            selectionArgsList.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                            selectionArgsList.add("image/%")
                            selectionArgsList.add("video/%")
                        }
                    }
                }

                val selection = if (selectionConditions.isNotEmpty()) {
                    selectionConditions.joinToString(" AND ")
                } else null
                val selectionArgs = if (selectionArgsList.isNotEmpty()) {
                    selectionArgsList.toTypedArray()
                } else null

                val cursor: Cursor? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val queryArgs = Bundle().apply {
                            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                            putStringArray(
                                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                                arrayOf(sortColumn)
                            )
                            putInt(
                                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                                sortDirectionInt
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isTrashed) {
                                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 2)
                            }
                            if (selection != null) {
                                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                            }
                            if (selectionArgs != null) {
                                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                            }
                        }
                        context.contentResolver.query(
                            targetUri,
                            projection,
                            queryArgs,
                            null
                        )
                    } else {
                        val sortOrder = "$sortColumn $sortDirectionStr LIMIT $limit OFFSET $offset"
                        context.contentResolver.query(
                            targetUri,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder
                        )
                    }
                } catch (e: Exception) {
                    val sortOrder = "$sortColumn $sortDirectionStr LIMIT $limit OFFSET $offset"
                    context.contentResolver.query(
                        targetUri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                    )
                }

                cursor?.use { c ->
                    val idColumn = c.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val displayNameColumn = c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dateTakenColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    val dateAddedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                    val dateModifiedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val sizeColumn = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val widthColumn = c.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
                    val heightColumn = c.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
                    val mimeTypeColumn = c.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val mediaTypeColumn = c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val orientationColumn = -1
                    val isTrashedColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                    } else -1

                    while (c.moveToNext()) {
                        if (idColumn == -1) continue
                        val id = c.getLong(idColumn)
                        val mimeType = if (mimeTypeColumn != -1) c.getString(mimeTypeColumn) ?: "" else ""
                        val mediaType = if (mediaTypeColumn != -1) c.getInt(mediaTypeColumn) else 0

                        val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mimeType.startsWith("video/")

                        val contentUri = if (isVideo) {
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }

                        val uri = ContentUris.withAppendedId(contentUri, id)
                        val displayName = if (displayNameColumn != -1) c.getString(displayNameColumn) ?: "" else ""
                        var dateTaken = if (dateTakenColumn != -1) c.getLong(dateTakenColumn) else 0L
                        if (dateTaken <= 0L) {
                            val dateAdded = if (dateAddedColumn != -1) c.getLong(dateAddedColumn) else 0L
                            val dateModified = if (dateModifiedColumn != -1) c.getLong(dateModifiedColumn) else 0L
                            dateTaken = when {
                                dateAdded > 0L -> dateAdded * 1000L
                                dateModified > 0L -> dateModified * 1000L
                                else -> System.currentTimeMillis()
                            }
                        }
                        val size = if (sizeColumn != -1) c.getLong(sizeColumn) else 0L
                        val width = if (widthColumn != -1) c.getInt(widthColumn) else 0
                        val height = if (heightColumn != -1) c.getInt(heightColumn) else 0
                        val defaultMime = if (isVideo) "video/mp4" else "image/jpeg"
                        val finalMime = mimeType.ifBlank { defaultMime }
                        val orientation = if (orientationColumn != -1) c.getInt(orientationColumn) else 0
                        val isTrashed = if (isTrashedColumn != -1) {
                            c.getInt(isTrashedColumn) == 1
                        } else false

                        photos.add(
                            PhotoItem(
                                id = id,
                                uri = uri,
                                displayName = displayName,
                                dateTaken = dateTaken,
                                size = size,
                                width = width,
                                height = height,
                                mimeType = finalMime,
                                orientation = orientation,
                                isTrashed = isTrashed
                            )
                        )
                    }
                }

                val nextKey = if (photos.size < limit) null else page + 1
                val prevKey = if (page == 0) null else page - 1

                LoadResult.Page(
                    data = photos,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}
