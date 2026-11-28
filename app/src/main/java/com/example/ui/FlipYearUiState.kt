package com.example.ui

import android.app.PendingIntent
import androidx.compose.runtime.Immutable
import com.example.data.CleanupStatsEntity
import com.example.data.CollageBackdropStyle
import com.example.data.CollageSlotTransform
import com.example.data.MediaAccessLevel
import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.YearbookCoverMode
import com.example.ui.components.TimelineClusterLayout
import com.example.ui.theme.TimelineSortOrder

enum class MediaFilter(val label: String) {
    ALL("全部"),
    PHOTOS("照片"),
    VIDEOS("视频")
}

@Immutable
data class YearbookSummary(
    val year: Int,
    val coverPhoto: PhotoItemEntity?,
    val coverSlides: List<PhotoItemEntity> = listOfNotNull(coverPhoto),
    val coverMode: YearbookCoverMode = YearbookCoverMode.SMART_CAROUSEL,
    val coverIntervalSeconds: Int = YearbookCoverCurator.DEFAULT_INTERVAL_SECONDS,
    val customCoverPhotoIds: List<Long> = emptyList(),
    val effectiveIsCollage: Boolean = false,
    val collageTemplateId: String = "diag_quad_4",
    val collageSpacingDp: Float = 4f,
    val collageBackdrop: CollageBackdropStyle = CollageBackdropStyle.EDITION,
    val collageSlotTransforms: List<CollageSlotTransform> = emptyList(),
    val totalPhotos: Int,
    val videoCount: Int,
    val unreviewedCount: Int,
    val keptCount: Int,
    val trashedCount: Int,
    val cleanedMonthsCount: Int,
    val totalMonthsCount: Int,
    val reclaimableBytes: Long,
    val editionIndex: Int
) {
    val progressFraction: Float
        get() = if (totalPhotos == 0) 1f else ((totalPhotos - unreviewedCount).toFloat() / totalPhotos.toFloat()).coerceIn(0f, 1f)
}

@Immutable
data class MonthPageModel(
    val year: Int,
    val month: Int,
    val photos: List<PhotoItemEntity>,
    val photosByDay: Map<Int, List<PhotoItemEntity>>,
    val cleanedDays: Set<Int>,
    val totalPhotos: Int,
    val videoCount: Int,
    val unreviewedCount: Int,
    val keptCount: Int,
    val trashedCount: Int,
    val burstGroupCount: Int,
    val reclaimableBytes: Long,
    val stamp: MonthStampEntity?,
    val coverPhoto: PhotoItemEntity?
) {
    val isCleaned: Boolean
        get() = stamp != null || (totalPhotos > 0 && unreviewedCount == 0)
}

@Immutable
sealed interface CleanupCardItem {
    val stableKey: String
    val primaryPhoto: PhotoItemEntity
    val totalBytes: Long

    @Immutable
    data class SinglePhoto(
        val photo: PhotoItemEntity
    ) : CleanupCardItem {
        override val stableKey: String = "single_${photo.id}"
        override val primaryPhoto: PhotoItemEntity = photo
        override val totalBytes: Long = photo.fileSizeBytes
    }

    @Immutable
    data class BurstStack(
        val groupId: String,
        val photos: List<PhotoItemEntity>,
        val bestPhoto: PhotoItemEntity
    ) : CleanupCardItem {
        override val stableKey: String = "burst_$groupId"
        override val primaryPhoto: PhotoItemEntity = bestPhoto
        override val totalBytes: Long = photos.sumOf { it.fileSizeBytes }
        val nonBestBytes: Long
            get() = photos.filter { it.id != bestPhoto.id }.sumOf { it.fileSizeBytes }
    }
}

@Immutable
data class LocationCluster(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val photos: List<PhotoItemEntity>,
    val coverPhoto: PhotoItemEntity,
    val unreviewedCount: Int,
    val totalBytes: Long
)

data class UndoSnapshot(
    val id: Long = System.nanoTime(),
    val photoStates: List<PhotoStateBackup>,
    val description: String,
    val actionLabel: String = "撤销",
    val cardKey: String? = null
)

@Immutable
data class SnackbarMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val undoLabel: String? = null,
    val undoSnapshot: UndoSnapshot? = null,
    val durationMs: Long = if (undoSnapshot != null) 4500L else 2800L
)

enum class CleanerEntrySource(val routeKey: String, val backLabel: String) {
    FLIPBOOK("flipbook", "返回月历"),
    BOOKSHELF("bookshelf", "返回书架"),
    MAP("map", "返回足迹"),
    GALLERY("gallery", "返回画廊");

    companion object {
        fun fromRouteKey(key: String?): CleanerEntrySource =
            entries.firstOrNull { it.routeKey == key } ?: BOOKSHELF
    }
}

data class PhotoStateBackup(
    val photoId: Long,
    val year: Int,
    val month: Int,
    val previousStatus: String,
    val previousFavorite: Boolean,
    val previousBucketName: String? = null,
    val restoreLocation: Boolean = false,
    val previousLocationName: String? = null,
    val previousLatitude: Double? = null,
    val previousLongitude: Double? = null,
    val previousGpsScanned: Boolean = false,
    val favoriteTouched: Boolean = false
)

data class PendingFavoriteConsent(
    val pendingIntent: PendingIntent,
    val onApproved: suspend () -> Unit,
    val onCancelled: () -> Unit
)

@Immutable
sealed interface GalleryEntry {
    val stableKey: String
    val latestTimestamp: Long
    val primaryPhoto: PhotoItemEntity

    @Immutable
    data class Single(
        val photo: PhotoItemEntity
    ) : GalleryEntry {
        override val stableKey: String = "gal_single_${photo.id}"
        override val latestTimestamp: Long = photo.takenAtMillis
        override val primaryPhoto: PhotoItemEntity = photo
    }

    @Immutable
    data class Burst(
        val groupId: String,
        val photos: List<PhotoItemEntity>,
        val bestPhoto: PhotoItemEntity
    ) : GalleryEntry {
        override val stableKey: String = "gal_burst_$groupId"
        override val latestTimestamp: Long = photos.maxOf { it.takenAtMillis }
        override val primaryPhoto: PhotoItemEntity = bestPhoto
        val totalBytes: Long = photos.sumOf { it.fileSizeBytes }
        val nonBestBytes: Long = photos.filter { it.id != bestPhoto.id }.sumOf { it.fileSizeBytes }
        val spanSeconds: Long = ((photos.maxOf { it.takenAtMillis } - photos.minOf { it.takenAtMillis }) / 1000L).coerceAtLeast(1L)
        val representativePhoto: PhotoItemEntity
            get() {
                val treasured = photos.filter { it.isFavorite }
                if (treasured.isEmpty()) return bestPhoto
                return treasured.firstOrNull { it.id == bestPhoto.id }
                    ?: treasured.maxWithOrNull(
                        compareBy<PhotoItemEntity> { it.qualityScore }.thenBy { it.takenAtMillis }
                    )
                    ?: bestPhoto
            }
    }
}

internal fun filterTreasuredGalleryEntries(entries: List<GalleryEntry>): List<GalleryEntry> {
    return entries.mapNotNull { entry ->
        when (entry) {
            is GalleryEntry.Single -> entry.takeIf { it.photo.isFavorite }
            is GalleryEntry.Burst -> {
                val treasuredPhotos = entry.photos.filter { it.isFavorite }
                if (treasuredPhotos.isEmpty()) {
                    null
                } else {
                    entry.copy(
                        photos = treasuredPhotos,
                        bestPhoto = entry.bestPhoto.takeIf { it.isFavorite } ?: treasuredPhotos.first()
                    )
                }
            }
        }
    }
}

const val UNLOCATED_LOCATION_KEY = "__UNLOCATED__"

@Immutable
data class CustomCleanerSession(
    val title: String,
    val photoIds: Set<Long>,
    val includeReviewed: Boolean = true
)

@Immutable
data class PrecomputedGalleryIndex(
    val sourceEntries: List<GalleryEntry> = emptyList(),
    val existingAlbums: List<String> = emptyList(),
    val burstEntries: List<GalleryEntry.Burst> = emptyList(),
    val treasuredEntries: List<GalleryEntry> = emptyList(),
    val largePhotoEntries: List<GalleryEntry> = emptyList(),
    val missingLocationEntries: List<GalleryEntry> = emptyList(),
    val extractedKeywordTags: List<String> = emptyList(),
    val defaultClusterLayout: TimelineClusterLayout = TimelineClusterLayout(emptyList(), emptyList())
) {
    fun matches(entries: List<GalleryEntry>): Boolean = sourceEntries === entries

    companion object {
        val EMPTY = PrecomputedGalleryIndex()
    }
}

@Immutable
data class TreasureGalleryRequest(
    val year: Int? = null,
    val month: Int? = null,
    val requestId: Long = 0L
)

@Immutable
data class FlipYearUiState(
    val isLoading: Boolean = true,
    val isScanningDevice: Boolean = false,
    val isIndexingGps: Boolean = false,
    val gpsIndexedCount: Int = 0,
    val gpsTotalCount: Int = 0,
    val mediaAccessLevel: MediaAccessLevel = MediaAccessLevel.NONE,
    val hasMediaLocationPermission: Boolean = true,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val timelineSortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST,
    val allPhotos: List<PhotoItemEntity> = emptyList(),
    val galleryEntries: List<GalleryEntry> = emptyList(),
    val galleryIndex: PrecomputedGalleryIndex = PrecomputedGalleryIndex.EMPTY,
    val yearbooks: List<YearbookSummary> = emptyList(),
    val monthStamps: Map<String, MonthStampEntity> = emptyMap(),
    val trashedPhotos: List<PhotoItemEntity> = emptyList(),
    val locationClusters: List<LocationCluster> = emptyList(),
    val unlocatedPhotos: List<PhotoItemEntity> = emptyList(),
    val stats: CleanupStatsEntity = CleanupStatsEntity(),
    val totalImageCount: Int = 0,
    val totalVideoCount: Int = 0,
    val hasDeviceMedia: Boolean = false,
    val hasSamplePhotos: Boolean = false,
    val pendingTrashBytes: Long = 0L,
    val undoStackDepth: Int = 0,
    val cleanerSessionUndoDepth: Int = 0,
    val snackbarMessage: SnackbarMessage? = null,
    val feedbackBanner: String? = null,
    val recentlyStampedMonthKey: String? = null,
    val vaultSessionFilterIds: Set<Long>? = null,
    val customCleanerSession: CustomCleanerSession? = null,
    val pendingTreasureGalleryRequest: TreasureGalleryRequest? = null,
    val batchTaskMessage: String? = null,
    val batchTaskProcessed: Int = 0,
    val batchTaskTotal: Int = 0,
    val isIndexingSmartTags: Boolean = false,
    val smartTagIndexedCount: Int = 0,
    val smartTagTotalCount: Int = 0,
    val smartTagRevision: Int = 0
)
