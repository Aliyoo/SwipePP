package com.example.ui

import android.app.Application
import android.app.PendingIntent
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
    private val mediaFilter = MutableStateFlow(MediaFilter.ALL)
    private val mediaAccessLevel = MutableStateFlow(repository.checkMediaAccess())
    private val hasMediaLocationPermission = MutableStateFlow(repository.hasMediaLocationAccess())
    private val isScanningDevice = MutableStateFlow(false)
    private val gpsIndexProgress = MutableStateFlow(GpsIndexProgress())
    private val batchTaskProgress = MutableStateFlow(BatchTaskProgress())
    private val feedbackBanner = MutableStateFlow<String?>(null)
    private val recentlyStampedMonthKey = MutableStateFlow<String?>(null)
    private var gpsBackfillJob: Job? = null
    private var pendingBatchWriteTask: PendingBatchWriteTask? = null

    private sealed interface PendingBatchWriteTask {
        data class Compress(
            val photos: List<PhotoItemEntity>,
            val preset: CompressionPreset
        ) : PendingBatchWriteTask

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
        val access: MediaAccessLevel,
        val hasLocationPerm: Boolean,
        val scanning: Boolean,
        val gpsProgress: GpsIndexProgress,
        val batchProgress: BatchTaskProgress,
        val banner: String?,
        val recentStamp: String?
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
        val pendingTrashBytes: Long
    )

    private val metaFlow = combine(
        undoStack,
        combine(mediaAccessLevel, hasMediaLocationPermission) { a, loc -> a to loc },
        combine(isScanningDevice, gpsIndexProgress, batchTaskProgress) { scan, gps, batch ->
            Triple(scan, gps, batch)
        },
        combine(feedbackBanner, recentlyStampedMonthKey) { b, s -> b to s }
    ) { stack, accessPair, workTriple, bannerPair ->
        UiMeta(
            undoDepth = stack.size,
            access = accessPair.first,
            hasLocationPerm = accessPair.second,
            scanning = workTriple.first,
            gpsProgress = workTriple.second,
            batchProgress = workTriple.third,
            banner = bannerPair.first,
            recentStamp = bannerPair.second
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
        repository.monthStamps,
        repository.cleanupStats,
        mediaFilter
    ) { activePhotos, trashed, stamps, stats, filter ->
        var totalImageCount = 0
        var totalVideoCount = 0
        var hasDeviceMedia = false
        for (p in activePhotos) {
            if (p.isVideo) totalVideoCount++ else totalImageCount++
            if (!p.isSample) hasDeviceMedia = true
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

        val yearbooks = buildYearbooksLinear(filtered, stampMap)

        val visibleGallery = ArrayList<PhotoItemEntity>(filtered.size)
        for (p in filtered) {
            if (p.photoStatus != PhotoStatus.TRASHED) {
                visibleGallery.add(p)
            }
        }

        val galleryEntries = buildGalleryEntriesLinear(visibleGallery)
        val (clusters, unlocated) = buildLocationClustersAndUnlocatedLinear(visibleGallery)

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
            pendingTrashBytes = data.pendingTrashBytes,
            undoStackDepth = meta.undoDepth,
            feedbackBanner = meta.banner,
            recentlyStampedMonthKey = meta.recentStamp,
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
            repository.loadDemoArchive()
            showBanner("已加载示例照片与 4K 视频影集")
        }
    }

    fun setMediaFilter(filter: MediaFilter) {
        mediaFilter.value = filter
    }

    fun getMonthPagesForYear(year: Int, state: FlipYearUiState = uiState.value): List<MonthPageModel> {
        val byMonth = HashMap<Int, ArrayList<PhotoItemEntity>>(16)
        for (photo in state.allPhotos) {
            if (photo.year == year) {
                byMonth.getOrPut(photo.month) { ArrayList() }.add(photo)
            }
        }
        val monthsPresent = byMonth.keys.sortedDescending()
        return monthsPresent.map { month ->
            val monthPhotos = byMonth[month].orEmpty()
            val photosByDay = HashMap<Int, ArrayList<PhotoItemEntity>>(32)
            var videoCount = 0
            var unreviewedCount = 0
            var keptCount = 0
            var trashedCount = 0
            var reclaimableBytes = 0L
            val burstIds = HashSet<String>()
            var favoriteCover: PhotoItemEntity? = null
            var bestQualityCover: PhotoItemEntity? = null

            for (p in monthPhotos) {
                if (p.isVideo) videoCount++
                val st = p.photoStatus
                when (st) {
                    PhotoStatus.UNREVIEWED -> {
                        unreviewedCount++
                        reclaimableBytes += p.fileSizeBytes
                        p.burstGroupId?.let { burstIds.add(it) }
                    }
                    PhotoStatus.KEPT -> keptCount++
                    PhotoStatus.TRASHED -> trashedCount++
                    PhotoStatus.PURGED -> Unit
                }
                if (st != PhotoStatus.TRASHED) {
                    photosByDay.getOrPut(p.day) { ArrayList() }.add(p)
                    if (favoriteCover == null && p.isFavorite) {
                        favoriteCover = p
                    }
                    if (bestQualityCover == null || p.qualityScore > bestQualityCover.qualityScore) {
                        bestQualityCover = p
                    }
                }
            }

            val cleanedDays = HashSet<Int>(photosByDay.size)
            for ((day, dayList) in photosByDay) {
                if (dayList.isNotEmpty() && dayList.none { it.photoStatus == PhotoStatus.UNREVIEWED }) {
                    cleanedDays.add(day)
                }
            }

            val key = PhotoRepository.yearMonthKey(year, month)
            val cover = favoriteCover ?: bestQualityCover ?: monthPhotos.firstOrNull()

            MonthPageModel(
                year = year,
                month = month,
                photos = monthPhotos,
                photosByDay = photosByDay,
                cleanedDays = cleanedDays,
                totalPhotos = monthPhotos.size,
                videoCount = videoCount,
                unreviewedCount = unreviewedCount,
                keptCount = keptCount,
                trashedCount = trashedCount,
                burstGroupCount = burstIds.size,
                reclaimableBytes = reclaimableBytes,
                stamp = state.monthStamps[key],
                coverPhoto = cover
            )
        }
    }

    fun buildArcQueue(
        year: Int?,
        month: Int?,
        locationName: String?,
        includeReviewed: Boolean = false,
        state: FlipYearUiState = uiState.value
    ): List<CleanupCardItem> {
        val candidates = ArrayList<PhotoItemEntity>()
        for (photo in state.allPhotos) {
            if (year != null && photo.year != year) continue
            if (month != null && photo.month != month) continue
            val matchLocation = when (locationName) {
                null -> true
                UNLOCATED_LOCATION_KEY -> !hasValidLocation(photo)
                else -> photo.locationName == locationName
            }
            if (!matchLocation) continue
            val st = photo.photoStatus
            val matchStatus = if (includeReviewed) st != PhotoStatus.TRASHED else st == PhotoStatus.UNREVIEWED
            if (matchStatus) candidates.add(photo)
        }
        candidates.sortBy { it.takenAtMillis }

        // O(N) pre-grouping of burst members (eliminates O(N^2) inner filter)
        val burstGroups = HashMap<String, ArrayList<PhotoItemEntity>>()
        for (photo in candidates) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                burstGroups.getOrPut(gid) { ArrayList(4) }.add(photo)
            }
        }

        val result = ArrayList<CleanupCardItem>(candidates.size)
        val consumedGroupIds = HashSet<String>(burstGroups.size * 2)

        for (photo in candidates) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                if (consumedGroupIds.add(gid)) {
                    val stackPhotos = burstGroups[gid].orEmpty()
                    if (stackPhotos.size >= 2) {
                        var best = stackPhotos.first()
                        var foundExplicitBest = false
                        for (m in stackPhotos) {
                            if (m.isBestInStack) {
                                best = m
                                foundExplicitBest = true
                                break
                            }
                            if (m.qualityScore > best.qualityScore) {
                                best = m
                            }
                        }
                        if (!foundExplicitBest) {
                            // already holds max qualityScore
                        }
                        result.add(
                            CleanupCardItem.BurstStack(
                                groupId = gid,
                                photos = stackPhotos,
                                bestPhoto = best
                            )
                        )
                    } else {
                        result.add(CleanupCardItem.SinglePhoto(photo))
                    }
                }
            } else {
                result.add(CleanupCardItem.SinglePhoto(photo))
            }
        }
        return result
    }

    fun swipeTrashPhoto(photo: PhotoItemEntity) {
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = if (photo.isVideo) "已撤回视频移入回收站" else "已撤回照片移入回收站"
                )
            )
            val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.TRASHED, photo.year, photo.month)
            if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
        }
    }

    fun keepPhoto(photo: PhotoItemEntity, markFavorite: Boolean = false) {
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已撤回保留操作"
                )
            )
            if (markFavorite && !photo.isFavorite) {
                repository.toggleFavorite(photo.id, true)
            }
            val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.KEPT, photo.year, photo.month)
            if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
        }
    }

    fun keepBestAndTrashStackOthers(stack: CleanupCardItem.BurstStack, chosenBest: PhotoItemEntity = stack.bestPhoto) {
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已还原 ${stack.photos.size} 张连拍照片"
                )
            )
            repository.setBestInStack(stack.groupId, chosenBest.id)
            val others = stack.photos.filter { it.id != chosenBest.id }
            val newlyStamped = repository.keepBestAndTrashOthers(chosenBest, others)
            showBanner("已保留最佳照片，将 ${others.size} 张相似连拍移入回收站")
            if (newlyStamped) triggerMonthStamped(chosenBest.year, chosenBest.month)
        }
    }

    fun selectBestInStack(groupId: String, photoId: Long) {
        viewModelScope.launch {
            repository.setBestInStack(groupId, photoId)
        }
    }

    /**
     * Strictly O(N) linear gallery builder for 10,000+ items.
     * Pre-groups burst clusters in a single HashMap pass instead of scanning all items per burst group.
     */
    fun buildGalleryEntries(photos: List<PhotoItemEntity>): List<GalleryEntry> {
        return buildGalleryEntriesLinear(photos.sortedByDescending { it.takenAtMillis })
    }

    private fun buildGalleryEntriesLinear(alreadySortedDesc: List<PhotoItemEntity>): List<GalleryEntry> {
        if (alreadySortedDesc.isEmpty()) return emptyList()

        val burstMembers = HashMap<String, ArrayList<PhotoItemEntity>>()
        for (photo in alreadySortedDesc) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                burstMembers.getOrPut(gid) { ArrayList(4) }.add(photo)
            }
        }

        val entries = ArrayList<GalleryEntry>(alreadySortedDesc.size)
        val consumedGroups = HashSet<String>(burstMembers.size * 2)

        for (photo in alreadySortedDesc) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                if (consumedGroups.add(gid)) {
                    val rawMembers = burstMembers[gid]
                    if (rawMembers != null && rawMembers.size >= 2) {
                        // Already in DESC order; reverse to ASC chronological order in O(k) without sorting
                        val members = if (rawMembers.size <= 2) {
                            if (rawMembers[0].takenAtMillis <= rawMembers[1].takenAtMillis) rawMembers else rawMembers.asReversed()
                        } else {
                            rawMembers.sortedBy { it.takenAtMillis }
                        }
                        var best = members.first()
                        for (m in members) {
                            if (m.isBestInStack) {
                                best = m
                                break
                            }
                            if (m.qualityScore > best.qualityScore) {
                                best = m
                            }
                        }
                        entries.add(
                            GalleryEntry.Burst(
                                groupId = gid,
                                photos = members,
                                bestPhoto = best
                            )
                        )
                    } else {
                        entries.add(GalleryEntry.Single(photo))
                    }
                }
            } else {
                entries.add(GalleryEntry.Single(photo))
            }
        }
        return entries
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

    fun trashEntireStack(stack: CleanupCardItem.BurstStack) {
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已还原整组连拍 (${stack.photos.size} 张)"
                )
            )
            val months = stack.photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = stack.photos.map { it.id },
                status = PhotoStatus.TRASHED,
                affectedMonths = months
            )
            if (newlyStamped) triggerMonthStamped(stack.bestPhoto.year, stack.bestPhoto.month)
        }
    }

    fun bulkTrashPhotos(photos: List<PhotoItemEntity>) {
        if (photos.isEmpty()) return
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已撤回批量删除 (${photos.size} 项)"
                )
            )
            val months = photos.map { it.year to it.month }.toSet()
            val newlyStamped = repository.markPhotosStatus(
                ids = photos.map { it.id },
                status = PhotoStatus.TRASHED,
                affectedMonths = months
            )
            val mb = photos.sumOf { it.fileSizeBytes } / (1024.0 * 1024.0)
            showBanner(String.format("已将 %d 项移入回收站 (%.1f MB)", photos.size, mb))
            if (newlyStamped) {
                val first = photos.first()
                triggerMonthStamped(first.year, first.month)
            }
        }
    }

    fun bulkKeepPhotos(photos: List<PhotoItemEntity>, markFavorite: Boolean = false) {
        if (photos.isEmpty()) return
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = if (markFavorite) "已撤回批量收藏 (${photos.size} 项)" else "已撤回批量保留 (${photos.size} 项)"
                )
            )
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
            showBanner(if (markFavorite) "已收藏并保留 ${photos.size} 项媒体" else "已批量保留 ${photos.size} 项媒体")
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
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已撤回移动 (${photos.size} 项)"
                )
            )
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
            showBanner("已将 ${photos.size} 项移动至 $destDesc")
        }
    }

    fun requestBatchCompress(
        photos: List<PhotoItemEntity>,
        preset: CompressionPreset
    ): PendingIntent? {
        val imageTargets = photos.filter { !it.isVideo }
        if (imageTargets.isEmpty()) {
            showBanner("所选项目中暂无可压缩的照片")
            return null
        }
        val writeIntent = repository.buildSystemWriteIntent(imageTargets)
        return if (writeIntent != null) {
            pendingBatchWriteTask = PendingBatchWriteTask.Compress(
                photos = photos,
                preset = preset
            )
            writeIntent
        } else {
            executeBatchCompress(photos = photos, preset = preset)
            null
        }
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
            is PendingBatchWriteTask.Compress -> {
                if (granted) {
                    executeBatchCompress(task.photos, task.preset)
                } else {
                    showBanner("已取消批量近无损压缩")
                }
            }
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
            showBanner("已有批量处理任务正在进行，请稍候")
            return
        }
        viewModelScope.launch {
            val imageCount = photos.count { !it.isVideo }
            batchTaskProgress.value = BatchTaskProgress(
                message = "正在近无损压缩 (0/$imageCount)",
                processed = 0,
                total = imageCount
            )
            try {
                val result = repository.compressPhotosNearLossless(
                    photos = photos,
                    preset = preset
                ) { processed, total, _ ->
                    batchTaskProgress.value = BatchTaskProgress(
                        message = "正在近无损压缩 ($processed/$total)",
                        processed = processed,
                        total = total
                    )
                }
                val compressedCount = result.compressedUpdates.size
                val savedMb = result.totalSavedBytes / (1024.0 * 1024.0)
                val summary = when {
                    compressedCount > 0 -> buildString {
                        append(String.format("已完成 %d 张近无损压缩，节省 %.1f MB", compressedCount, savedMb))
                        if (result.skippedAlreadyOptimalCount > 0) {
                            append("（${result.skippedAlreadyOptimalCount} 张已是最优跳过）")
                        }
                    }
                    result.skippedAlreadyOptimalCount > 0 ->
                        "所选 ${result.skippedAlreadyOptimalCount} 张照片体积已最优，无需重复压缩"
                    else ->
                        "未能压缩所选照片，请确认文件可写权限"
                }
                showBanner(summary)
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
            showBanner("已有批量处理任务正在进行，请稍候")
            return
        }
        viewModelScope.launch {
            pushUndo(
                UndoSnapshot(
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
                    description = "已撤回批量定位更新 (${updates.size} 项)"
                )
            )

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
                showBanner(msg)
            } finally {
                batchTaskProgress.value = BatchTaskProgress()
            }
        }
    }

    fun undoLastAction(triggeredByShake: Boolean = false): Boolean {
        val current = undoStack.value
        if (current.isEmpty()) return false
        val snapshot = current.last()
        undoStack.value = current.dropLast(1)

        viewModelScope.launch {
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
            showBanner(msg)
        }
        return true
    }

    fun restoreFromVault(photo: PhotoItemEntity) {
        viewModelScope.launch {
            repository.restorePhoto(photo)
            showBanner("已放回 ${photo.year}年${photo.month}月 影集")
        }
    }

    fun restoreAllFromVault() {
        val trashed = uiState.value.trashedPhotos
        if (trashed.isEmpty()) return
        viewModelScope.launch {
            repository.restoreAllTrashed(trashed)
            showBanner("已还原全部 ${trashed.size} 项媒体")
        }
    }

    fun buildSystemTrashIntentForVault(): PendingIntent? {
        return repository.buildSystemTrashIntent(uiState.value.trashedPhotos)
    }

    fun finalizePurgeTrashed() {
        val trashed = uiState.value.trashedPhotos
        if (trashed.isEmpty()) return
        viewModelScope.launch {
            val freedBytes = repository.purgeTrashedPhotos(trashed)
            undoStack.value = emptyList()
            val mb = freedBytes / (1024.0 * 1024.0)
            showBanner(String.format("成功释放 %.1f MB 空间", mb))
        }
    }

    fun dismissBanner() {
        feedbackBanner.value = null
    }

    private fun pushUndo(snapshot: UndoSnapshot) {
        undoStack.value = (undoStack.value + snapshot).takeLast(25)
    }

    private fun triggerMonthStamped(year: Int, month: Int) {
        recentlyStampedMonthKey.value = PhotoRepository.yearMonthKey(year, month)
    }

    private fun showBanner(message: String) {
        feedbackBanner.value = message
        viewModelScope.launch {
            delay(2800)
            if (feedbackBanner.value == message) {
                feedbackBanner.value = null
            }
        }
    }

    /**
     * Single-pass O(N) Yearbook aggregator using fixed IntArray(13) month accumulators per year
     * (replaces nested month filters across 10,000+ photos).
     */
    private class YearAcc(val year: Int) {
        var total = 0
        var videoCount = 0
        var unreviewedCount = 0
        var keptCount = 0
        var trashedCount = 0
        var reclaimableBytes = 0L
        var favoriteCover: PhotoItemEntity? = null
        var firstVisibleImage: PhotoItemEntity? = null
        var firstVisibleAny: PhotoItemEntity? = null
        var firstAny: PhotoItemEntity? = null
        val monthPresent = BooleanArray(13)
        val monthUnreviewed = IntArray(13)
    }

    private fun buildYearbooksLinear(
        photos: List<PhotoItemEntity>,
        stamps: Map<String, MonthStampEntity>
    ): List<YearbookSummary> {
        if (photos.isEmpty()) return emptyList()
        val byYear = HashMap<Int, YearAcc>(16)

        for (p in photos) {
            val acc = byYear.getOrPut(p.year) { YearAcc(p.year) }
            acc.total++
            if (acc.firstAny == null) acc.firstAny = p
            if (p.isVideo) acc.videoCount++

            val m = p.month
            if (m in 1..12) acc.monthPresent[m] = true

            val st = p.photoStatus
            when (st) {
                PhotoStatus.UNREVIEWED -> {
                    acc.unreviewedCount++
                    acc.reclaimableBytes += p.fileSizeBytes
                    if (m in 1..12) acc.monthUnreviewed[m]++
                }
                PhotoStatus.KEPT -> acc.keptCount++
                PhotoStatus.TRASHED -> acc.trashedCount++
                PhotoStatus.PURGED -> Unit
            }

            if (st != PhotoStatus.TRASHED) {
                if (acc.firstVisibleAny == null) acc.firstVisibleAny = p
                if (!p.isVideo && acc.firstVisibleImage == null) acc.firstVisibleImage = p
                if (p.isFavorite && acc.favoriteCover == null) acc.favoriteCover = p
            }
        }

        val sortedYears = byYear.keys.sortedDescending()
        return sortedYears.mapIndexed { idx, year ->
            val acc = byYear[year]!!
            var totalMonths = 0
            var cleanedMonths = 0
            for (m in 1..12) {
                if (acc.monthPresent[m]) {
                    totalMonths++
                    val key = PhotoRepository.yearMonthKey(year, m)
                    if (acc.monthUnreviewed[m] == 0 || stamps.containsKey(key)) {
                        cleanedMonths++
                    }
                }
            }
            val cover = acc.favoriteCover
                ?: acc.firstVisibleImage
                ?: acc.firstVisibleAny
                ?: acc.firstAny

            YearbookSummary(
                year = year,
                coverPhoto = cover,
                totalPhotos = acc.total,
                videoCount = acc.videoCount,
                unreviewedCount = acc.unreviewedCount,
                keptCount = acc.keptCount,
                trashedCount = acc.trashedCount,
                cleanedMonthsCount = cleanedMonths,
                totalMonthsCount = totalMonths,
                reclaimableBytes = acc.reclaimableBytes,
                editionIndex = idx % 4
            )
        }
    }

    private fun hasValidLocation(photo: PhotoItemEntity): Boolean {
        val lat = photo.latitude
        val lon = photo.longitude
        return lat != null && lon != null &&
            lat.isFinite() && lon.isFinite() &&
            lat in -85.05..85.05 && lon in -180.0..180.0 &&
            !photo.locationName.isNullOrBlank()
    }

    private class ClusterAcc(val name: String, firstPhoto: PhotoItemEntity) {
        val photos = ArrayList<PhotoItemEntity>(16)
        var latSum = 0.0
        var lonSum = 0.0
        var coordCount = 0
        var unreviewedCount = 0
        var totalBytes = 0L
        var favoriteCover: PhotoItemEntity? = null
        var bestQualityCover: PhotoItemEntity = firstPhoto
    }

    /**
     * Single-pass O(N) cluster + unlocated split with primitive accumulators (zero intermediate list allocations).
     */
    private fun buildLocationClustersAndUnlocatedLinear(
        photos: List<PhotoItemEntity>
    ): Pair<List<LocationCluster>, List<PhotoItemEntity>> {
        if (photos.isEmpty()) return emptyList<LocationCluster>() to emptyList()

        val clusterMap = HashMap<String, ClusterAcc>(64)
        val unlocated = ArrayList<PhotoItemEntity>()

        for (p in photos) {
            if (!hasValidLocation(p)) {
                unlocated.add(p)
                continue
            }
            val name = p.locationName!!
            val acc = clusterMap.getOrPut(name) { ClusterAcc(name, p) }
            acc.photos.add(p)
            acc.latSum += p.latitude!!
            acc.lonSum += p.longitude!!
            acc.coordCount++
            if (p.photoStatus == PhotoStatus.UNREVIEWED) acc.unreviewedCount++
            acc.totalBytes += p.fileSizeBytes
            if (p.isFavorite && acc.favoriteCover == null) acc.favoriteCover = p
            if (p.qualityScore > acc.bestQualityCover.qualityScore) acc.bestQualityCover = p
        }

        val clusters = ArrayList<LocationCluster>(clusterMap.size)
        for (acc in clusterMap.values) {
            if (acc.coordCount <= 0) continue
            val avgLat = (acc.latSum / acc.coordCount).coerceIn(-85.0, 85.0)
            val avgLon = (acc.lonSum / acc.coordCount).coerceIn(-180.0, 180.0)
            clusters.add(
                LocationCluster(
                    locationName = acc.name,
                    latitude = avgLat,
                    longitude = avgLon,
                    photos = acc.photos,
                    coverPhoto = acc.favoriteCover ?: acc.bestQualityCover,
                    unreviewedCount = acc.unreviewedCount,
                    totalBytes = acc.totalBytes
                )
            )
        }
        clusters.sortWith(compareByDescending<LocationCluster> { it.unreviewedCount }.thenByDescending { it.photos.size })
        return clusters to unlocated
    }
}
