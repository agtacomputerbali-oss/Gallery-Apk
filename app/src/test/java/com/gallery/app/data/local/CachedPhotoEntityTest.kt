package com.gallery.app.data.local

import com.gallery.app.data.local.entity.CachedPhotoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CachedPhotoEntityTest {

    @Test
    fun createCachedPhotoEntity_defaultGpsValuesAreNull() {
        val entity = CachedPhotoEntity(
            id = 100L,
            uriString = "content://media/external/images/media/100",
            displayName = "test_photo.jpg",
            dateTaken = 1700000000000L,
            size = 2048576L,
            width = 1920,
            height = 1080,
            mimeType = "image/jpeg",
            orientation = 0,
            bucketId = 1L,
            bucketName = "Camera",
            isTrashed = false
        )

        assertEquals(100L, entity.id)
        assertEquals("test_photo.jpg", entity.displayName)
        assertFalse(entity.isTrashed)
        assertNull(entity.latitude)
        assertNull(entity.longitude)
    }

    @Test
    fun createCachedPhotoEntity_withGpsCoordinates() {
        val entity = CachedPhotoEntity(
            id = 101L,
            uriString = "content://media/external/images/media/101",
            displayName = "gps_photo.jpg",
            dateTaken = 1700000000000L,
            size = 1024L,
            width = 800,
            height = 600,
            mimeType = "image/jpeg",
            orientation = 90,
            bucketId = 2L,
            bucketName = "Vacation",
            isTrashed = false,
            latitude = -6.2088,
            longitude = 106.8456
        )

        assertEquals(-6.2088, entity.latitude!!, 0.0001)
        assertEquals(106.8456, entity.longitude!!, 0.0001)
    }
}
