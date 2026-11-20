package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PhotoStatus {
    UNREVIEWED,
    KEPT,
    TRASHED,
    PURGED
}

enum class MediaType {
    IMAGE,
    VIDEO
}

@Immutable
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["mediaStoreKey"], unique = true),
        Index(value = ["status", "takenAtMillis"]),
        Index(value = ["year", "month", "status"]),
        Index(value = ["isSample", "gpsScanned", "status"]),
        Index(value = ["burstGroupId"])
    ]
)
data class PhotoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreKey: String? = null, // e.g. "img_1024" or "vid_2048" for idempotent MediaStore sync
    val mediaType: String = MediaType.IMAGE.name,
    val durationMs: Long = 0L,
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..31
    val takenAtMillis: Long,
    val fileSizeBytes: Long,
    val widthPx: Int,
    val heightPx: Int,
    val drawableName: String? = null,
    val contentUri: String? = null,
    val status: String = PhotoStatus.UNREVIEWED.name,
    val isFavorite: Boolean = false,
    val burstGroupId: String? = null,
    val qualityScore: Int = 85,
    val isBestInStack: Boolean = false,
    val visualVariant: String = "CRISP",
    val caption: String = "",
    val bucketName: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val trashedAtMillis: Long? = null,
    val isSample: Boolean = true,
    val gpsScanned: Boolean = false
) {
    // Zero-allocation fast lookup (avoids PhotoStatus.valueOf + runCatching overhead across 10k+ rows)
    val photoStatus: PhotoStatus
        get() = when (status) {
            "UNREVIEWED" -> PhotoStatus.UNREVIEWED
            "KEPT" -> PhotoStatus.KEPT
            "TRASHED" -> PhotoStatus.TRASHED
            "PURGED" -> PhotoStatus.PURGED
            else -> PhotoStatus.UNREVIEWED
        }

    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO.name
}

data class DeviceMediaKeyRow(
    val id: Long,
    val mediaStoreKey: String?
)

data class LocationBatchUpdate(
    val id: Long,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?
)

@Entity(tableName = "month_stamps")
data class MonthStampEntity(
    @PrimaryKey val yearMonthKey: String,
    val year: Int,
    val month: Int,
    val cleanedAtMillis: Long,
    val freedBytes: Long
)

@Entity(tableName = "cleanup_stats")
data class CleanupStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalPurgedBytes: Long = 0L,
    val totalPurgedCount: Int = 0,
    val cleanedStreakDays: Int = 1
)

enum class YearbookCoverMode(val label: String) {
    SMART_CAROUSEL("智能轮播"),
    SINGLE_CUSTOM("单图定格"),
    CUSTOM_CAROUSEL("多图轮播")
}

@Immutable
@Entity(tableName = "yearbook_covers")
data class YearbookCoverConfigEntity(
    @PrimaryKey val year: Int,
    val mode: String = YearbookCoverMode.SMART_CAROUSEL.name,
    val selectedPhotoIdsCsv: String = "",
    val intervalSeconds: Int = 5,
    val updatedAtMillis: Long = 0L
) {
    val coverMode: YearbookCoverMode
        get() = when (mode) {
            YearbookCoverMode.SINGLE_CUSTOM.name -> YearbookCoverMode.SINGLE_CUSTOM
            YearbookCoverMode.CUSTOM_CAROUSEL.name -> YearbookCoverMode.CUSTOM_CAROUSEL
            else -> YearbookCoverMode.SMART_CAROUSEL
        }

    val selectedPhotoIds: List<Long>
        get() = if (selectedPhotoIdsCsv.isBlank()) {
            emptyList()
        } else {
            selectedPhotoIdsCsv
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
                .distinct()
        }
}

