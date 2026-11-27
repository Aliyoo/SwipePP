package com.example.data

import android.app.PendingIntent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class MediaSyncResult(
    val totalFound: Int,
    val imageCount: Int,
    val videoCount: Int,
    val newlyAddedCount: Int
)

class PhotoRepository(
    private val dao: FlipYearDao,
    private val scanner: LocalMediaScanner,
    private val batchEditor: BatchMediaEditor
) {
    val allActivePhotos: Flow<List<PhotoItemEntity>> = dao.observeAllActivePhotos()
    val trashedPhotos: Flow<List<PhotoItemEntity>> = dao.observeTrashedPhotos()
    val monthStamps: Flow<List<MonthStampEntity>> = dao.observeMonthStamps()
    val cleanupStats: Flow<CleanupStatsEntity> = dao.observeCleanupStats().map {
        it ?: CleanupStatsEntity()
    }
    val yearbookCovers: Flow<List<YearbookCoverConfigEntity>> = dao.observeYearbookCovers()

    suspend fun saveYearbookCover(
        year: Int,
        mode: YearbookCoverMode,
        selectedPhotoIds: List<Long>,
        intervalSeconds: Int,
        collageTemplateId: String = "",
        collageSpacingDp: Float = 4f,
        collageBackdrop: CollageBackdropStyle = CollageBackdropStyle.EDITION,
        slotTransforms: List<CollageSlotTransform> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val sanitizedInterval = intervalSeconds.coerceIn(3, 12)
        val csv = selectedPhotoIds.distinct().joinToString(",")
        dao.upsertYearbookCover(
            YearbookCoverConfigEntity(
                year = year,
                mode = mode.name,
                selectedPhotoIdsCsv = csv,
                intervalSeconds = sanitizedInterval,
                updatedAtMillis = System.currentTimeMillis(),
                collageTemplateId = collageTemplateId,
                collageSpacingDp = collageSpacingDp.coerceIn(0f, 16f),
                collageBackdrop = collageBackdrop.storageKey,
                slotTransformsCsv = encodeSlotTransforms(slotTransforms)
            )
        )
    }

    suspend fun resetYearbookCover(year: Int) = withContext(Dispatchers.IO) {
        dao.deleteYearbookCover(year)
    }

    fun requiredMediaPermissions(): Array<String> = scanner.requiredPermissions()

    fun checkMediaAccess(): MediaAccessLevel = scanner.checkMediaAccess()

    fun hasMediaLocationAccess(): Boolean = scanner.hasMediaLocationAccess()

    suspend fun hasCachedDeviceMedia(): Boolean = withContext(Dispatchers.IO) {
        dao.getDeviceMediaCount() > 0
    }

    suspend fun syncLocalDeviceMedia(): MediaSyncResult = withContext(Dispatchers.IO) {
        val scanned = scanner.scanAllLocalPhotosAndVideos()
        val existingRows = dao.getAllDeviceMediaSyncRowsSync()
        val existingByKey = HashMap<String, DeviceMediaSyncRow>(existingRows.size * 2)
        existingRows.forEach { row ->
            row.mediaStoreKey?.let { existingByKey[it] = row }
        }

        val scannedKeys = HashSet<String>(scanned.size * 2)
        var imageCount = 0
        var videoCount = 0
        val newItems = ArrayList<PhotoItemEntity>()
        val metadataUpdates = ArrayList<PhotoItemEntity>()
        for (item in scanned) {
            if (item.isVideo) videoCount++ else imageCount++
            val key = item.mediaStoreKey
            if (key != null) {
                scannedKeys.add(key)
                val existing = existingByKey[key]
                if (existing == null) {
                    newItems.add(item)
                } else if (deviceMediaMetadataDiffers(existing, item)) {
                    metadataUpdates.add(item)
                }
            }
        }

        val vanishedIds = ArrayList<Long>()
        for (row in existingRows) {
            val key = row.mediaStoreKey
            if (key != null && key !in scannedKeys) {
                vanishedIds.add(row.id)
            }
        }

        val shouldDeleteSamples = scanned.isNotEmpty() && dao.getSamplePhotoCount() > 0

        // Only write to SQLite if there is an actual diff -> zero wasted Room Flow invalidations on warm start!
        if (vanishedIds.isNotEmpty() || newItems.isNotEmpty() || shouldDeleteSamples || metadataUpdates.isNotEmpty()) {
            val invalidateKeys = (newItems + metadataUpdates).map { yearMonthKey(it.year, it.month) }.distinct()
            dao.syncDeviceMediaBatch(
                vanishedIds = vanishedIds,
                newItems = newItems,
                invalidateMonthKeys = invalidateKeys,
                deleteSamples = shouldDeleteSamples,
                metadataUpdates = metadataUpdates
            )
        }

        MediaSyncResult(
            totalFound = scanned.size,
            imageCount = imageCount,
            videoCount = videoCount,
            newlyAddedCount = newItems.size
        )
    }

    /**
     * Background media probe: detects embedded motion photos and animated WebP files,
     * which were moved off the scan critical path so first-scan rows reach the gallery
     * in seconds. Batched writes keep Room invalidations smooth; rows whose probe finds
     * nothing are still marked probed so the pass terminates.
     */
    suspend fun backfillMediaProbe(
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val version = MEDIA_PROBE_VERSION
        val total = dao.countPendingMediaProbe(version)
        if (total == 0) return@withContext 0

        var processed = 0
        val failedIds = ArrayList<Long>()
        onProgress(0, total)
        while (true) {
            val batch = if (failedIds.isEmpty()) {
                dao.getPhotosPendingMediaProbe(version, MEDIA_PROBE_BATCH_SIZE)
            } else {
                dao.getPhotosPendingMediaProbeExcluding(version, MEDIA_PROBE_BATCH_SIZE, failedIds)
            }
            if (batch.isEmpty()) break

            val results = ArrayList<com.example.data.MediaProbeBatchItem>(batch.size)
            val probedOnlyIds = ArrayList<Long>(batch.size)
            for (subChunk in batch.chunked(MEDIA_PROBE_CONCURRENCY)) {
                val resolved = coroutineScope {
                    subChunk.map { item ->
                        async {
                            val outcome = try {
                                scanner.probeImageMedia(item, item.caption.takeIf { it.isNotBlank() }) to null
                            } catch (t: Throwable) {
                                // IO failures (permission not yet granted, transient read errors)
                                // must NOT be recorded as probed — the row stays pending and is
                                // retried on a later pass.
                                null to t
                            }
                            Triple(item.id, outcome.first, outcome.second)
                        }
                    }.map { it.await() }
                }
                for ((id, probe, error) in resolved) {
                    when {
                        error != null -> failedIds.add(id)
                        probe != null -> results.add(
                            com.example.data.MediaProbeBatchItem(
                                id = id,
                                isAnimated = probe.isAnimated,
                                motionPhotoType = probe.motionPhotoType,
                                motionVideoUri = probe.motionVideoUri,
                                motionVideoOffset = probe.motionVideoOffset,
                                motionVideoLength = probe.motionVideoLength
                            )
                        )
                        else -> probedOnlyIds.add(id)
                    }
                }
            }
            dao.applyMediaProbeBatch(results, probedOnlyIds, version)
            processed += results.size + probedOnlyIds.size
            onProgress(processed, total)
        }
        processed
    }

    /**
     * Scans only device photos & videos whose GPS has not yet been scanned (`gpsScanned = 0`),
     * writing results in batched Room transactions so 10,000+ items never flood Flow invalidations
     * and screenshots/non-GPS files are never redundantly re-read on subsequent visits.
     */
    suspend fun backfillAllMissingGpsLocations(
        forceRescanUnlocated: Boolean = false,
        onProgress: (processed: Int, total: Int, newlyLocated: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (forceRescanUnlocated) {
            dao.resetGpsScannedForUnlocated()
        }
        val pending = dao.getDeviceMediaMissingLocationSync()
        val total = pending.size
        if (total == 0) {
            onProgress(0, 0, 0)
            return@withContext 0
        }

        var processed = 0
        var newlyLocated = 0
        onProgress(0, total, 0)

        // Start with a snappy 16-item first batch so initial pins populate rapidly,
        // then scale to 64-item transactions to keep SQLite & Room emissions ultra-smooth.
        var cursor = 0
        var isFirstBatch = true
        while (cursor < total) {
            val batchSize = if (isFirstBatch) 16 else 64
            isFirstBatch = false
            val end = (cursor + batchSize).coerceAtMost(total)
            val batch = pending.subList(cursor, end)
            cursor = end

            val locatedUpdates = ArrayList<LocationBatchUpdate>(batch.size)
            val unlocatedIds = ArrayList<Long>(batch.size)

            // Bounded concurrency (6 parallel IO readers) keeps EXIF/video reads fast without starving Coil
            for (subChunk in batch.chunked(6)) {
                val resolvedPairs = coroutineScope {
                    subChunk.map { item ->
                        async {
                            item.id to scanner.resolveLocationForItem(item)
                        }
                    }.map { it.await() }
                }
                for ((id, geo) in resolvedPairs) {
                    processed++
                    if (geo != null) {
                        locatedUpdates.add(
                            LocationBatchUpdate(
                                id = id,
                                locationName = geo.locationName,
                                latitude = geo.latitude,
                                longitude = geo.longitude
                            )
                        )
                        newlyLocated++
                    } else {
                        unlocatedIds.add(id)
                    }
                }
            }

            // Single Room transaction per batch -> 1 Flow invalidation per 64 photos instead of 64!
            dao.applyGpsBatch(located = locatedUpdates, unlocatedIds = unlocatedIds)
            onProgress(processed, total, newlyLocated)
        }
        newlyLocated
    }

    suspend fun loadDemoArchive() {
        dao.deleteSamplePhotos()
        dao.insertPhotos(SampleArchiveSeeder.buildSamplePhotos())
        SampleArchiveSeeder.buildInitialStamps().forEach { dao.upsertMonthStamp(it) }
        if (dao.getCleanupStatsSync() == null) {
            dao.upsertCleanupStats(SampleArchiveSeeder.buildInitialStats())
        }
    }

    suspend fun markPhotoStatus(photoId: Long, status: PhotoStatus, year: Int, month: Int): Boolean {
        val trashedAt = if (status == PhotoStatus.TRASHED) System.currentTimeMillis() else null
        dao.updatePhotoStatus(photoId, status.name, trashedAt)
        return syncMonthStamp(year, month)
    }

    suspend fun markPhotosStatus(ids: List<Long>, status: PhotoStatus, affectedMonths: Set<Pair<Int, Int>>): Boolean {
        if (ids.isEmpty()) return false
        val trashedAt = if (status == PhotoStatus.TRASHED) System.currentTimeMillis() else null
        ids.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
            dao.updatePhotosStatus(chunk, status.name, trashedAt)
        }
        var newlyStamped = false
        affectedMonths.forEach { (y, m) ->
            if (syncMonthStamp(y, m)) newlyStamped = true
        }
        return newlyStamped
    }

    suspend fun toggleFavorite(photoId: Long, isFavorite: Boolean) {
        dao.updatePhotoFavorite(photoId, isFavorite)
    }

    suspend fun togglePhotosFavorite(ids: List<Long>, isFavorite: Boolean) {
        if (ids.isEmpty()) return
        ids.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
            dao.updatePhotosFavorite(chunk, isFavorite)
        }
    }

    suspend fun movePhotos(
        photos: List<PhotoItemEntity>,
        targetAlbumName: String?,
        targetYear: Int?,
        targetMonth: Int?
    ) {
        if (photos.isEmpty()) return
        val ids = photos.map { it.id }
        val affectedMonths = photos.map { it.year to it.month }.toMutableSet()
        if (targetYear != null && targetMonth != null) {
            ids.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
                dao.updatePhotosAlbumAndMonth(chunk, targetAlbumName, targetYear, targetMonth)
            }
            affectedMonths += targetYear to targetMonth
        } else {
            ids.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
                dao.updatePhotosAlbum(chunk, targetAlbumName)
            }
        }
        affectedMonths.forEach { (y, m) -> syncMonthStamp(y, m) }
    }

    suspend fun restorePhotoAlbumAndMonth(
        photoId: Long,
        bucketName: String?,
        year: Int,
        month: Int
    ) {
        dao.updatePhotoAlbumAndMonth(photoId, bucketName, year, month)
        syncMonthStamp(year, month)
    }

    suspend fun restorePhotoStatesBatch(
        restores: List<PhotoRestoreSpec>,
        affectedMonths: Set<Pair<Int, Int>>
    ) {
        if (restores.isEmpty()) return
        dao.restorePhotoStatesBatch(restores)
        affectedMonths.forEach { (y, m) -> syncMonthStamp(y, m) }
    }

    suspend fun setBestInStack(groupId: String, bestPhotoId: Long) {
        dao.setBestInStack(groupId, bestPhotoId)
    }

    suspend fun keepBestAndTrashOthers(
        bestPhoto: PhotoItemEntity,
        others: List<PhotoItemEntity>
    ): Boolean {
        dao.updatePhotoStatus(bestPhoto.id, PhotoStatus.KEPT.name, null)
        if (others.isNotEmpty()) {
            val now = System.currentTimeMillis()
            others.map { it.id }.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
                dao.updatePhotosStatus(
                    ids = chunk,
                    status = PhotoStatus.TRASHED.name,
                    trashedAtMillis = now
                )
            }
        }
        return syncMonthStamp(bestPhoto.year, bestPhoto.month)
    }

    suspend fun restorePhoto(photo: PhotoItemEntity) {
        dao.updatePhotoStatus(photo.id, PhotoStatus.UNREVIEWED.name, null)
        dao.deleteMonthStamp(yearMonthKey(photo.year, photo.month))
    }

    suspend fun restoreAllTrashed(photos: List<PhotoItemEntity>) {
        if (photos.isEmpty()) return
        photos.map { it.id }.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
            dao.updatePhotosStatus(chunk, PhotoStatus.UNREVIEWED.name, null)
        }
        photos.map { yearMonthKey(it.year, it.month) }.distinct().forEach {
            dao.deleteMonthStamp(it)
        }
    }

    fun buildSystemTrashIntent(photos: List<PhotoItemEntity>): PendingIntent? {
        val realMediaUris = photos
            .filter { !it.isSample }
            .flatMap { it.physicalMediaUris }
            .filter { it.startsWith("content://") }
            .distinct()
            .map { Uri.parse(it) }
        return scanner.buildSystemTrashPendingIntent(realMediaUris)
    }

    fun buildSystemWriteIntent(photos: List<PhotoItemEntity>): PendingIntent? {
        return batchEditor.buildSystemWritePendingIntent(photos)
    }

    suspend fun compressPhotosNearLossless(
        photos: List<PhotoItemEntity>,
        preset: CompressionPreset,
        onProgress: (processed: Int, total: Int, savedBytesSoFar: Long) -> Unit
    ): BatchCompressionResult = withContext(Dispatchers.IO) {
        val result = batchEditor.compressPhotosNearLossless(photos, preset, onProgress)
        if (result.compressedUpdates.isNotEmpty()) {
            val newCopies = result.compressedUpdates.mapNotNull { it.newCompressedPhoto }
            val originalIds = result.compressedUpdates.map { it.id }
            if (newCopies.size == originalIds.size) {
                dao.applySafeCompressedPhotosBatch(
                    newCompressedPhotos = newCopies,
                    originalPhotoIdsToTrash = originalIds,
                    trashedAtMillis = System.currentTimeMillis()
                )
            } else {
                dao.applyCompressedPhotosBatch(result.compressedUpdates)
            }
            val saved = result.totalSavedBytes
            if (saved > 0L) {
                val current = dao.getCleanupStatsSync() ?: CleanupStatsEntity()
                dao.upsertCleanupStats(
                    current.copy(
                        totalCompressedSavedBytes = current.totalCompressedSavedBytes + saved,
                        totalCompressedCount = current.totalCompressedCount + result.compressedUpdates.size
                    )
                )
            }
        }
        result
    }

    suspend fun removeSamplePhotos(): Int {
        val removed = dao.deleteSamplePhotos()
        dao.clearAllMonthStamps()
        return removed
    }

    suspend fun applyManualLocationsBatch(
        updates: List<Pair<PhotoItemEntity, LocationBatchUpdate>>,
        writeExif: Boolean,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext 0
        dao.applyManualLocationBatch(updates.map { it.second })
        if (writeExif) {
            batchEditor.writeExifLocationToPhotos(updates, onProgress)
        } else {
            0
        }
    }

    suspend fun geocodeLocationQuery(query: String): ResolvedGeoLocation? {
        return batchEditor.geocodeLocationQuery(query)
    }

    suspend fun purgeLegacyDevicePhotos(photos: List<PhotoItemEntity>): LegacyDeleteResult =
        withContext(Dispatchers.IO) {
            if (photos.isEmpty()) {
                return@withContext LegacyDeleteResult(emptyList(), emptyList())
            }
            val deleteResult = scanner.deleteDevicePhotosLegacy(photos)
            if (deleteResult.deletedPhotos.isNotEmpty()) {
                purgeTrashedPhotos(deleteResult.deletedPhotos)
            }
            deleteResult
        }

    suspend fun purgeTrashedPhotos(photos: List<PhotoItemEntity>): Long {
        if (photos.isEmpty()) return 0L
        val freed = photos.sumOf { it.fileSizeBytes }
        photos.map { it.id }.chunked(FlipYearDao.SQLITE_BIND_CHUNK).forEach { chunk ->
            dao.updatePhotosStatus(chunk, PhotoStatus.PURGED.name, null)
        }
        val current = dao.getCleanupStatsSync() ?: CleanupStatsEntity()
        dao.upsertCleanupStats(
            current.copy(
                totalPurgedBytes = current.totalPurgedBytes + freed,
                totalPurgedCount = current.totalPurgedCount + photos.size
            )
        )
        return freed
    }

    private suspend fun syncMonthStamp(year: Int, month: Int): Boolean {
        val monthPhotos = dao.getPhotosForMonth(year, month)
        val key = yearMonthKey(year, month)
        val hasUnreviewed = monthPhotos.any { it.photoStatus == PhotoStatus.UNREVIEWED }
        return if (!hasUnreviewed && monthPhotos.isNotEmpty()) {
            val freed = monthPhotos.filter { it.photoStatus == PhotoStatus.TRASHED }
                .sumOf { it.fileSizeBytes }
            dao.upsertMonthStamp(
                MonthStampEntity(
                    yearMonthKey = key,
                    year = year,
                    month = month,
                    cleanedAtMillis = System.currentTimeMillis(),
                    freedBytes = freed
                )
            )
            true
        } else {
            dao.deleteMonthStamp(key)
            false
        }
    }

    // Probe-owned fields (isAnimated / embedded motion video data) are intentionally excluded:
    // scan rows never carry them anymore, and the background probe rewrites them — comparing
    // them here would retrigger a metadata update (and probe reset) on every single scan.
    private fun deviceMediaMetadataDiffers(existing: DeviceMediaSyncRow, scanned: PhotoItemEntity): Boolean {
        return existing.durationMs != scanned.durationMs ||
            existing.mimeType != scanned.mimeType ||
            existing.imageFormat != scanned.imageFormat ||
            existing.mediaSubtype != scanned.mediaSubtype ||
            existing.pairedMediaStoreKey != scanned.pairedMediaStoreKey ||
            existing.fileSizeBytes != scanned.fileSizeBytes ||
            existing.widthPx != scanned.widthPx ||
            existing.heightPx != scanned.heightPx ||
            existing.caption != scanned.caption ||
            existing.bucketName != scanned.bucketName
    }

    companion object {
        private const val MEDIA_PROBE_BATCH_SIZE = 48
        private const val MEDIA_PROBE_CONCURRENCY = 8
        fun yearMonthKey(year: Int, month: Int): String =
            "%04d-%02d".format(year, month)
    }
}
