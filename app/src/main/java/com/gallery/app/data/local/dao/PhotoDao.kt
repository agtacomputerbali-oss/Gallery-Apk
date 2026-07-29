package com.gallery.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gallery.app.data.local.entity.CachedPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 ORDER BY dateTaken DESC")
    fun getPhotosPagingSource(): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND bucketId = :bucketId ORDER BY dateTaken DESC")
    fun getPhotosByBucketPagingSource(bucketId: Long): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 1 ORDER BY dateTaken DESC")
    fun getTrashedPhotosPagingSource(): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0")
    fun getPhotoCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0")
    suspend fun getPhotoCount(): Int

    @Query("SELECT COUNT(*) FROM cached_photos")
    suspend fun getTotalCachedCount(): Int

    @Upsert
    suspend fun upsertPhotos(photos: List<CachedPhotoEntity>)

    @Query("DELETE FROM cached_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)

    @Query("DELETE FROM cached_photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<Long>)

    @Query("DELETE FROM cached_photos")
    suspend fun clearAll()

    // --- Smart Albums Queries ---
    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0 AND mimeType LIKE 'video/%'")
    suspend fun getVideoCount(): Int

    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Screenshot%' OR bucketName LIKE '%screenshot%')")
    suspend fun getScreenshotCount(): Int

    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0 AND latitude IS NOT NULL")
    suspend fun getGeotaggedCount(): Int

    @Query("SELECT COUNT(*) FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Selfie%' OR bucketName LIKE '%Front%' OR bucketName LIKE '%selfie%')")
    suspend fun getSelfieCount(): Int

    @Query("SELECT uriString FROM cached_photos WHERE isTrashed = 0 AND mimeType LIKE 'video/%' ORDER BY dateTaken DESC LIMIT 1")
    suspend fun getVideoCoverUri(): String?

    @Query("SELECT uriString FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Screenshot%' OR bucketName LIKE '%screenshot%') ORDER BY dateTaken DESC LIMIT 1")
    suspend fun getScreenshotCoverUri(): String?

    @Query("SELECT uriString FROM cached_photos WHERE isTrashed = 0 AND latitude IS NOT NULL ORDER BY dateTaken DESC LIMIT 1")
    suspend fun getGeotaggedCoverUri(): String?

    @Query("SELECT uriString FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Selfie%' OR bucketName LIKE '%Front%' OR bucketName LIKE '%selfie%') ORDER BY dateTaken DESC LIMIT 1")
    suspend fun getSelfieCoverUri(): String?

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND mimeType LIKE 'video/%' ORDER BY dateTaken DESC")
    fun getVideosPagingSource(): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Screenshot%' OR bucketName LIKE '%screenshot%') ORDER BY dateTaken DESC")
    fun getScreenshotsPagingSource(): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND latitude IS NOT NULL ORDER BY dateTaken DESC")
    fun getGeotaggedPagingSource(): PagingSource<Int, CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND (bucketName LIKE '%Selfie%' OR bucketName LIKE '%Front%' OR bucketName LIKE '%selfie%') ORDER BY dateTaken DESC")
    fun getSelfiesPagingSource(): PagingSource<Int, CachedPhotoEntity>

    // --- pHash & Duplicate Finder Queries ---
    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND pHash IS NULL")
    suspend fun getPhotosWithoutHash(): List<CachedPhotoEntity>

    @Query("SELECT * FROM cached_photos WHERE isTrashed = 0 AND pHash IS NOT NULL ORDER BY dateTaken DESC")
    suspend fun getAllHashedPhotos(): List<CachedPhotoEntity>

    @Query("UPDATE cached_photos SET pHash = :hash WHERE id = :id")
    suspend fun updatePhotoHash(id: Long, hash: String)
}
