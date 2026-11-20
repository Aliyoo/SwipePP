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
        CleanupStatsEntity::class
    ],
    version = 3,
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

        fun getInstance(context: Context): FlipYearDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlipYearDatabase::class.java,
                    "flipyear_cleaner.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
