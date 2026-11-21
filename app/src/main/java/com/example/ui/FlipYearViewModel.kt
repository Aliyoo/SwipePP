package com.example.ui

import android.app.Application
import android.app.PendingIntent
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatchMediaEditor
import com.example.data.CleanupStatsEntity
import com.example.data.CompressionPreset
import com.example.data.FlipYearDatabase
import com.example.data.LocalMediaScanner
import com.example.data.LocationBatchUpdate
import com.example.data.MediaAccessLevel
import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.PhotoRepository
import com.example.data.PhotoRestoreSpec
import com.example.data.PhotoStatus
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.data.YearbookCoverConfigEntity
import com.example.data.YearbookCoverMode
import com.example.ui.domain.LibraryDerivationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlipYearViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FlipYearDatabase.getInstance(application)
    private val scanner = LocalMediaScanner(application)
    private val batchEditor = BatchMediaEditor(application)
    private val repository = PhotoRepository(
        dao = database.flipYearDao(),
        scanner = scanner,
        batchEditor = batchEditor
    )

    private val undoStack = MutableStateFlow<List<UndoSnapshot>>(emptyList())
    private val cleanerSessionUndoStack = MutableStateFlow<List<UndoSnapshot>>(emptyList())
    private val mediaFilter = MutableStateFlow(MediaFilter.ALL)
    private val mediaAccessLevel = MutableStateFlow(repository.checkMediaAccess())
    private val hasMediaLocationPermission = MutableStateFlow(repository.hasMediaLocationAccess())
    private val isScanningDevice = MutableStateFlow(false)
    private val gpsIndexProgress = MutableStateFlow(GpsIndexProgress())
    private val batchTaskProgress = MutableStateFlow(BatchTaskProgress())
    private val snackbarMessage = MutableStateFlow<SnackbarMessage?>(null)
    private val recentlyStampedMonthKey = MutableStateFlow<String?>(null)
    private val vaultSessionFilterIds = MutableStateFlow<Set<Long>?>(null)
    private val customCleanerSession = MutableStateFlow<CustomCleanerSession?>(null)
    private var gpsBackfillJob: Job? = null
    private var snackbarDismissJob: Job? = null
    private var pendingBatchWriteTask: PendingBatchWriteTask? = null
    private var pendingLegacyRetryPhotos: List<PhotoItemEntity> = emptyList()
    private var accumulatedLegacyFreedBytes: Long = 0L
    private var accumulatedLegacyDeletedCount: Int = 0

    private sealed interface PendingBatchWriteTask {
        data class TagLocations(
            val updates: List<Pair<PhotoItemEntity, LocationBatchUpdate>>,
            val summaryLabel: String
        ) : PendingBatchWriteTask
    }

    private data class GpsIndexProgress(
        val isIndexing: Boolean = false,
        val processed: Int = 0,
        val total: Int = 0
    )

    private data class BatchTaskProgress(
        val message: String? = null,
        val processed: Int = 0,
        val total: Int = 0
    )

    private data class UiMeta(
        val undoDepth: Int,
        val cleanerSessionUndoDepth: Int,
        val access: MediaAccessLevel,
        val hasLocationPerm: Boolean,
        val scanning: Boolean,
        val gpsProgress: GpsIndexProgress,
        val batchProgress: BatchTaskProgress,
        val snackbar: SnackbarMessage?,
        val recentStamp: String?,
        val vaultSessionFilter: Set<Long>?,
        val customSession: CustomCleanerSession?
    )

    private data class DerivedLibraryData(
        val filter: MediaFilter,
        val allPhotos: List<PhotoItemEntity>,
        val galleryEntries: List<GalleryEntry>,
        val yearbooks: List<YearbookSummary>,
        val monthStamps: Map<String, MonthStampEntity>,
        val trashedPhotos: List<PhotoItemEntity>,
        val locationClusters: List<LocationCluster>,
        val unlocatedPhotos: List<PhotoItemEntity>,
        val stats: CleanupStatsEntity,
        val totalImageCount: Int,
        val totalVideoCount: Int,
        val hasDeviceMedia: Boolean,
        val hasSamplePhotos: Boolean,
        val pendingTrashBytes: Long
    )

    private val metaFlow = combine(
        combine(undoStack, cleanerSessionUndoStack) { global, session -> global.size to session.size },
        combine(mediaAccessLevel, hasMediaLocationPermission) { a, loc -> a to loc },
        combine(isScanningDevice, gpsIndexProgress, batchTaskProgress) { scan, gps, batch ->
            Triple(scan, gps, batch)
        },
        combine(snackbarMessage, recentlyStampedMonthKey, vaultSessionFilterIds, customCleanerSession) { snack, stamp, filterIds, custom ->
            listOf(snack, stamp, filterIds, custom)
        }
    ) { undoPair, accessPair, workTriple, feedbackList ->
        @Suppress("UNCHECKED_CAST")
        UiMeta(
            undoDepth = undoPair.first,
            cleanerSessionUndoDepth = undoPair.second,
            access = accessPair.first,
            hasLocationPerm = accessPair.second,
            scanning = workTriple.first,
            gpsProgress = workTriple.second,
            batchProgress = workTriple.third,
            snackbar = feedbackList[0] as SnackbarMessage?,
            recentStamp = feedbackList[1] as String?,
            vaultSessionFilter = feedbackList[2] as Set<Long>?,
            customSession = feedbackList[3] as CustomCleanerSession?
        )
    }

    /**
     * Heavy O(N) derivations over 10,000+ photos are strictly decoupled from transient UI meta
     * (banners, GPS progress counter, undo depth), conflated during rapid bursts, and executed
     * completely off the main thread on [Dispatchers.Default].
     */
    private val derivedLibraryFlow = combine(
        repository.allActivePhotos,
        repository.trashedPhotos,
        combine(repository.monthStamps, repository.yearbookCovers) { stamps, covers -> stamps to covers },
        repository.cleanupStats,
        mediaFilter
    ) { activePhotos, trashed, stampsAndCovers, stats, filter ->
        val (stamps, coverConfigs) = stampsAndCovers
        var totalImageCount = 0
        var totalVideoCount = 0
        var hasDeviceMedia = false
        var hasSamplePhotos = false
        for (p in activePhotos) {
            if (p.isVideo) totalVideoCount++ else totalImageCount++
            if (p.isSample) hasSamplePhotos = true else hasDeviceMedia = true
        }
        if (!hasSamplePhotos) {
            for (t in trashed) {
                if (t.isSample) {
                    hasSamplePhotos = true
                    break
                }
            }
        }

        val filtered = when (filter) {
            MediaFilter.ALL -> activePhotos
            MediaFilter.PHOTOS -> activePhotos.filter { !it.isVideo }
            MediaFilter.VIDEOS -> activePhotos.filter { it.isVideo }
        }

        val stampMap = HashMap<String, MonthStampEntity>(stamps.size * 2)
        for (s in stamps) {
            stampMap[s.yearMonthKey] = s
        }

        val coverConfigMap = HashMap<Int, YearbookCoverConfigEntity>(coverConfigs.size * 2)
        for (c in coverConfigs) {
            coverConfigMap[c.year] = c
        }

        val yearbooks = LibraryDerivationEngine.buildYearbooksLinear(filtered, stampMap, coverConfigMap)

        val visibleGallery = ArrayList<PhotoItemEntity>(filtered.size)
        for (p in filtered) {
            if (p.photoStatus != PhotoStatus.TRASHED) {
                visibleGallery.add(p)
            }
        }

        val galleryEntries = LibraryDerivationEngine.buildGalleryEntriesLinear(visibleGallery)
        val (clusters, unlocated) = LibraryDerivationEngine.buildLocationClustersAndUnlocatedLinear(visibleGallery)

        var pendingBytes = 0L
        for (t in trashed) {
            pendingBytes += t.fileSizeBytes
        }

        DerivedLibraryData(
            filter = filter,
            allPhotos = filtered,
            galleryEntries = galleryEntries,
            yearbooks = yearbooks,
            monthStamps = stampMap,
            trashedPhotos = trashed,
            locationClusters = clusters,
            unlocatedPhotos = unlocated,
            stats = stats,
            totalImageCount = totalImageCount,
            totalVideoCount = totalVideoCount,
            hasDeviceMedia = hasDeviceMedia,
            hasSamplePhotos = hasSamplePhotos,
            pendingTrashBytes = pendingBytes
        )
    }
        .conflate()
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<FlipYearUiState> = combine(
        derivedLibraryFlow,
        metaFlow
    ) { data, meta ->
        FlipYearUiState(
            isLoading = false,
            isScanningDevice = meta.scanning,
            isIndexingGps = meta.gpsProgress.isIndexing,
            gpsIndexedCount = meta.gpsProgress.processed,
            gpsTotalCount = meta.gpsProgress.total,
            mediaAccessLevel = meta.access,
            hasMediaLocationPermission = meta.hasLocationPerm,
            mediaFilter = data.filter,
            allPhotos = data.allPhotos,
            galleryEntries = data.galleryEntries,
            yearbooks = data.yearbooks,
            monthStamps = data.monthStamps,
            trashedPhotos = data.trashedPhotos,
            locationClusters = data.locationClusters,
            unlocatedPhotos = data.unlocatedPhotos,
            stats = data.stats,
            totalImageCount = data.totalImageCount,
            totalVideoCount = data.totalVideoCount,
            hasDeviceMedia = data.hasDeviceMedia,
            hasSamplePhotos = data.hasSamplePhotos,
            pendingTrashBytes = data.pendingTrashBytes,
            undoStackDepth = meta.undoDepth,
            cleanerSessionUndoDepth = meta.cleanerSessionUndoDepth,
            snackbarMessage = meta.snackbar,
            feedbackBanner = meta.snackbar?.text,
            recentlyStampedMonthKey = meta.recentStamp,
            vaultSessionFilterIds = meta.vaultSessionFilter,
            customCleanerSession = meta.customSession,
            batchTaskMessage = meta.batchProgress.message,
            batchTaskProcessed = meta.batchProgress.processed,
            batchTaskTotal = meta.batchProgress.total
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlipYearUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            val level = repository.checkMediaAccess()
            mediaAccessLevel.value = level
            hasMediaLocationPermission.value = repository.hasMediaLocationAccess()
            if (level != MediaAccessLevel.NONE) {
                // If Room already has cached device media, let the initial 3D Bookshelf settle at 60/120fps
                // before running the background incremental MediaStore diff scan.
                if (repository.hasCachedDeviceMedia()) {
                    delay(650L)
                }
                scanDeviceMedia(silent = true)
            }
        }
    }

    fun requiredMediaPermissions(): Array<String> = repository.requiredMediaPermissions()

    fun refreshPermissionAndScan(silent: Boolean = false) {
        val prevLocationAccess = hasMediaLocationPermission.value
        val level = repository.checkMediaAccess()
        val nowLocationAccess = repository.hasMediaLocationAccess()
        mediaAccessLevel.value = level
        hasMediaLocationPermission.value = nowLocationAccess
        val newlyGrantedLocation = !prevLocationAccess && nowLocationAccess
        if (level != MediaAccessLevel.NONE) {
            scanDeviceMedia(silent = silent, forceRescanGps = newlyGrantedLocation)
        }
    }

    fun scanDeviceMedia(silent: Boolean = false, forceRescanGps: Boolean = false) {
        if (isScanningDevice.value) return
        viewModelScope.launch {
            isScanningDevice.value = true
            try {
                hasMediaLocationPermission.value = repository.hasMediaLocationAccess()
                val result = repository.syncLocalDeviceMedia()
                if (!silent) {
                    if (result.totalFound > 0) {
                        showBanner("已扫描本地 ${result.imageCount} 张照片与 ${result.videoCount} 个视频")
                    } else {
                        showBanner("当前设备相册暂无本地照片或视频，可加载示例影集体验")
                    }
                }
                if (result.totalFound > 0) {
                    startFullGpsBackfill(
                        notifyResult = !silent || forceRescanGps,
                        forceRescan = forceRescanGps
                    )
                }
            } finally {
                isScanningDevice.value = false
            }
        }
    }

    /**
     * Runs batched background GPS indexing only on device items with `gpsScanned = false`
     * (unless `forceRescan = true`), so visiting Footprint on a 10,000-photo library is instant
     * once initial indexing completes.
     */
    fun startFullGpsBackfill(notifyResult: Boolean = false, forceRescan: Boolean = false) {
        if (gpsBackfillJob?.isActive == true) {
            if (!forceRescan) return
            gpsBackfillJob?.cancel()
        }
        hasMediaLocationPermission.value = repository.hasMediaLocationAccess()
        if (repository.checkMediaAccess() == MediaAccessLevel.NONE) return

        gpsBackfillJob = viewModelScope.launch {
            try {
                val newlyLocated = repository.backfillAllMissingGpsLocations(
                    forceRescanUnlocated = forceRescan
                ) { processed, total, _ ->
                    if (total > 0) {
                        gpsIndexProgress.value = GpsIndexProgress(
                            isIndexing = processed < total,
                            processed = processed,
                            total = total
                        )
                    }
                }
                if (notifyResult && newlyLocated > 0) {
                    showBanner("已完成全量定位解析，新增 $newlyLocated 项足迹媒体")
                } else if (notifyResult && forceRescan) {
                    showBanner("全量定位检查完成，当前足迹已是最新")
                }
            } finally {
                gpsIndexProgress.value = GpsIndexProgress(isIndexing = false, processed = 0, total = 0)
            }
        }
    }

    fun loadDemoArchive() {
        viewModelScope.launch {
            undoStack.value = emptyList()
            cleanerSessionUndoStack.value = emptyList()
            repository.loadDemoArchive()
            showSnackbar("已加载示例照片与 4K 视频影集")
        }
    }

    fun removeDemoArchive() {
        viewModelScope.launch {
            val removed = repository.removeSamplePhotos()
            undoStack.value = emptyList()
            cleanerSessionUndoStack.value = emptyList()
            if (removed > 0) {
                showSnackbar("已移除示例影集（共 $removed 项）")
            } else {
                showSnackbar("当前没有示例影集")
            }
        }
    }

    fun setVaultSessionFilter(photoIds: Set<Long>?) {
        vaultSessionFilterIds.value = photoIds?.takeIf { it.isNotEmpty() }
    }

    fun resetCleanerSessionUndo() {
        cleanerSessionUndoStack.value = emptyList()
    }

    fun setMediaFilter(filter: MediaFilter) {
        mediaFilter.value = filter
    }

    fun saveYearbookCover(
        year: Int,
        mode: YearbookCoverMode,
        selectedPhotoIds: List<Long>,
        intervalSeconds: Int
    ) {
        viewModelScope.launch {
            val sanitizedInterval = intervalSeconds.coerceIn(3, 12)
            repository.saveYearbookCover(
                year = year,
                mode = mode,
                selectedPhotoIds = selectedPhotoIds,
                intervalSeconds = sanitizedInterval
            )
            val msg = when (mode) {
                YearbookCoverMode.SMART_CAROUSEL -> "${year}年已切换为智能精选轮播（每${sanitizedInterval}秒）"
                YearbookCoverMode.SINGLE_CUSTOM -> "已锁定 ${year}年专属单图封面"
                YearbookCoverMode.CUSTOM_CAROUSEL -> "已保存 ${year}年 ${selectedPhotoIds.size} 张自选轮播封面"
            }
            showBanner(msg)
        }
    }

    fun resetYearbookCoverToSmart(year: Int) {
        viewModelScope.launch {
            repository.resetYearbookCover(year)
            showBanner("${year}年已恢复智能精选轮播封面")
        }
    }

    fun setCustomCleanerSession(title: String, photoIds: Set<Long>, includeReviewed: Boolean = true) {
        customCleanerSession.value = if (photoIds.isEmpty()) {
            null
        } else {
            CustomCleanerSession(
                title = title,
                photoIds = photoIds,
                includeReviewed = includeReviewed
            )
        }
    }

    fun clearCustomCleanerSession() {
        customCleanerSession.value = null
    }

    fun getMonthPagesForYear(year: Int, state: FlipYearUiState = uiState.value): List<MonthPageModel> {
        return LibraryDerivationEngine.buildMonthPagesForYear(
            year = year,
            allPhotos = state.allPhotos,
            monthStamps = state.monthStamps
        )
    }

    fun buildArcQueue(
        year: Int?,
        month: Int?,
        locationName: String?,
        includeReviewed: Boolean = false,
        state: FlipYearUiState = uiState.value,
        customPhotoIds: Set<Long>? = null
    ): List<CleanupCardItem> {
        return LibraryDerivationEngine.buildArcQueue(
            allPhotos = state.allPhotos,
            year = year,
            month = month,
            locationName = locationName,
            includeReviewed = includeReviewed,
            customPhotoIds = customPhotoIds
        )
    }

    fun swipeTrashPhoto(
        photo: PhotoItemEntity,
        fromCleanerSession: Boolean = false
    ) {
        val mediaNoun = if (photo.isVideo) "视频" else "照片"
        val snapshot = UndoSnapshot(
            photoStates = listOf(
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = photo.status,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName
                )
            ),
            description = "已撤回${mediaNoun}移入回收站",
            actionLabel = "撤销：移入回收站 1 项",
            cardKey = "single_${photo.id}"
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = fromCleanerSession)
            val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.TRASHED, photo.year, photo.month)
            if (!fromCleanerSession) {
                showSnackbar(
                    text = "已将 1 项${mediaNoun}移入回收站",
                    undoSnapshot = snapshot
                )
            }
            if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
        }
    }

    fun keepPhoto(
        photo: PhotoItemEntity,
        markFavorite: Boolean = false,
        fromCleanerSession: Boolean = false
    ) {
        val actionText = if (markFavorite) "收藏并保留" else "保留"
        val snapshot = UndoSnapshot(
            photoStates = listOf(
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = photo.status,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName
                )
            ),
            description = "已撤回${actionText}操作",
            actionLabel = "撤销：${actionText} 1 项",
            cardKey = "single_${photo.id}"
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = fromCleanerSession)
            if (markFavorite && !photo.isFavorite) {
                repository.toggleFavorite(photo.id, true)
            }
            val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.KEPT, photo.year, photo.month)
            if (!fromCleanerSession) {
                showSnackbar(
                    text = if (markFavorite) "已收藏并保留 1 项" else "已标记保留 1 项",
                    undoSnapshot = snapshot
                )
            }
            if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
        }
    }

    fun toggleFavoritePhoto(photo: PhotoItemEntity, newFavorite: Boolean) {
        val actionText = if (newFavorite) "收藏" else "取消收藏"
        val snapshot = UndoSnapshot(
            photoStates = listOf(
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = photo.status,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName
                )
            ),
            description = "已撤回${actionText}",
            actionLabel = "撤销：${actionText} 1 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = false)
            repository.toggleFavorite(photo.id, newFavorite)
            if (newFavorite && photo.photoStatus == PhotoStatus.UNREVIEWED) {
                val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.KEPT, photo.year, photo.month)
                if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
            }
            showSnackbar(
                text = if (newFavorite) "已加入收藏" else "已取消收藏",
                undoSnapshot = snapshot
            )
        }
    }

    fun keepEntireBurstStack(
        stack: CleanupCardItem.BurstStack,
        markFavorite: Boolean = false,
        fromCleanerSession: Boolean = true
    ) {
        val actionText = if (markFavorite) "收藏并保留整组" else "保留整组"
        val snapshot = UndoSnapshot(
            photoStates = stack.photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = "已撤回${actionText} (${stack.photos.size} 张)",
            actionLabel = "撤销：保留连拍 ${stack.photos.size} 张",
            cardKey = stack.stableKey
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = fromCleanerSession)
            if (markFavorite) {
                repository.toggleFavorite(stack.bestPhoto.id, true)
            }
            val months = stack.photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = stack.photos.map { it.id },
                status = PhotoStatus.KEPT,
                affectedMonths = months
            )
            if (!fromCleanerSession) {
                showSnackbar(
                    text = "已保留整组 ${stack.photos.size} 张连拍照片",
                    undoSnapshot = snapshot
                )
            }
            if (newlyStamped) triggerMonthStamped(stack.bestPhoto.year, stack.bestPhoto.month)
        }
    }

    fun keepBestAndTrashStackOthers(
        stack: CleanupCardItem.BurstStack,
        chosenBest: PhotoItemEntity = stack.bestPhoto,
        fromCleanerSession: Boolean = false
    ) {
        val others = stack.photos.filter { it.id != chosenBest.id }
        val snapshot = UndoSnapshot(
            photoStates = stack.photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = "已还原 ${stack.photos.size} 张连拍照片",
            actionLabel = "撤销：仅留最佳（移入回收站 ${others.size} 张）",
            cardKey = stack.stableKey
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = fromCleanerSession)
            repository.setBestInStack(stack.groupId, chosenBest.id)
            val newlyStamped = repository.keepBestAndTrashOthers(chosenBest, others)
            showSnackbar(
                text = "已仅留最佳照片，${others.size} 张相似连拍移入回收站",
                undoSnapshot = snapshot
            )
            if (newlyStamped) triggerMonthStamped(chosenBest.year, chosenBest.month)
        }
    }

    fun selectBestInStack(groupId: String, photoId: Long) {
        viewModelScope.launch {
            repository.setBestInStack(groupId, photoId)
        }
    }

    fun buildGalleryEntries(photos: List<PhotoItemEntity>): List<GalleryEntry> {
        return LibraryDerivationEngine.buildGalleryEntries(photos)
    }

    fun keepBestInGalleryBurst(burst: GalleryEntry.Burst, chosenBest: PhotoItemEntity = burst.bestPhoto) {
        keepBestAndTrashStackOthers(
            stack = CleanupCardItem.BurstStack(
                groupId = burst.groupId,
                photos = burst.photos,
                bestPhoto = burst.bestPhoto
            ),
            chosenBest = chosenBest
        )
    }

    fun trashEntireStack(stack: CleanupCardItem.BurstStack, fromCleanerSession: Boolean = true) {
        val snapshot = UndoSnapshot(
            photoStates = stack.photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = "已还原整组连拍 (${stack.photos.size} 张)",
            actionLabel = "撤销：移入回收站 ${stack.photos.size} 张",
            cardKey = stack.stableKey
        )
        viewModelScope.launch {
            pushUndo(snapshot, inCleanerSession = fromCleanerSession)
            val months = stack.photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = stack.photos.map { it.id },
                status = PhotoStatus.TRASHED,
                affectedMonths = months
            )
            if (!fromCleanerSession) {
                showSnackbar(
                    text = "已将整组 ${stack.photos.size} 张连拍移入回收站",
                    undoSnapshot = snapshot
                )
            }
            if (newlyStamped) triggerMonthStamped(stack.bestPhoto.year, stack.bestPhoto.month)
        }
    }

    fun bulkTrashPhotos(photos: List<PhotoItemEntity>) {
        if (photos.isEmpty()) return
        val snapshot = UndoSnapshot(
            photoStates = photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = "已撤回移入回收站 (${photos.size} 项)",
            actionLabel = "撤销：移入回收站 ${photos.size} 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)
            val months = photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = photos.map { it.id },
                status = PhotoStatus.TRASHED,
                affectedMonths = months
            )
            val mb = photos.sumOf { it.fileSizeBytes } / (1024.0 * 1024.0)
            showSnackbar(
                text = String.format("已将 %d 项移入回收站 (%.1f MB)", photos.size, mb),
                undoSnapshot = snapshot
            )
            if (newlyStamped) {
                val first = photos.first()
                triggerMonthStamped(first.year, first.month)
            }
        }
    }

    fun bulkKeepPhotos(photos: List<PhotoItemEntity>, markFavorite: Boolean = false) {
        if (photos.isEmpty()) return
        val snapshot = UndoSnapshot(
            photoStates = photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = if (markFavorite) "已撤回批量收藏 (${photos.size} 项)" else "已撤回批量保留 (${photos.size} 项)",
            actionLabel = if (markFavorite) "撤销：收藏 ${photos.size} 项" else "撤销：保留 ${photos.size} 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)
            val ids = photos.map { it.id }
            if (markFavorite) {
                repository.togglePhotosFavorite(ids, true)
            }
            val months = photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = ids,
                status = PhotoStatus.KEPT,
                affectedMonths = months
            )
            showSnackbar(
                text = if (markFavorite) "已收藏并保留 ${photos.size} 项媒体" else "已批量保留 ${photos.size} 项媒体",
                undoSnapshot = snapshot
            )
            if (newlyStamped) {
                val first = photos.first()
                triggerMonthStamped(first.year, first.month)
            }
        }
    }

    fun bulkMovePhotos(
        photos: List<PhotoItemEntity>,
        targetAlbumName: String?,
        targetYear: Int? = null,
        targetMonth: Int? = null
    ) {
        if (photos.isEmpty()) return
        val snapshot = UndoSnapshot(
            photoStates = photos.map {
                PhotoStateBackup(
                    photoId = it.id,
                    year = it.year,
                    month = it.month,
                    previousStatus = it.status,
                    previousFavorite = it.isFavorite,
                    previousBucketName = it.bucketName
                )
            },
            description = "已撤回移动 (${photos.size} 项)",
            actionLabel = "撤销：移动 ${photos.size} 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)
            repository.movePhotos(
                photos = photos,
                targetAlbumName = targetAlbumName?.trim()?.takeIf { it.isNotBlank() },
                targetYear = targetYear,
                targetMonth = targetMonth
            )
            val destDesc = buildString {
                if (!targetAlbumName.isNullOrBlank()) append("「${targetAlbumName.trim()}」")
                if (targetYear != null && targetMonth != null) {
                    if (isNotEmpty()) append(" · ")
                    append("${targetYear}年${targetMonth}月影集")
                }
            }.ifBlank { "目标相册" }
            showSnackbar(
                text = "已将 ${photos.size} 项移动至 $destDesc",
                undoSnapshot = snapshot
            )
        }
    }

    /**
     * Stage 1.3: Safe non-destructive compression writes a NEW MediaStore file and moves the
     * original photo into FlipYear's Trash Vault, so no destructive overwrite permission dialog
     * is required and originals remain recoverable.
     */
    fun requestBatchCompress(
        photos: List<PhotoItemEntity>,
        preset: CompressionPreset
    ): PendingIntent? {
        val imageTargets = photos.filter { !it.isVideo }
        if (imageTargets.isEmpty()) {
            showSnackbar("所选项目中暂无可压缩的照片")
            return null
        }
        executeBatchCompress(photos = photos, preset = preset)
        return null
    }

    fun requestBatchManualLocation(
        photos: List<PhotoItemEntity>,
        locationName: String,
        latitude: Double,
        longitude: Double,
        writeExif: Boolean
    ): PendingIntent? {
        val cleanName = locationName.trim()
        if (photos.isEmpty() || cleanName.isEmpty()) return null
        val updates = photos.map { photo ->
            photo to LocationBatchUpdate(
                id = photo.id,
                locationName = cleanName,
                latitude = latitude,
                longitude = longitude
            )
        }
        val writeIntent = if (writeExif) repository.buildSystemWriteIntent(photos) else null
        return if (writeIntent != null) {
            pendingBatchWriteTask = PendingBatchWriteTask.TagLocations(
                updates = updates,
                summaryLabel = cleanName
            )
            writeIntent
        } else {
            executeBatchLocationUpdates(
                updates = updates,
                writeExif = writeExif,
                summaryLabel = cleanName,
                writeDeclinedFallback = false
            )
            null
        }
    }

    fun requestBatchSameDayLocation(
        matches: List<SameDayLocationMatch>,
        writeExif: Boolean
    ): PendingIntent? {
        if (matches.isEmpty()) return null
        val updates = matches.map { match ->
            match.photo to LocationBatchUpdate(
                id = match.photo.id,
                locationName = match.matchedLocationName,
                latitude = match.matchedLatitude,
                longitude = match.matchedLongitude
            )
        }
        val targetPhotos = matches.map { it.photo }
        val writeIntent = if (writeExif) repository.buildSystemWriteIntent(targetPhotos) else null
        return if (writeIntent != null) {
            pendingBatchWriteTask = PendingBatchWriteTask.TagLocations(
                updates = updates,
                summaryLabel = "同日智能推断"
            )
            writeIntent
        } else {
            executeBatchLocationUpdates(
                updates = updates,
                writeExif = writeExif,
                summaryLabel = "同日智能推断",
                writeDeclinedFallback = false
            )
            null
        }
    }

    fun confirmPendingBatchWrite(granted: Boolean) {
        val task = pendingBatchWriteTask ?: return
        pendingBatchWriteTask = null
        when (task) {
            is PendingBatchWriteTask.TagLocations -> {
                executeBatchLocationUpdates(
                    updates = task.updates,
                    writeExif = granted,
                    summaryLabel = task.summaryLabel,
                    writeDeclinedFallback = !granted
                )
            }
        }
    }

    suspend fun geocodeLocationQuery(query: String): ResolvedGeoLocation? {
        return repository.geocodeLocationQuery(query)
    }

    private fun executeBatchCompress(
        photos: List<PhotoItemEntity>,
        preset: CompressionPreset
    ) {
        if (batchTaskProgress.value.message != null) {
            showSnackbar("已有批量处理任务正在进行，请稍候")
            return
        }
        viewModelScope.launch {
            val imageCount = photos.count { !it.isVideo }
            batchTaskProgress.value = BatchTaskProgress(
                message = "正在批量压缩 (0/$imageCount)",
                processed = 0,
                total = imageCount
            )
            try {
                val result = repository.compressPhotosNearLossless(
                    photos = photos,
                    preset = preset
                ) { processed, total, _ ->
                    batchTaskProgress.value = BatchTaskProgress(
                        message = "正在批量压缩 ($processed/$total)",
                        processed = processed,
                        total = total
                    )
                }
                val compressedCount = result.compressedUpdates.size
                val savedMb = result.totalSavedBytes / (1024.0 * 1024.0)
                val summary = when {
                    compressedCount > 0 -> buildString {
                        append(String.format("已另存 %d 张压缩照片，节省 %.1f MB；原图已移入回收站可随时还原", compressedCount, savedMb))
                        if (result.skippedAlreadyOptimalCount > 0) {
                            append("（${result.skippedAlreadyOptimalCount} 张已是最优跳过）")
                        }
                    }
                    result.skippedAlreadyOptimalCount > 0 ->
                        "所选 ${result.skippedAlreadyOptimalCount} 张照片体积已最优，无需重复压缩"
                    else ->
                        "未能压缩所选照片，请确认存储空间充足"
                }
                showSnackbar(summary, durationMs = 4500L)
            } finally {
                batchTaskProgress.value = BatchTaskProgress()
            }
        }
    }

    private fun executeBatchLocationUpdates(
        updates: List<Pair<PhotoItemEntity, LocationBatchUpdate>>,
        writeExif: Boolean,
        summaryLabel: String,
        writeDeclinedFallback: Boolean
    ) {
        if (updates.isEmpty()) return
        if (batchTaskProgress.value.message != null) {
            showSnackbar("已有批量处理任务正在进行，请稍候")
            return
        }
        val snapshot = UndoSnapshot(
            photoStates = updates.map { (photo, _) ->
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = photo.status,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName,
                    restoreLocation = true,
                    previousLocationName = photo.locationName,
                    previousLatitude = photo.latitude,
                    previousLongitude = photo.longitude,
                    previousGpsScanned = photo.gpsScanned
                )
            },
            description = "已撤回批量定位更新 (${updates.size} 项)",
            actionLabel = "撤销：定位 ${updates.size} 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)

            if (writeExif) {
                batchTaskProgress.value = BatchTaskProgress(
                    message = "正在写入照片定位 (0/${updates.size})",
                    processed = 0,
                    total = updates.size
                )
            }

            try {
                val exifWritten = repository.applyManualLocationsBatch(
                    updates = updates,
                    writeExif = writeExif
                ) { processed, total ->
                    if (total > 0) {
                        batchTaskProgress.value = BatchTaskProgress(
                            message = "正在写入照片 EXIF 定位 ($processed/$total)",
                            processed = processed,
                            total = total
                        )
                    }
                }
                val msg = when {
                    writeDeclinedFallback ->
                        "已为 ${updates.size} 项更新影集定位至「$summaryLabel」（未修改原图 EXIF）"
                    writeExif && exifWritten > 0 ->
                        "已为 ${updates.size} 项补全定位「$summaryLabel」（已同步写入 $exifWritten 张原图 EXIF）"
                    else ->
                        "已为 ${updates.size} 项补全定位「$summaryLabel」"
                }
                showSnackbar(text = msg, undoSnapshot = snapshot)
            } finally {
                batchTaskProgress.value = BatchTaskProgress()
            }
        }
    }

    /**
     * Stage 1.4: Message-bound undo restores ONLY the exact snapshot attached to a Snackbar,
     * leaving any other unrelated undo entries intact.
     */
    fun undoBySnapshot(snapshot: UndoSnapshot) {
        undoStack.value = undoStack.value.filterNot { it.id == snapshot.id }
        cleanerSessionUndoStack.value = cleanerSessionUndoStack.value.filterNot { it.id == snapshot.id }
        dismissBanner()
        viewModelScope.launch {
            applyUndoSnapshot(snapshot, triggeredByShake = false)
        }
    }

    /**
     * Stage 1.4: Shake-to-undo and cleaner undo button operate strictly within the active
     * ArcCleaner session stack (never rolling back unrelated actions from Gallery/Vault).
     * Returns the restored cardKey if any, so ArcCleanerScreen can unmark it in Revisit mode.
     */
    fun undoCleanerSessionAction(triggeredByShake: Boolean = false): String? {
        val current = cleanerSessionUndoStack.value
        if (current.isEmpty()) return null
        val snapshot = current.last()
        cleanerSessionUndoStack.value = current.dropLast(1)
        undoStack.value = undoStack.value.filterNot { it.id == snapshot.id }
        viewModelScope.launch {
            applyUndoSnapshot(snapshot, triggeredByShake = triggeredByShake)
        }
        return snapshot.cardKey
    }

    fun undoLastAction(triggeredByShake: Boolean = false): Boolean {
        val current = undoStack.value
        if (current.isEmpty()) return false
        val snapshot = current.last()
        undoStack.value = current.dropLast(1)
        cleanerSessionUndoStack.value = cleanerSessionUndoStack.value.filterNot { it.id == snapshot.id }
        viewModelScope.launch {
            applyUndoSnapshot(snapshot, triggeredByShake = triggeredByShake)
        }
        return true
    }

    private suspend fun applyUndoSnapshot(snapshot: UndoSnapshot, triggeredByShake: Boolean) {
        val specs = snapshot.photoStates.map { backup ->
            val targetStatus = runCatching { PhotoStatus.valueOf(backup.previousStatus) }
                .getOrDefault(PhotoStatus.UNREVIEWED)
            PhotoRestoreSpec(
                photoId = backup.photoId,
                status = targetStatus.name,
                trashedAtMillis = if (targetStatus == PhotoStatus.TRASHED) System.currentTimeMillis() else null,
                isFavorite = backup.previousFavorite,
                bucketName = backup.previousBucketName,
                year = backup.year,
                month = backup.month,
                restoreLocation = backup.restoreLocation,
                locationName = backup.previousLocationName,
                latitude = backup.previousLatitude,
                longitude = backup.previousLongitude,
                gpsScanned = backup.previousGpsScanned
            )
        }
        val affectedMonths = snapshot.photoStates.map { it.year to it.month }.toSet()
        repository.restorePhotoStatesBatch(specs, affectedMonths)
        val msg = if (triggeredByShake) {
            "已摇动撤销 · ${snapshot.description}"
        } else {
            snapshot.description
        }
        showSnackbar(msg)
    }

    fun restoreFromVault(photo: PhotoItemEntity) {
        val snapshot = UndoSnapshot(
            photoStates = listOf(
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = PhotoStatus.TRASHED.name,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName
                )
            ),
            description = "已撤回还原，放回回收站",
            actionLabel = "撤销：还原 1 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)
            repository.restorePhoto(photo)
            showSnackbar(
                text = "已放回 ${photo.year}年${photo.month}月 影集",
                undoSnapshot = snapshot
            )
        }
    }

    fun restoreAllFromVault(targets: List<PhotoItemEntity> = uiState.value.trashedPhotos) {
        if (targets.isEmpty()) return
        val snapshot = UndoSnapshot(
            photoStates = targets.map { photo ->
                PhotoStateBackup(
                    photoId = photo.id,
                    year = photo.year,
                    month = photo.month,
                    previousStatus = PhotoStatus.TRASHED.name,
                    previousFavorite = photo.isFavorite,
                    previousBucketName = photo.bucketName
                )
            },
            description = "已撤回全部还原 (${targets.size} 项)",
            actionLabel = "撤销：全部还原 ${targets.size} 项"
        )
        viewModelScope.launch {
            pushUndo(snapshot)
            repository.restoreAllTrashed(targets)
            showSnackbar(
                text = "已还原 ${targets.size} 项媒体",
                undoSnapshot = snapshot
            )
        }
    }

    fun buildSystemTrashIntentForVault(targets: List<PhotoItemEntity> = uiState.value.trashedPhotos): PendingIntent? {
        return repository.buildSystemTrashIntent(targets)
    }

    fun finalizePurgeTrashed(targets: List<PhotoItemEntity> = uiState.value.trashedPhotos) {
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val freedBytes = repository.purgeTrashedPhotos(targets)
            undoStack.value = emptyList()
            cleanerSessionUndoStack.value = emptyList()
            vaultSessionFilterIds.value = vaultSessionFilterIds.value?.minus(targets.map { it.id }.toSet())?.takeIf { it.isNotEmpty() }
            val mb = freedBytes / (1024.0 * 1024.0)
            showSnackbar(String.format("成功释放 %.1f MB 空间（%d 项）", mb, targets.size))
        }
    }

    /**
     * Stage 1.1: Real deletion path on Android 9 & 10 (SDK < 30).
     * Deletes each item via ContentResolver, triggers API 29 RecoverableSecurityException consent
     * when required, accumulates ONLY actually deleted bytes, and leaves failed items in the Vault.
     */
    fun purgeLegacyPhotos(
        targets: List<PhotoItemEntity>,
        onNeedRecoverableConsent: (IntentSender) -> Unit
    ) {
        if (targets.isEmpty()) return
        pendingLegacyRetryPhotos = emptyList()
        accumulatedLegacyFreedBytes = 0L
        accumulatedLegacyDeletedCount = 0
        runLegacyPurgeBatch(targets, onNeedRecoverableConsent)
    }

    fun continuePendingLegacyPurge(
        granted: Boolean,
        onNeedRecoverableConsent: (IntentSender) -> Unit
    ) {
        val retryList = pendingLegacyRetryPhotos
        pendingLegacyRetryPhotos = emptyList()
        if (retryList.isEmpty()) return
        if (granted) {
            runLegacyPurgeBatch(retryList, onNeedRecoverableConsent)
        } else {
            val failedCount = retryList.size
            val msg = if (accumulatedLegacyDeletedCount > 0) {
                val mb = accumulatedLegacyFreedBytes / (1024.0 * 1024.0)
                String.format(
                    "已永久删除 %d 项（释放 %.1f MB）；%d 项未获系统授权，保留在回收站",
                    accumulatedLegacyDeletedCount,
                    mb,
                    failedCount
                )
            } else {
                "未获得系统删除授权，$failedCount 项保留在回收站"
            }
            accumulatedLegacyFreedBytes = 0L
            accumulatedLegacyDeletedCount = 0
            showSnackbar(msg, durationMs = 4500L)
        }
    }

    private fun runLegacyPurgeBatch(
        targets: List<PhotoItemEntity>,
        onNeedRecoverableConsent: (IntentSender) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.purgeLegacyDevicePhotos(targets)
            accumulatedLegacyFreedBytes += result.freedBytes
            accumulatedLegacyDeletedCount += result.deletedPhotos.size
            if (result.deletedPhotos.isNotEmpty()) {
                val deletedIds = result.deletedPhotos.map { it.id }.toSet()
                vaultSessionFilterIds.value = vaultSessionFilterIds.value?.minus(deletedIds)?.takeIf { it.isNotEmpty() }
                undoStack.value = emptyList()
                cleanerSessionUndoStack.value = emptyList()
            }

            val consentSender = result.recoverableIntentSender
            if (consentSender != null && result.remainingUnprocessedPhotos.isNotEmpty()) {
                pendingLegacyRetryPhotos = result.remainingUnprocessedPhotos
                onNeedRecoverableConsent(consentSender)
                return@launch
            }

            val totalFreed = accumulatedLegacyFreedBytes
            val totalDeleted = accumulatedLegacyDeletedCount
            accumulatedLegacyFreedBytes = 0L
            accumulatedLegacyDeletedCount = 0

            val mb = totalFreed / (1024.0 * 1024.0)
            val failedCount = result.failedPhotos.size
            val summary = when {
                failedCount == 0 && totalDeleted > 0 ->
                    String.format("已永久删除 %d 项，实际释放 %.1f MB 空间", totalDeleted, mb)
                totalDeleted > 0 && failedCount > 0 -> {
                    val reason = result.failureReason ?: "权限不足"
                    String.format(
                        "已删除 %d 项（释放 %.1f MB）；%d 项删除失败保留在回收站（%s）",
                        totalDeleted,
                        mb,
                        failedCount,
                        reason
                    )
                }
                failedCount > 0 -> {
                    val reason = result.failureReason ?: "系统拒绝删除"
                    "未能删除所选 $failedCount 项（$reason），已保留在回收站"
                }
                else -> "回收站暂无可清理项目"
            }
            showSnackbar(summary, durationMs = 4500L)
        }
    }

    fun dismissBanner() {
        snackbarDismissJob?.cancel()
        snackbarMessage.value = null
    }

    private fun pushUndo(snapshot: UndoSnapshot, inCleanerSession: Boolean = false) {
        undoStack.value = (undoStack.value + snapshot).takeLast(25)
        if (inCleanerSession) {
            cleanerSessionUndoStack.value = (cleanerSessionUndoStack.value + snapshot).takeLast(25)
        }
    }

    private fun triggerMonthStamped(year: Int, month: Int) {
        recentlyStampedMonthKey.value = PhotoRepository.yearMonthKey(year, month)
    }

    private fun showBanner(message: String) {
        showSnackbar(text = message)
    }

    private fun showSnackbar(
        text: String,
        undoSnapshot: UndoSnapshot? = null,
        durationMs: Long = if (undoSnapshot != null) 4500L else 2800L
    ) {
        val msg = SnackbarMessage(
            text = text,
            undoLabel = undoSnapshot?.actionLabel,
            undoSnapshot = undoSnapshot,
            durationMs = durationMs
        )
        snackbarMessage.value = msg
        snackbarDismissJob?.cancel()
        snackbarDismissJob = viewModelScope.launch {
            delay(msg.durationMs)
            if (snackbarMessage.value?.id == msg.id) {
                snackbarMessage.value = null
            }
        }
    }
}
