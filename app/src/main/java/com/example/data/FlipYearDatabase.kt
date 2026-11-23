package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PhotoItemEntity::class,
        MonthStampEntity::class,
        CleanupStatsEntity::class,
        YearbookCoverConfigEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FlipYearDatabase : RoomDatabase() {
    abstract fun flipYearDao(): FlipYearDao

    companion object {
        @Volatile
        private var INSTANCE: FlipYearDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN gpsScanned INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE photos SET gpsScanned = 1 WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND locationName IS NOT NULL AND locationName != ''"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_status_takenAtMillis ON photos(status, takenAtMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_year_month_status ON photos(year, month, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_isSample_gpsScanned_status ON photos(isSample, gpsScanned, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_burstGroupId ON photos(burstGroupId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `yearbook_covers` (" +
                        "`year` INTEGER NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`selectedPhotoIdsCsv` TEXT NOT NULL, " +
                        "`intervalSeconds` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`year`))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cleanup_stats ADD COLUMN totalCompressedSavedBytes INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE cleanup_stats ADD COLUMN totalCompressedCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE yearbook_covers ADD COLUMN collageTemplateId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE yearbook_covers ADD COLUMN collageSpacingDp REAL NOT NULL DEFAULT 4.0")
                db.execSQL("ALTER TABLE yearbook_covers ADD COLUMN collageBackdrop TEXT NOT NULL DEFAULT 'edition'")
                db.execSQL("ALTER TABLE yearbook_covers ADD COLUMN slotTransformsCsv TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): FlipYearDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlipYearDatabase::class.java,
                    "flipyear_cleaner.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
