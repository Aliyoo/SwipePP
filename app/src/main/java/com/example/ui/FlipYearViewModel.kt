package com.example.ui

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.os.Build
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
import com.example.data.SystemFavoriteWriteStatus
import com.example.data.CollageBackdropStyle
import com.example.data.CollageExportResult
import com.example.data.CollageSlotTransform
import com.example.data.YearbookCollageExporter
import com.example.data.YearbookCoverConfigEntity
import com.example.data.YearbookCoverMode
import com.example.data.isSafelyCompressibleImage
import com.example.data.smarttag.SmartTagAnalyzer
import com.example.data.smarttag.SmartTaggingPipeline
import com.example.ui.components.resolveSampleDrawable
import com.example.ui.components.CollageTemplate
import com.example.ui.components.buildTimelineClusterLayout
import com.example.ui.domain.LibraryDerivationEngine
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.YearbookCoverStyle
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

// Zero-centered semantic lift constants calibrated on Chinese-CLIP RN50 + CLC2 neutral baseline.
// Each photo's Hubness baseline `dot(imgVec, neutralMean)` is subtracted from `dot(imgVec, queryVec)`,
// eliminating document/screenshot/blank-frame bias so true matches sit at +0.026..+0.110 while
// absent-concept queries sit below +0.015.
internal const val SEMANTIC_LIFT_FLOOR = 0.025f
internal const val SEMANTIC_TOP1_MARGIN = 0.036f
internal const val SEMANTIC_MEDIAN_MARGIN = 0.022f
internal const val SEMANTIC_STRONG_LIFT = 0.042f
internal const val SEMANTIC_RAW_FLOOR = 0.435f
internal const val SEMANTIC_MATCH_CAP = 200

internal data class ScoredSemanticCandidate(
    val photoId: Long,
    val lift: Float,
    val rawCosine: Float
)

/**
 * Pure threshold & ranking filter over debiased semantic candidates. Returns a map of
 * `photoId -> lift` ordered by lift descending, or emptyMap when the query is out-of-library noise.
 */
internal fun selectSemanticMatchesByLift(
    candidates: List<ScoredSemanticCandidate>
): Map<Long, Float> {
    if (candidates.isEmpty()) return emptyMap()
    val sorted = candidates.sortedByDescending { it.lift }
    val top = sorted.first()
    if (top.lift < SEMANTIC_LIFT_FLOOR || top.rawCosine < SEMANTIC_RAW_FLOOR) return emptyMap()

    if (sorted.size >= 6) {
        val mid = sorted.size / 2
        val medianLift = if (sorted.size % 2 == 0) {
            (sorted[mid - 1].lift + sorted[mid].lift) * 0.5f
        } else {
            sorted[mid].lift
        }
        if ((top.lift - medianLift) < SEMANTIC_MEDIAN_MARGIN && top.lift < SEMANTIC_STRONG_LIFT) {
            return emptyMap()
        }
    }

    val cut = (top.lift - SEMANTIC_TOP1_MARGIN).coerceAtLeast(SEMANTIC_LIFT_FLOOR)
    val out = LinkedHashMap<Long, Float>(minOf(sorted.size, SEMANTIC_MATCH_CAP))
    for (item in sorted) {
        if (out.size >= SEMANTIC_MATCH_CAP) break
        if (item.lift < cut) break
        if (item.rawCosine < SEMANTIC_RAW_FLOOR) continue
        out[item.photoId] = item.lift
    }
    return out
}

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
    private val timelineSortOrder = MutableStateFlow(
        ThemePreferences(application).userPreferences.value.timelineSortOrder
    )
    private val defaultYearbookCoverStyle = MutableStateFlow(
        ThemePreferences(application).userPreferences.value.yearbookCoverStyle
    )
    private val mediaAccessLevel = MutableStateFlow(repository.checkMediaAccess())
    private val hasMediaLocationPermission = MutableStateFlow(repository.hasMediaLocationAccess())
    private val isScanningDevice = MutableStateFlow(false)
    private val gpsIndexProgress = MutableStateFlow(GpsIndexProgress())
    private val smartTagProgress = MutableStateFlow(SmartTagIndexProgress())
    private val batchTaskProgress = MutableStateFlow(BatchTaskProgress())
    private val snackbarMessage = MutableStateFlow<SnackbarMessage?>(null)
    private val recentlyStampedMonthKey = MutableStateFlow<String?>(null)
    private val vaultSessionFilterIds = MutableStateFlow<Set<Long>?>(null)
    private val customCleanerSession = MutableStateFlow<CustomCleanerSession?>(null)
    private var gpsBackfillJob: Job? = null
    private var smartTagJob: Job? = null
    @Volatile private var smartTagRerunRequested = false
    private val smartTagAnalyzerLazy = lazy { SmartTagAnalyzer(application) }
    private val smartTagAnalyzer by smartTagAnalyzerLazy
    private val smartTagPipeline by lazy {
        SmartTaggingPipeline(
            dao = database.flipYearDao(),
            analyzer = smartTagAnalyzer,
            drawableResolver = { name -> resolveSampleDrawable(name).takeIf { it != 0 } }
        )
    }
    private var smartTagIndexedOnce = false

    private data class CachedEmbeddingRow(
        val photoId: Long,
        val embeddingFp16: ByteArray,
        val imageBaseline: Float
    )

    // Semantic-search caches. Invalidated automatically by Room's InvalidationTracker whenever
    // photos or photo_embeddings change.
    private var smartTagWarmupDone = false
    @Volatile private var semanticEmbeddingCache: List<CachedEmbeddingRow>? = null
    private var lastSemanticQuery: Pair<String, FloatArray>? = null

    private fun invalidateSemanticEmbeddingCache() {
        semanticEmbeddingCache = null
    }

    private var snackbarDismissJob: Job? = null
    private val systemSyncPrefs = application.getSharedPreferences("flipyear_system_sync", Context.MODE_PRIVATE)
    private val pendingFavoriteConsentFlow = MutableStateFlow<PendingFavoriteConsent?>(null)
    val pendingFavoriteConsent: StateFlow<PendingFavoriteConsent?> = pendingFavoriteConsentFlow
    private var favoriteMigrationPromptedThisSession = false

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

    private data class FavoriteConsentUnit(
        val photos: List<PhotoItemEntity>,
        val favorite: Boolean
    )

    private data class GpsIndexProgress(
        val isIndexing: Boolean = false,
        val processed: Int = 0,
        val total: Int = 0
    )

    private data class SmartTagIndexProgress(
        val isIndexing: Boolean = false,
        val processed: Int = 0,
        val total: Int = 0,
        val revision: Int = 0
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
        val smartTagProgress: SmartTagIndexProgress,
        val batchProgress: BatchTaskProgress,
        val snackbar: SnackbarMessage?,
        val recentStamp: String?,
        val vaultSessionFilter: Set<Long>?,
        val customSession: CustomCleanerSession?
    )

    private data class DerivedLibraryData(
        val filter: MediaFilter,
        val sortOrder: TimelineSortOrder,
        val allPhotos: List<PhotoItemEntity>,
        val galleryEntries: List<GalleryEntry>,
        val galleryIndex: PrecomputedGalleryIndex,
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
        combine(isScanningDevice, gpsIndexProgress, smartTagProgress, batchTaskProgress) { scan, gps, smartTag, batch ->
            listOf(scan, gps, smartTag, batch)
        },
        combine(snackbarMessage, recentlyStampedMonthKey, vaultSessionFilterIds, customCleanerSession) { snack, stamp, filterIds, custom ->
            listOf(snack, stamp, filterIds, custom)
        }
    ) { undoPair, accessPair, workList, feedbackList ->
        @Suppress("UNCHECKED_CAST")
        UiMeta(
            undoDepth = undoPair.first,
            cleanerSessionUndoDepth = undoPair.second,
            access = accessPair.first,
            hasLocationPerm = accessPair.second,
            scanning = workList[0] as Boolean,
            gpsProgress = workList[1] as GpsIndexProgress,
            smartTagProgress = workList[2] as SmartTagIndexProgress,
            batchProgress = workList[3] as BatchTaskProgress,
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
        combine(mediaFilter, timelineSortOrder, defaultYearbookCoverStyle) { filter, order, coverStyle ->
            Triple(filter, order, coverStyle)
        }
    ) { activePhotos, trashed, stampsAndCovers, stats, filterOrderStyle ->
        val (stamps, coverConfigs) = stampsAndCovers
        val (filter, sortOrder, coverStyle) = filterOrderStyle
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

        val yearbooks = LibraryDerivationEngine.buildYearbooksLinear(
            photos = filtered,
            stamps = stampMap,
            coverConfigs = coverConfigMap,
            sortOrder = sortOrder,
            defaultCoverStyle = coverStyle
        )

        val visibleGallery = ArrayList<PhotoItemEntity>(filtered.size)
        for (p in filtered) {
            if (p.photoStatus != PhotoStatus.TRASHED) {
                visibleGallery.add(p)
            }
        }

        // Room already emits allActivePhotos ordered by takenAtMillis DESC;
        // reversing in O(1) view for OLDEST_FIRST avoids an extra O(N log N) sort.
        val orderedGallerySource = if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
            visibleGallery
        } else {
            visibleGallery.asReversed()
        }
        val galleryEntries = LibraryDerivationEngine.buildGalleryEntriesLinear(orderedGallerySource)
        val galleryIndex = buildPrecomputedGalleryIndex(galleryEntries, stampMap)
        val (clusters, unlocated) = LibraryDerivationEngine.buildLocationClustersAndUnlocatedLinear(visibleGallery)

        var pendingBytes = 0L
        for (t in trashed) {
            pendingBytes += t.fileSizeBytes
        }

        DerivedLibraryData(
            filter = filter,
            sortOrder = sortOrder,
            allPhotos = filtered,
            galleryEntries = galleryEntries,
            galleryIndex = galleryIndex,
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
            timelineSortOrder = data.sortOrder,
            allPhotos = data.allPhotos,
            galleryEntries = data.galleryEntries,
            galleryIndex = data.galleryIndex,
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
            batchTaskTotal = meta.batchProgress.total,
            isIndexingSmartTags = meta.smartTagProgress.isIndexing,
            smartTagIndexedCount = meta.smartTagProgress.processed,
            smartTagTotalCount = meta.smartTagProgress.total,
            smartTagRevision = meta.smartTagProgress.revision
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlipYearUiState(
            isLoading = true,
            timelineSortOrder = timelineSortOrder.value
        )
    )

    // Registered asynchronously off the UI thread (observer setup can touch the writable
    // database) and unregistered in onCleared so a dead ViewModel is not retained.
    private val semanticInvalidationObserver =
        object : androidx.room.InvalidationTracker.Observer(arrayOf("photos", "photo_embeddings")) {
            override fun onInvalidated(tables: Set<String>) {
                invalidateSemanticEmbeddingCache()
            }
        }

    init {
        // Any write to photos/photo_embeddings (scan, trash, undo, demo removal, purge,
        // backfill upserts — including paths added in the future) invalidates the
        // semantic embedding cache, instead of hooking every mutation site by hand.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.invalidationTracker.addObserver(semanticInvalidationObserver)
        }
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
        // Index whatever rows already exist (cached device media or a previously loaded demo
        // archive) even when media permission is missing; the pipeline is idempotent and cheap
        // when there is nothing pending.
        startSmartTagBackfill()
        // Same for the media probe: without it, a permission-less upgrade leaves rows
        // unprobed forever and probe-gated features (compression, EXIF writes) never unlock.
        startMediaProbeBackfill()
    }

    override fun onCleared() {
        // Removing the last observer makes Room drop its invalidation triggers in a
        // synchronous write transaction — off the main thread, and outside viewModelScope
        // which is already cancelled by the time onCleared runs.
        val tracker = database.invalidationTracker
        val observer = semanticInvalidationObserver
        kotlin.concurrent.thread(isDaemon = true) {
            tracker.removeObserver(observer)
        }
        if (smartTagAnalyzerLazy.isInitialized()) {
            smartTagAnalyzer.close()
        }
        super.onCleared()
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
                val systemFavoritesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                val migrationPending = systemFavoritesSupported && !isSystemFavoriteMigrationComplete()
                val result = repository.syncLocalDeviceMedia(
                    preserveLocalFavorites = migrationPending || !systemFavoritesSupported
                )
                if (migrationPending) {
                    migrateLegacyFavoritesIfNeeded(result.favoriteExportCandidates)
                }
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
                startSmartTagBackfill()
                startMediaProbeBackfill()
            } finally {
                isScanningDevice.value = false
            }
        }
    }

    private fun isSystemFavoriteMigrationComplete(): Boolean {
        return systemSyncPrefs.getBoolean(KEY_SYSTEM_FAVORITE_MIGRATION_DONE, false)
    }

    private fun markSystemFavoriteMigrationComplete() {
        systemSyncPrefs.edit().putBoolean(KEY_SYSTEM_FAVORITE_MIGRATION_DONE, true).apply()
    }

    private suspend fun migrateLegacyFavoritesIfNeeded(candidates: List<PhotoItemEntity>) {
        if (candidates.isEmpty()) {
            markSystemFavoriteMigrationComplete()
            return
        }
        if (favoriteMigrationPromptedThisSession) return
        favoriteMigrationPromptedThisSession = true
        writeFavoritesWithOptionalConsent(
            photos = candidates,
            newFavorite = true,
            onApplied = { markSystemFavoriteMigrationComplete() },
            onRejected = {
                showBanner("已有珍藏暂未同步到系统相册，下次启动会再次尝试")
            }
        )
    }

    private suspend fun writeFavoritesWithOptionalConsent(
        photos: List<PhotoItemEntity>,
        newFavorite: Boolean,
        onApplied: suspend () -> Unit,
        onRejected: () -> Unit
    ) {
        val outcome = repository.writeFavoritesToSystem(photos, newFavorite)
        when (outcome.status) {
            SystemFavoriteWriteStatus.APPLIED,
            SystemFavoriteWriteStatus.LOCAL_ONLY -> onApplied()
            SystemFavoriteWriteStatus.NEEDS_CONSENT -> {
                val units = favoriteConsentUnits(photos, newFavorite)
                if (units.isEmpty()) {
                    onApplied()
                } else {
                    queueFavoriteConsentUnits(units, onApplied, onRejected)
                }
            }
        }
    }

    private fun favoriteConsentUnits(
        photos: List<PhotoItemEntity>,
        favorite: Boolean
    ): List<FavoriteConsentUnit> {
        return photos
            .filter { !it.isSample && !it.contentUri.isNullOrBlank() && it.contentUri.startsWith("content://") }
            .chunked(FAVORITE_CONSENT_CHUNK_SIZE)
            .map { FavoriteConsentUnit(it, favorite) }
    }

    private fun queueFavoriteConsentUnits(
        units: List<FavoriteConsentUnit>,
        onApproved: suspend () -> Unit,
        onCancelled: () -> Unit
    ) {
        fun enqueue(index: Int) {
            if (index >= units.size) {
                viewModelScope.launch { onApproved() }
                return
            }
            val unit = units[index]
            val pendingIntent = repository.buildSystemFavoriteIntent(unit.photos, unit.favorite)
            if (pendingIntent == null) {
                onCancelled()
                return
            }
            pendingFavoriteConsentFlow.value = PendingFavoriteConsent(
                pendingIntent = pendingIntent,
                onApproved = { enqueue(index + 1) },
                onCancelled = onCancelled
            )
        }
        enqueue(0)
    }

    fun confirmPendingFavoriteConsent(granted: Boolean) {
        val pending = pendingFavoriteConsentFlow.value ?: return
        pendingFavoriteConsentFlow.value = null
        if (granted) {
            viewModelScope.launch { pending.onApproved() }
        } else {
            pending.onCancelled()
        }
    }

    private suspend fun runFavoriteMutation(
        photos: List<PhotoItemEntity>,
        newFavorite: Boolean,
        snapshot: UndoSnapshot,
        inCleanerSession: Boolean,
        showFeedback: Boolean,
        successText: String,
        afterFavoriteApplied: suspend () -> Unit = {}
    ) {
        val targets = photos.filter { it.isFavorite != newFavorite }
        if (targets.isEmpty()) {
            pushUndo(snapshot, inCleanerSession = inCleanerSession)
            afterFavoriteApplied()
            if (showFeedback) {
                showSnackbar(text = successText, undoSnapshot = snapshot)
            }
            return
        }
        writeFavoritesWithOptionalConsent(
            photos = targets,
            newFavorite = newFavorite,
            onApplied = {
                repository.togglePhotosFavorite(targets.map { it.id }, newFavorite)
                pushUndo(snapshot, inCleanerSession = inCleanerSession)
                afterFavoriteApplied()
                if (showFeedback) {
                    showSnackbar(text = successText, undoSnapshot = snapshot)
                }
            },
            onRejected = {
                showBanner("未获得系统授权，珍藏状态未变更")
            }
        )
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
                    showBanner("已完成全部定位解析，新增 $newlyLocated 项足迹媒体")
                } else if (notifyResult && forceRescan) {
                    showBanner("全部定位检查完成，当前足迹已是最新")
                }
            } finally {
                gpsIndexProgress.value = GpsIndexProgress(isIndexing = false, processed = 0, total = 0)
            }
        }
    }

    /**
     * Background ML content tagging (ML Kit object/scene labels + on-device MobileCLIP
     * zero-shot categories). Idempotent: only rows behind the current taxonomy version
     * are processed, so repeated calls after incremental syncs are cheap.
     */
    private var mediaProbeJob: Job? = null

    /**
     * Background media probe (embedded motion photos / animated WebP). Silent by design:
     * rows are playable progressively as batches land, newest first.
     */
    fun startMediaProbeBackfill() {
        if (mediaProbeJob?.isActive == true) return
        mediaProbeJob = viewModelScope.launch {
            try {
                repository.backfillMediaProbe()
            } catch (t: Throwable) {
                if (t !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.w("FlipYearViewModel", "media probe backfill failed", t)
                }
            }
        }
    }

    fun startSmartTagBackfill() {
        // Warm up the text tower independently of vision indexing (they hold separate mutexes),
        // so semantic search stays snappy even while a large library is still indexing.
        if (!smartTagWarmupDone) {
            smartTagWarmupDone = true
            viewModelScope.launch {
                try {
                    smartTagAnalyzer.neutralConceptBaseline()
                    smartTagAnalyzer.encodeText("照片")
                } catch (t: Throwable) {
                    if (t !is kotlinx.coroutines.CancellationException) {
                        android.util.Log.w("FlipYearViewModel", "semantic warmup failed", t)
                    }
                }
            }
        }
        if (smartTagJob?.isActive == true) {
            // The pipeline caps a run at the pending count sampled when it started; media scanned
            // mid-run would otherwise wait for the next launch. Remember the follow-up request
            // and re-run when the active job finishes.
            smartTagRerunRequested = true
            return
        }
        smartTagJob = viewModelScope.launch {
            try {
                do {
                    smartTagRerunRequested = false
                    var lastReportedProcessed = 0
                    val processed = smartTagPipeline.runPending { progress ->
                        val prev = smartTagProgress.value
                        val bumpedRev = if (progress.processed > lastReportedProcessed &&
                            (progress.processed % 12 == 0 || !progress.isRunning)
                        ) {
                            lastReportedProcessed = progress.processed
                            prev.revision + 1
                        } else {
                            prev.revision
                        }
                        smartTagProgress.value = SmartTagIndexProgress(
                            isIndexing = progress.isRunning && progress.total > 0,
                            processed = progress.processed,
                            total = progress.total,
                            revision = bumpedRev
                        )
                    }
                    if (processed > 0 && !smartTagIndexedOnce) {
                        showBanner("智能标签索引完成，可在筛选中按内容标签查找")
                    }
                    if (processed > 0) smartTagIndexedOnce = true
                } while (smartTagRerunRequested)
            } catch (t: Throwable) {
                if (t !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.w("FlipYearViewModel", "smart tag backfill failed", t)
                }
            } finally {
                val prev = smartTagProgress.value
                smartTagProgress.value = prev.copy(isIndexing = false)
            }
        }
    }

    /**
     * Encodes [keyword] with the cn-clip text tower and computes Hubness-debiased zero-centered
     * lift (`dot(imgVec, queryVec) - dot(imgVec, neutralMean)`) against every visible stored
     * embedding. Returns `photoId -> lift` ordered by relevance descending.
     */
    suspend fun computeSemanticMatches(keyword: String): Map<Long, Float> {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return emptyMap()
        return try {
            val cached = lastSemanticQuery
            val queryVec = if (cached != null && cached.first == trimmed) {
                cached.second
            } else {
                (smartTagAnalyzer.encodeText(trimmed) ?: return emptyMap()).also {
                    lastSemanticQuery = trimmed to it
                }
            }
            val expectedBytes = queryVec.size * 2
            val rows = semanticEmbeddingCache ?: loadDebiasedEmbeddingCache(expectedBytes).also {
                semanticEmbeddingCache = it
            }
            if (rows.isEmpty()) return emptyMap()

            val candidates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val out = ArrayList<ScoredSemanticCandidate>(rows.size)
                val v = FloatArray(com.example.data.smarttag.ClipEmbedder.EMBEDDING_DIM)
                for (row in rows) {
                    if (row.embeddingFp16.size != expectedBytes) continue
                    com.example.data.smarttag.decodeEmbeddingFp16Into(row.embeddingFp16, v)
                    var dot = 0f
                    var normSq = 0f
                    for (i in v.indices) {
                        dot += queryVec[i] * v[i]
                        normSq += v[i] * v[i]
                    }
                    val rawCosine = if (normSq in 0.99f..1.01f) {
                        dot
                    } else if (normSq > 0f) {
                        dot / kotlin.math.sqrt(normSq)
                    } else {
                        continue
                    }
                    val lift = rawCosine - row.imageBaseline
                    out.add(ScoredSemanticCandidate(row.photoId, lift = lift, rawCosine = rawCosine))
                }
                out
            }
            selectSemanticMatchesByLift(candidates)
        } catch (t: Throwable) {
            if (t !is kotlinx.coroutines.CancellationException) {
                android.util.Log.w("FlipYearViewModel", "semantic search failed", t)
            }
            emptyMap()
        }
    }

    private suspend fun loadDebiasedEmbeddingCache(expectedBytes: Int): List<CachedEmbeddingRow> {
        val rawRows = database.flipYearDao().getAllEmbeddings()
        if (rawRows.isEmpty()) return emptyList()
        val neutralBaseline = smartTagAnalyzer.neutralConceptBaseline()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val out = ArrayList<CachedEmbeddingRow>(rawRows.size)
            val v = FloatArray(com.example.data.smarttag.ClipEmbedder.EMBEDDING_DIM)
            for (row in rawRows) {
                if (row.embeddingFp16.size != expectedBytes) continue
                val base = if (neutralBaseline != null && neutralBaseline.size == v.size) {
                    com.example.data.smarttag.decodeEmbeddingFp16Into(row.embeddingFp16, v)
                    var dot = 0f
                    var normSq = 0f
                    for (i in v.indices) {
                        dot += v[i] * neutralBaseline[i]
                        normSq += v[i] * v[i]
                    }
                    if (normSq in 0.99f..1.01f) {
                        dot
                    } else if (normSq > 0f) {
                        dot / kotlin.math.sqrt(normSq)
                    } else {
                        0f
                    }
                } else {
                    0.43f
                }
                out.add(
                    CachedEmbeddingRow(
                        photoId = row.photoId,
                        embeddingFp16 = row.embeddingFp16,
                        imageBaseline = base
                    )
                )
            }
            out
        }
    }

    fun loadDemoArchive() {
        viewModelScope.launch {
            undoStack.value = emptyList()
            cleanerSessionUndoStack.value = emptyList()
            repository.loadDemoArchive()
            showSnackbar("已加载示例照片与 4K 视频影集")
            startSmartTagBackfill()
            startMediaProbeBackfill()
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

    fun setDefaultYearbookCoverStyle(style: YearbookCoverStyle) {
        if (defaultYearbookCoverStyle.value != style) {
            defaultYearbookCoverStyle.value = style
        }
    }

    fun saveYearbookCover(
        year: Int,
        mode: YearbookCoverMode,
        selectedPhotoIds: List<Long>,
        intervalSeconds: Int,
        collageTemplateId: String = "",
        collageSpacingDp: Float = 4f,
        collageBackdrop: CollageBackdropStyle = CollageBackdropStyle.EDITION,
        slotTransforms: List<CollageSlotTransform> = emptyList()
    ) {
        viewModelScope.launch {
            val sanitizedInterval = intervalSeconds.coerceIn(3, 12)
            repository.saveYearbookCover(
                year = year,
                mode = mode,
                selectedPhotoIds = selectedPhotoIds,
                intervalSeconds = sanitizedInterval,
                collageTemplateId = collageTemplateId,
                collageSpacingDp = collageSpacingDp,
                collageBackdrop = collageBackdrop,
                slotTransforms = slotTransforms
            )
            val msg = when (mode) {
                YearbookCoverMode.SMART_CAROUSEL -> {
                    val label = if (defaultYearbookCoverStyle.value == YearbookCoverStyle.COLLAGE) "智能拼图" else "智能轮播"
                    "${year}年已恢复跟随全局默认（$label）"
                }
                YearbookCoverMode.CUSTOM_COLLAGE -> "已保存 ${year}年专属不规则拼图封面"
                YearbookCoverMode.SINGLE_CUSTOM -> "已锁定 ${year}年专属单图封面"
                YearbookCoverMode.CUSTOM_CAROUSEL -> "已保存 ${year}年 ${selectedPhotoIds.size} 张自选轮播封面"
            }
            showBanner(msg)
        }
    }

    fun resetYearbookCoverToSmart(year: Int) {
        viewModelScope.launch {
            repository.resetYearbookCover(year)
            val label = if (defaultYearbookCoverStyle.value == YearbookCoverStyle.COLLAGE) "智能拼图" else "智能轮播"
            showBanner("${year}年已恢复全局默认（$label）")
        }
    }

    suspend fun exportYearbookCollage(
        year: Int,
        photos: List<PhotoItemEntity>,
        template: CollageTemplate,
        spacingDp: Float,
        backdrop: CollageBackdropStyle,
        editionColorArgb: Int,
        slotTransforms: List<CollageSlotTransform>,
        includeYearWatermark: Boolean
    ): CollageExportResult {
        val result = YearbookCollageExporter.exportCollageToGallery(
            context = getApplication(),
            year = year,
            photos = photos,
            template = template,
            spacingDp = spacingDp,
            backdrop = backdrop,
            editionColorArgb = editionColorArgb,
            slotTransforms = slotTransforms,
            includeYearWatermark = includeYearWatermark
        )
        showBanner(result.message)
        return result
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

    fun setTimelineSortOrder(order: TimelineSortOrder) {
        timelineSortOrder.value = order
    }

    fun getMonthPagesForYear(
        year: Int,
        state: FlipYearUiState = uiState.value,
        showEmptyMonths: Boolean = false
    ): List<MonthPageModel> {
        return LibraryDerivationEngine.buildMonthPagesForYear(
            year = year,
            allPhotos = state.allPhotos,
            monthStamps = state.monthStamps,
            sortOrder = state.timelineSortOrder,
            showEmptyMonths = showEmptyMonths
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
            customPhotoIds = customPhotoIds,
            sortOrder = state.timelineSortOrder
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
                    previousBucketName = photo.bucketName,
                    favoriteTouched = markFavorite && !photo.isFavorite
                )
            ),
            description = "已撤回${actionText}操作",
            actionLabel = "撤销：${actionText} 1 项",
            cardKey = "single_${photo.id}"
        )
        viewModelScope.launch {
            suspend fun completeKeep() {
                val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.KEPT, photo.year, photo.month)
                if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
            }
            if (markFavorite && !photo.isFavorite) {
                runFavoriteMutation(
                    photos = listOf(photo),
                    newFavorite = true,
                    snapshot = snapshot,
                    inCleanerSession = fromCleanerSession,
                    showFeedback = !fromCleanerSession,
                    successText = "已收入珍藏并保留 1 项",
                    afterFavoriteApplied = { completeKeep() }
                )
            } else {
                pushUndo(snapshot, inCleanerSession = fromCleanerSession)
                completeKeep()
                if (!fromCleanerSession) {
                    showSnackbar(
                        text = "已标记保留 1 项",
                        undoSnapshot = snapshot
                    )
                }
            }
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
                    previousBucketName = photo.bucketName,
                    favoriteTouched = newFavorite != photo.isFavorite
                )
            ),
            description = "已撤回${actionText}",
            actionLabel = "撤销：${actionText} 1 项"
        )
        viewModelScope.launch {
            runFavoriteMutation(
                photos = listOf(photo),
                newFavorite = newFavorite,
                snapshot = snapshot,
                inCleanerSession = false,
                showFeedback = true,
                successText = if (newFavorite) "已收入珍藏" else "已移出珍藏",
                afterFavoriteApplied = {
                    val currentPhoto = repository.getPhotosByIds(listOf(photo.id)).firstOrNull() ?: photo
                    if (newFavorite && currentPhoto.photoStatus == PhotoStatus.UNREVIEWED) {
                        val newlyStamped = repository.markPhotoStatus(photo.id, PhotoStatus.KEPT, photo.year, photo.month)
                        if (newlyStamped) triggerMonthStamped(photo.year, photo.month)
                    }
                }
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
                    previousBucketName = it.bucketName,
                    favoriteTouched = markFavorite && it.id == stack.bestPhoto.id && !it.isFavorite
                )
            },
            description = "已撤回${actionText} (${stack.photos.size} 张)",
            actionLabel = "撤销：保留连拍 ${stack.photos.size} 张",
            cardKey = stack.stableKey
        )
        viewModelScope.launch {
            suspend fun completeKeepStack() {
                val months = stack.photos.map { it.year to it.month }.toSet()
                val newlyStamped = repository.markPhotosStatus(
                    ids = stack.photos.map { it.id },
                    status = PhotoStatus.KEPT,
                    affectedMonths = months
                )
                if (newlyStamped) triggerMonthStamped(stack.bestPhoto.year, stack.bestPhoto.month)
            }
            if (markFavorite && !stack.bestPhoto.isFavorite) {
                runFavoriteMutation(
                    photos = listOf(stack.bestPhoto),
                    newFavorite = true,
                    snapshot = snapshot,
                    inCleanerSession = fromCleanerSession,
                    showFeedback = !fromCleanerSession,
                    successText = "已保留整组 ${stack.photos.size} 张连拍照片",
                    afterFavoriteApplied = { completeKeepStack() }
                )
            } else {
                pushUndo(snapshot, inCleanerSession = fromCleanerSession)
                completeKeepStack()
                if (!fromCleanerSession) {
                    showSnackbar(
                        text = "已保留整组 ${stack.photos.size} 张连拍照片",
                        undoSnapshot = snapshot
                    )
                }
            }
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
                    previousBucketName = it.bucketName,
                    favoriteTouched = markFavorite && !it.isFavorite
                )
            },
            description = if (markFavorite) "已撤回批量收藏 (${photos.size} 项)" else "已撤回批量保留 (${photos.size} 项)",
            actionLabel = if (markFavorite) "撤销：收藏 ${photos.size} 项" else "撤销：保留 ${photos.size} 项"
        )
        viewModelScope.launch {
            val ids = photos.map { it.id }
            suspend fun completeBulkKeep() {
                val months = photos.map { it.year to it.month }.toSet()
                val newlyStamped = repository.markPhotosStatus(
                    ids = ids,
                    status = PhotoStatus.KEPT,
                    affectedMonths = months
                )
                if (newlyStamped) {
                    val first = photos.first()
                    triggerMonthStamped(first.year, first.month)
                }
            }
            if (markFavorite) {
                runFavoriteMutation(
                    photos = photos,
                    newFavorite = true,
                    snapshot = snapshot,
                    inCleanerSession = false,
                    showFeedback = true,
                    successText = "已收入珍藏并保留 ${photos.size} 项媒体",
                    afterFavoriteApplied = { completeBulkKeep() }
                )
            } else {
                pushUndo(snapshot)
                completeBulkKeep()
                showSnackbar(
                    text = "已批量保留 ${photos.size} 项媒体",
                    undoSnapshot = snapshot
                )
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
            val imageCount = photos.count { !it.isVideo && isSafelyCompressibleImage(it) }
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
                        if (result.skippedProtectedCount > 0) {
                            append("；已保护 ${result.skippedProtectedCount} 个 Live/特殊格式")
                        }
                    }
                    result.skippedProtectedCount > 0 ->
                        "已保护 ${result.skippedProtectedCount} 个 Live/特殊格式，未进行破坏性压缩"
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
        val favoriteBackups = snapshot.photoStates.filter { it.favoriteTouched }
        if (favoriteBackups.isEmpty()) {
            applyUndoSnapshotRestore(snapshot, triggeredByShake)
            return
        }

        val photos = repository.getPhotosByIds(favoriteBackups.map { it.photoId })
        val backupById = favoriteBackups.associateBy { it.photoId }
        val favoriteBatches = photos
            .mapNotNull { photo ->
                val backup = backupById[photo.id] ?: return@mapNotNull null
                photo to backup.previousFavorite
            }
            .groupBy({ it.second }, { it.first })
            .map { (favorite, targets) -> FavoriteConsentUnit(targets, favorite) }
        applyUndoFavoriteBatchesThen(favoriteBatches, 0) {
            applyUndoSnapshotRestore(snapshot, triggeredByShake)
        }
    }

    private suspend fun applyUndoFavoriteBatchesThen(
        batches: List<FavoriteConsentUnit>,
        index: Int,
        onComplete: suspend () -> Unit
    ) {
        if (index >= batches.size) {
            onComplete()
            return
        }
        val batch = batches[index]
        writeFavoritesWithOptionalConsent(
            photos = batch.photos,
            newFavorite = batch.favorite,
            onApplied = {
                applyUndoFavoriteBatchesThen(batches, index + 1, onComplete)
            },
            onRejected = {
                showBanner("未获得系统授权，撤销未执行")
            }
        )
    }

    private suspend fun applyUndoSnapshotRestore(snapshot: UndoSnapshot, triggeredByShake: Boolean) {
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

private const val PRECOMPUTED_LARGE_PHOTO_THRESHOLD_BYTES = 3_000_000L
private const val FAVORITE_CONSENT_CHUNK_SIZE = 1_000
private const val KEY_SYSTEM_FAVORITE_MIGRATION_DONE = "system_favorite_migration_done_v1"

/**
 * Single-pass background indexing of Gallery metadata & monthly timeline clusters.
 * Runs off the main thread inside `derivedLibraryFlow` on `Dispatchers.Default` so
 * tapping the Gallery tab mounts in O(1) even on 10,000+ item libraries.
 */
internal fun buildPrecomputedGalleryIndex(
    entries: List<GalleryEntry>,
    monthStamps: Map<String, MonthStampEntity>
): PrecomputedGalleryIndex {
    if (entries.isEmpty()) {
        return PrecomputedGalleryIndex(sourceEntries = entries)
    }
    val albumCounts = HashMap<String, Int>(24)
    val bursts = ArrayList<GalleryEntry.Burst>()
    val treasured = filterTreasuredGalleryEntries(entries)
    val largePhotos = ArrayList<GalleryEntry>()
    val missingLocation = ArrayList<GalleryEntry>()

    for (entry in entries) {
        when (entry) {
            is GalleryEntry.Single -> {
                val photo = entry.photo
                val album = photo.bucketName?.trim()
                if (!album.isNullOrEmpty()) {
                    albumCounts[album] = (albumCounts[album] ?: 0) + 1
                }
                if (!photo.isVideo && photo.fileSizeBytes >= PRECOMPUTED_LARGE_PHOTO_THRESHOLD_BYTES) {
                    largePhotos.add(entry)
                }
                if (photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null) {
                    missingLocation.add(entry)
                }
            }
            is GalleryEntry.Burst -> {
                bursts.add(entry)
                var hasLarge = false
                var hasMissingLoc = false
                for (photo in entry.photos) {
                    val album = photo.bucketName?.trim()
                    if (!album.isNullOrEmpty()) {
                        albumCounts[album] = (albumCounts[album] ?: 0) + 1
                    }
                    if (!hasLarge && !photo.isVideo && photo.fileSizeBytes >= PRECOMPUTED_LARGE_PHOTO_THRESHOLD_BYTES) {
                        hasLarge = true
                    }
                    if (!hasMissingLoc && (photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null)) {
                        hasMissingLoc = true
                    }
                }
                if (hasLarge) largePhotos.add(entry)
                if (hasMissingLoc) missingLocation.add(entry)
            }
        }
    }

    val existingAlbums = albumCounts.entries
        .sortedByDescending { it.value }
        .map { it.key }
    val keywordTags = extractDescriptiveKeywordTags(entries)
    val defaultClusterLayout = buildTimelineClusterLayout(entries, monthStamps)

    return PrecomputedGalleryIndex(
        sourceEntries = entries,
        existingAlbums = existingAlbums,
        burstEntries = bursts,
        treasuredEntries = treasured,
        largePhotoEntries = largePhotos,
        missingLocationEntries = missingLocation,
        extractedKeywordTags = keywordTags,
        defaultClusterLayout = defaultClusterLayout
    )
}

