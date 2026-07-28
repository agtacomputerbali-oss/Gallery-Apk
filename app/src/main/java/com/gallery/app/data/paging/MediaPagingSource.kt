package com.gallery.app.data.paging

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gallery.app.domain.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPagingSource(
    private val context: Context,
    private val bucketId: Long? = null,
    private val isTrashed: Boolean = false
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

        return withContext(Dispatchers.IO) {
            try {
                val photos = mutableListOf<PhotoItem>()
                val projection = mutableListOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.ORIENTATION
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.MediaColumns.IS_TRASHED)
                    }
                }.toTypedArray()

                val selectionConditions = mutableListOf<String>()
                val selectionArgsList = mutableListOf<String>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val trashedValue = if (isTrashed) 1 else 0
                    selectionConditions.add("${MediaStore.MediaColumns.IS_TRASHED} = $trashedValue")
                }
                if (bucketId != null) {
                    selectionConditions.add("${MediaStore.Images.Media.BUCKET_ID} = ?")
                    selectionArgsList.add(bucketId.toString())
                }

                val selection = if (selectionConditions.isNotEmpty()) {
                    selectionConditions.joinToString(" AND ")
                } else null
                val selectionArgs = if (selectionArgsList.isNotEmpty()) {
                    selectionArgsList.toTypedArray()
                } else null

                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.Media.DATE_TAKEN)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isTrashed) {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 2) // MediaStore.MATCH_TRASHED_ONLY
                    }
                    if (selection != null) {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    }
                    if (selectionArgs != null) {
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                }

                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    queryArgs,
                    null
                )

                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val displayNameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateTakenColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val widthColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                    val heightColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                    val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val orientationColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                    val isTrashedColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                    } else -1

                    while (c.moveToNext()) {
                        val id = c.getLong(idColumn)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val displayName = c.getString(displayNameColumn) ?: ""
                        val dateTaken = c.getLong(dateTakenColumn)
                        val size = c.getLong(sizeColumn)
                        val width = c.getInt(widthColumn)
                        val height = c.getInt(heightColumn)
                        val mimeType = c.getString(mimeTypeColumn) ?: "image/*"
                        val orientation = c.getInt(orientationColumn)
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
                                mimeType = mimeType,
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
