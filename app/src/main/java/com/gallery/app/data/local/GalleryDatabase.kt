package com.gallery.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gallery.app.data.local.dao.PhotoDao
import com.gallery.app.data.local.entity.CachedPhotoEntity

@Database(
    entities = [CachedPhotoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_photos ADD COLUMN pHash TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_photos_pHash ON cached_photos(pHash)")
            }
        }
    }
}
