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

enum class ImageFormat {
    JPEG,
    PNG,
    GIF,
    WEBP,
    HEIC,
    AVIF,
    RAW,
    UNKNOWN
}

enum class MediaSubtype {
    STANDARD,
    CAMERA,
    SCREENSHOT,
    LONG_SCREENSHOT,
    SCREEN_RECORD,
    DOCUMENT,
    CHAT_SAVE,
    DOWNLOAD
}

enum class MotionPhotoType {
    NONE,
    EMBEDDED,
    PAIRED
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
    val mimeType: String? = null,
    val imageFormat: String = ImageFormat.UNKNOWN.name,
    val mediaSubtype: String = MediaSubtype.STANDARD.name,
    val isAnimated: Boolean = false,
    val motionPhotoType: String = MotionPhotoType.NONE.name,
    val motionVideoUri: String? = null,
    val motionVideoDurationMs: Long = 0L,
    val motionVideoOffset: Long? = null,
    val motionVideoLength: Long? = null,
    val pairedMediaStoreKey: String? = null,
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

    val imageFormatKind: ImageFormat
        get() = ImageFormat.entries.firstOrNull { it.name == imageFormat } ?: ImageFormat.UNKNOWN

    val mediaSubtypeKind: MediaSubtype
        get() = MediaSubtype.entries.firstOrNull { it.name == mediaSubtype } ?: MediaSubtype.STANDARD

    val motionPhotoKind: MotionPhotoType
        get() = MotionPhotoType.entries.firstOrNull { it.name == motionPhotoType } ?: MotionPhotoType.NONE

    val isLivePhoto: Boolean
        get() = !isVideo && motionPhotoKind != MotionPhotoType.NONE && !motionVideoUri.isNullOrBlank()

    val physicalMediaUris: List<String>
        get() = buildList {
            contentUri?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (motionPhotoKind == MotionPhotoType.PAIRED) {
                motionVideoUri?.takeIf { it.isNotBlank() && it != contentUri }?.let { add(it) }
            }
        }
}

data class DeviceMediaKeyRow(
    val id: Long,
    val mediaStoreKey: String?
)

data class DeviceMediaSyncRow(
    val id: Long,
    val mediaStoreKey: String?,
    val durationMs: Long,
    val mimeType: String?,
    val imageFormat: String,
    val mediaSubtype: String,
    val isAnimated: Boolean,
    val motionPhotoType: String,
    val motionVideoUri: String?,
    val motionVideoDurationMs: Long,
    val motionVideoOffset: Long?,
    val motionVideoLength: Long?,
    val pairedMediaStoreKey: String?,
    val fileSizeBytes: Long,
    val widthPx: Int,
    val heightPx: Int,
    val caption: String,
    val bucketName: String?
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
    val cleanedStreakDays: Int = 1,
    val totalCompressedSavedBytes: Long = 0L,
    val totalCompressedCount: Int = 0
)

enum class YearbookCoverMode(val label: String) {
    SMART_CAROUSEL("智能默认"),
    CUSTOM_COLLAGE("不规则拼图"),
    SINGLE_CUSTOM("单图定格"),
    CUSTOM_CAROUSEL("多图轮播")
}

enum class CollageBackdropStyle(val storageKey: String, val label: String) {
    EDITION("edition", "装帧色"),
    WARM_PAPER("warm_paper", "暖白纸"),
    OBSIDIAN("obsidian", "曜石黑");

    companion object {
        fun fromStorageKey(key: String?): CollageBackdropStyle =
            entries.firstOrNull { it.storageKey == key } ?: EDITION
    }
}

@Immutable
data class CollageSlotTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    val isIdentity: Boolean
        get() = scale in 0.99f..1.01f && offsetX in -0.005f..0.005f && offsetY in -0.005f..0.005f
}

fun encodeSlotTransforms(transforms: List<CollageSlotTransform>): String =
    transforms.joinToString(";") { t ->
        "${"%.3f".format(java.util.Locale.US, t.scale.coerceIn(1f, 3f))}:" +
            "${"%.3f".format(java.util.Locale.US, t.offsetX.coerceIn(-0.85f, 0.85f))}:" +
            "${"%.3f".format(java.util.Locale.US, t.offsetY.coerceIn(-0.85f, 0.85f))}"
    }

fun decodeSlotTransforms(encoded: String): List<CollageSlotTransform> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(';').map { token ->
        val parts = token.split(':')
        if (parts.size >= 3) {
            CollageSlotTransform(
                scale = (parts[0].toFloatOrNull() ?: 1f).coerceIn(1f, 3f),
                offsetX = (parts[1].toFloatOrNull() ?: 0f).coerceIn(-0.85f, 0.85f),
                offsetY = (parts[2].toFloatOrNull() ?: 0f).coerceIn(-0.85f, 0.85f)
            )
        } else {
            CollageSlotTransform()
        }
    }
}

@Immutable
@Entity(tableName = "yearbook_covers")
data class YearbookCoverConfigEntity(
    @PrimaryKey val year: Int,
    val mode: String = YearbookCoverMode.SMART_CAROUSEL.name,
    val selectedPhotoIdsCsv: String = "",
    val intervalSeconds: Int = 5,
    val updatedAtMillis: Long = 0L,
    val collageTemplateId: String = "",
    val collageSpacingDp: Float = 4f,
    val collageBackdrop: String = CollageBackdropStyle.EDITION.storageKey,
    val slotTransformsCsv: String = ""
) {
    val coverMode: YearbookCoverMode
        get() = when (mode) {
            YearbookCoverMode.CUSTOM_COLLAGE.name -> YearbookCoverMode.CUSTOM_COLLAGE
            YearbookCoverMode.SINGLE_CUSTOM.name -> YearbookCoverMode.SINGLE_CUSTOM
            YearbookCoverMode.CUSTOM_CAROUSEL.name -> YearbookCoverMode.CUSTOM_CAROUSEL
            else -> YearbookCoverMode.SMART_CAROUSEL
        }

    val backdropStyle: CollageBackdropStyle
        get() = CollageBackdropStyle.fromStorageKey(collageBackdrop)

    val slotTransforms: List<CollageSlotTransform>
        get() = decodeSlotTransforms(slotTransformsCsv)

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


