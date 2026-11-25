package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FlipYearDao {

    @Query("SELECT * FROM photos WHERE status != 'PURGED' ORDER BY takenAtMillis DESC")
    fun observeAllActivePhotos(): Flow<List<PhotoItemEntity>>

    @Query("SELECT * FROM photos WHERE status = 'TRASHED' ORDER BY trashedAtMillis DESC")
    fun observeTrashedPhotos(): Flow<List<PhotoItemEntity>>

    @Query("SELECT * FROM month_stamps ORDER BY year DESC, month DESC")
    fun observeMonthStamps(): Flow<List<MonthStampEntity>>

    @Query("SELECT * FROM cleanup_stats WHERE id = 1")
    fun observeCleanupStats(): Flow<CleanupStatsEntity?>

    @Query("SELECT * FROM yearbook_covers")
    fun observeYearbookCovers(): Flow<List<YearbookCoverConfigEntity>>

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getTotalPhotoCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE isSample = 0")
    suspend fun getDeviceMediaCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE isSample = 1")
    suspend fun getSamplePhotoCount(): Int

    @Query("SELECT id, mediaStoreKey FROM photos WHERE isSample = 0")
    suspend fun getAllDeviceMediaKeysSync(): List<DeviceMediaKeyRow>

    @Query(
        "SELECT id, mediaStoreKey, durationMs, mimeType, imageFormat, mediaSubtype, isAnimated, " +
            "motionPhotoType, motionVideoUri, motionVideoDurationMs, motionVideoOffset, motionVideoLength, " +
            "pairedMediaStoreKey, fileSizeBytes, widthPx, heightPx, caption, bucketName " +
            "FROM photos WHERE isSample = 0"
    )
    suspend fun getAllDeviceMediaSyncRowsSync(): List<DeviceMediaSyncRow>

    @Query(
        "UPDATE photos SET durationMs = :durationMs, mimeType = :mimeType, imageFormat = :imageFormat, " +
            "mediaSubtype = :mediaSubtype, isAnimated = :isAnimated, motionPhotoType = :motionPhotoType, " +
            "motionVideoUri = :motionVideoUri, motionVideoDurationMs = :motionVideoDurationMs, " +
            "motionVideoOffset = :motionVideoOffset, motionVideoLength = :motionVideoLength, " +
            "pairedMediaStoreKey = :pairedMediaStoreKey, fileSizeBytes = :fileSizeBytes, " +
            "widthPx = :widthPx, heightPx = :heightPx, caption = :caption, bucketName = :bucketName " +
            "WHERE mediaStoreKey = :mediaStoreKey AND isSample = 0"
    )
    suspend fun updateDeviceMediaMetadata(
        mediaStoreKey: String,
        durationMs: Long,
        mimeType: String?,
        imageFormat: String,
        mediaSubtype: String,
        isAnimated: Boolean,
        motionPhotoType: String,
        motionVideoUri: String?,
        motionVideoDurationMs: Long,
        motionVideoOffset: Long?,
        motionVideoLength: Long?,
        pairedMediaStoreKey: String?,
        fileSizeBytes: Long,
        widthPx: Int,
        heightPx: Int,
        caption: String,
        bucketName: String?
    )

    @Query("SELECT * FROM photos WHERE isSample = 0 AND gpsScanned = 0 AND status != 'PURGED' AND (latitude IS NULL OR longitude IS NULL OR locationName IS NULL OR locationName = '') ORDER BY takenAtMillis DESC")
    suspend fun getDeviceMediaMissingLocationSync(): List<PhotoItemEntity>

    @Query("UPDATE photos SET gpsScanned = 0 WHERE isSample = 0 AND status != 'PURGED' AND (latitude IS NULL OR longitude IS NULL OR locationName IS NULL OR locationName = '')")
    suspend fun resetGpsScannedForUnlocated()

    @Query("SELECT * FROM photos WHERE year = :year AND month = :month AND status != 'PURGED'")
    suspend fun getPhotosForMonth(year: Int, month: Int): List<PhotoItemEntity>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun getPhotosByIds(ids: List<Long>): List<PhotoItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotosIgnoreConflicts(photos: List<PhotoItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoItemEntity>)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    @Query("UPDATE photos SET status = :status, trashedAtMillis = :trashedAtMillis WHERE id = :id")
    suspend fun updatePhotoStatus(id: Long, status: String, trashedAtMillis: Long?)

    @Query("UPDATE photos SET status = :status, trashedAtMillis = :trashedAtMillis WHERE id IN (:ids)")
    suspend fun updatePhotosStatus(ids: List<Long>, status: String, trashedAtMillis: Long?)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updatePhotoFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun updatePhotosFavorite(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE photos SET bucketName = :bucketName WHERE id IN (:ids)")
    suspend fun updatePhotosAlbum(ids: List<Long>, bucketName: String?)

    @Query("UPDATE photos SET bucketName = :bucketName, year = :year, month = :month WHERE id IN (:ids)")
    suspend fun updatePhotosAlbumAndMonth(ids: List<Long>, bucketName: String?, year: Int, month: Int)

    @Query("UPDATE photos SET bucketName = :bucketName, year = :year, month = :month WHERE id = :id")
    suspend fun updatePhotoAlbumAndMonth(id: Long, bucketName: String?, year: Int, month: Int)

    @Query(
        "UPDATE photos SET status = :status, trashedAtMillis = :trashedAtMillis, " +
            "isFavorite = :isFavorite, bucketName = :bucketName, year = :year, month = :month " +
            "WHERE id = :id"
    )
    suspend fun restorePhotoState(
        id: Long,
        status: String,
        trashedAtMillis: Long?,
        isFavorite: Boolean,
        bucketName: String?,
        year: Int,
        month: Int
    )

    @Query(
        "UPDATE photos SET status = :status, trashedAtMillis = :trashedAtMillis, " +
            "isFavorite = :isFavorite, bucketName = :bucketName, year = :year, month = :month, " +
            "locationName = :locationName, latitude = :latitude, longitude = :longitude, gpsScanned = :gpsScanned " +
            "WHERE id = :id"
    )
    suspend fun restorePhotoStateWithLocation(
        id: Long,
        status: String,
        trashedAtMillis: Long?,
        isFavorite: Boolean,
        bucketName: String?,
        year: Int,
        month: Int,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        gpsScanned: Boolean
    )

    @Query("UPDATE photos SET fileSizeBytes = :fileSizeBytes, widthPx = :widthPx, heightPx = :heightPx WHERE id = :id")
    suspend fun updatePhotoSizeAndDimensions(id: Long, fileSizeBytes: Long, widthPx: Int, heightPx: Int)

    @Query("UPDATE photos SET locationName = :locationName, latitude = :latitude, longitude = :longitude, gpsScanned = 1 WHERE id = :id")
    suspend fun updatePhotoLocationAndScanned(id: Long, locationName: String?, latitude: Double?, longitude: Double?)

    @Query("UPDATE photos SET gpsScanned = 1 WHERE id IN (:ids)")
    suspend fun markPhotosGpsScanned(ids: List<Long>)

    @Query("UPDATE photos SET isBestInStack = CASE WHEN id = :bestPhotoId THEN 1 ELSE 0 END WHERE burstGroupId = :groupId")
    suspend fun setBestInStack(groupId: String, bestPhotoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthStamp(stamp: MonthStampEntity)

    @Query("DELETE FROM month_stamps WHERE yearMonthKey = :yearMonthKey")
    suspend fun deleteMonthStamp(yearMonthKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCleanupStats(stats: CleanupStatsEntity)

    @Query("SELECT * FROM cleanup_stats WHERE id = 1")
    suspend fun getCleanupStatsSync(): CleanupStatsEntity?

    @Query("DELETE FROM photos WHERE isSample = 1")
    suspend fun deleteSamplePhotos(): Int

    @Query("DELETE FROM month_stamps")
    suspend fun clearAllMonthStamps()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertYearbookCover(config: YearbookCoverConfigEntity)

    @Query("DELETE FROM yearbook_covers WHERE year = :year")
    suspend fun deleteYearbookCover(year: Int)

    /**
     * Commits an entire MediaStore delta sync inside a single SQLite transaction so Room emits
     * at most ONE Flow invalidation instead of hundreds, and safely chunks IN(...) clauses under 999 params.
     */
    @Transaction
    suspend fun updateDeviceMediaMetadataBatch(items: List<PhotoItemEntity>) {
        for (item in items) {
            val key = item.mediaStoreKey ?: continue
            updateDeviceMediaMetadata(
                mediaStoreKey = key,
                durationMs = item.durationMs,
                mimeType = item.mimeType,
                imageFormat = item.imageFormat,
                mediaSubtype = item.mediaSubtype,
                isAnimated = item.isAnimated,
                motionPhotoType = item.motionPhotoType,
                motionVideoUri = item.motionVideoUri,
                motionVideoDurationMs = item.motionVideoDurationMs,
                motionVideoOffset = item.motionVideoOffset,
                motionVideoLength = item.motionVideoLength,
                pairedMediaStoreKey = item.pairedMediaStoreKey,
                fileSizeBytes = item.fileSizeBytes,
                widthPx = item.widthPx,
                heightPx = item.heightPx,
                caption = item.caption,
                bucketName = item.bucketName
            )
        }
    }

    @Transaction
    suspend fun syncDeviceMediaBatch(
        vanishedIds: List<Long>,
        newItems: List<PhotoItemEntity>,
        invalidateMonthKeys: List<String>,
        deleteSamples: Boolean,
        metadataUpdates: List<PhotoItemEntity> = emptyList()
    ) {
        if (metadataUpdates.isNotEmpty()) {
            updateDeviceMediaMetadataBatch(metadataUpdates)
        }
        if (vanishedIds.isNotEmpty()) {
            vanishedIds.chunked(SQLITE_BIND_CHUNK).forEach { chunk ->
                deletePhotosByIds(chunk)
            }
        }
        if (newItems.isNotEmpty()) {
            newItems.chunked(SQLITE_BIND_CHUNK).forEach { chunk ->
                insertPhotosIgnoreConflicts(chunk)
            }
            invalidateMonthKeys.forEach { key ->
                deleteMonthStamp(key)
            }
        }
        if (deleteSamples) {
            deleteSamplePhotos()
        }
    }

    /**
     * Commits a batch of resolved + unlocated GPS scan results in a single SQLite transaction,
     * drastically cutting Room Flow invalidations from O(N) down to O(N / 64).
     */
    @Transaction
    suspend fun applyGpsBatch(
        located: List<LocationBatchUpdate>,
        unlocatedIds: List<Long>
    ) {
        located.forEach { update ->
            updatePhotoLocationAndScanned(
                id = update.id,
                locationName = update.locationName,
                latitude = update.latitude,
                longitude = update.longitude
            )
        }
        if (unlocatedIds.isNotEmpty()) {
            unlocatedIds.chunked(SQLITE_BIND_CHUNK).forEach { chunk ->
                markPhotosGpsScanned(chunk)
            }
        }
    }

    /**
     * Atomically restores all photos in an Undo snapshot in a single SQLite transaction
     * so Room emits exactly ONE consistent snapshot update (preventing half-restored burst churn).
     */
    @Transaction
    suspend fun restorePhotoStatesBatch(
        restores: List<PhotoRestoreSpec>
    ) {
        for (spec in restores) {
            if (spec.restoreLocation) {
                restorePhotoStateWithLocation(
                    id = spec.photoId,
                    status = spec.status,
                    trashedAtMillis = spec.trashedAtMillis,
                    isFavorite = spec.isFavorite,
                    bucketName = spec.bucketName,
                    year = spec.year,
                    month = spec.month,
                    locationName = spec.locationName,
                    latitude = spec.latitude,
                    longitude = spec.longitude,
                    gpsScanned = spec.gpsScanned
                )
            } else {
                restorePhotoState(
                    id = spec.photoId,
                    status = spec.status,
                    trashedAtMillis = spec.trashedAtMillis,
                    isFavorite = spec.isFavorite,
                    bucketName = spec.bucketName,
                    year = spec.year,
                    month = spec.month
                )
            }
        }
    }

    @Transaction
    suspend fun applyCompressedPhotosBatch(
        updates: List<CompressedPhotoUpdate>
    ) {
        for (u in updates) {
            updatePhotoSizeAndDimensions(
                id = u.id,
                fileSizeBytes = u.newSizeBytes,
                widthPx = u.newWidthPx,
                heightPx = u.newHeightPx
            )
        }
    }

    /**
     * Safe two-stage compression transaction: inserts newly written compressed photo copies
     * and moves the original full-resolution photos into TRASHED so users can restore originals
     * from the Trash Vault at any time before final purge.
     */
    @Transaction
    suspend fun applySafeCompressedPhotosBatch(
        newCompressedPhotos: List<PhotoItemEntity>,
        originalPhotoIdsToTrash: List<Long>,
        trashedAtMillis: Long
    ) {
        if (newCompressedPhotos.isNotEmpty()) {
            newCompressedPhotos.chunked(SQLITE_BIND_CHUNK).forEach { chunk ->
                insertPhotosIgnoreConflicts(chunk)
            }
        }
        if (originalPhotoIdsToTrash.isNotEmpty()) {
            originalPhotoIdsToTrash.chunked(SQLITE_BIND_CHUNK).forEach { chunk ->
                updatePhotosStatus(
                    ids = chunk,
                    status = PhotoStatus.TRASHED.name,
                    trashedAtMillis = trashedAtMillis
                )
            }
        }
    }

    @Transaction
    suspend fun applyManualLocationBatch(
        updates: List<LocationBatchUpdate>
    ) {
        for (u in updates) {
            updatePhotoLocationAndScanned(
                id = u.id,
                locationName = u.locationName,
                latitude = u.latitude,
                longitude = u.longitude
            )
        }
    }

    companion object {
        const val SQLITE_BIND_CHUNK = 500
    }
}

data class PhotoRestoreSpec(
    val photoId: Long,
    val status: String,
    val trashedAtMillis: Long?,
    val isFavorite: Boolean,
    val bucketName: String?,
    val year: Int,
    val month: Int,
    val restoreLocation: Boolean = false,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsScanned: Boolean = false
)
