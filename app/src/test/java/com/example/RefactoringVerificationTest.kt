package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.runtime.mutableStateOf
import com.example.data.MediaAccessLevel
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import androidx.compose.ui.geometry.Offset
import com.example.ui.CleanupCardItem
import com.example.ui.FlipYearUiState
import com.example.ui.LocationCluster
import com.example.ui.MediaFilter
import com.example.ui.components.ArcTimeScaleScrubber
import com.example.ui.components.CHINA_DEFAULT_LAT
import com.example.ui.components.CHINA_DEFAULT_LON
import com.example.ui.components.CHINA_DEFAULT_ZOOM
import com.example.ui.components.ProjectedClusterMeta
import com.example.ui.components.buildVisibleAggregatedPins
import com.example.ui.components.computeChinaFocusTarget
import com.example.ui.components.formatEditorialPhotoTitle
import com.example.ui.components.isInsideChinaMainland
import com.example.ui.components.isMachineGeneratedFilename
import com.example.ui.components.latToNormY
import com.example.ui.components.lonToNormX
import com.example.ui.domain.LibraryDerivationEngine
import com.example.ui.screens.BookshelfScreen
import com.example.ui.screens.GalleryScreen
import kotlin.math.hypot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.FlipYearTheme
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.TimelineSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class RefactoringVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun photo(
        id: Long,
        year: Int = 2026,
        month: Int = 10,
        day: Int = 18,
        caption: String = "IMG_20261018_091500",
        locationName: String? = "京都 · 祇园",
        bucketName: String? = "Camera",
        fileSizeBytes: Long = 4_200_000L,
        burstGroupId: String? = null,
        status: PhotoStatus = PhotoStatus.UNREVIEWED
    ): PhotoItemEntity = PhotoItemEntity(
        id = id,
        year = year,
        month = month,
        day = day,
        takenAtMillis = 1_792_300_000_000L + id * 1_000L,
        fileSizeBytes = fileSizeBytes,
        widthPx = 4032,
        heightPx = 3024,
        drawableName = "img_photo_kyoto",
        status = status.name,
        burstGroupId = burstGroupId,
        caption = caption,
        bucketName = bucketName,
        locationName = locationName,
        latitude = if (locationName != null) 35.01 else null,
        longitude = if (locationName != null) 135.76 else null
    )

    @Test
    fun editorialTitleFormatter_beautifiesMachineFilenamesAndPreservesCuratedCaptions() {
        assertTrue(isMachineGeneratedFilename("20261109_091925"))
        assertTrue(isMachineGeneratedFilename("IMG_20261018_091500"))
        assertTrue(isMachineGeneratedFilename("Screenshot_20251228_102200"))
        assertTrue(isMachineGeneratedFilename("03fff229ff10c39e4a39875705b54f"))
        assertFalse(isMachineGeneratedFilename("清水寺石阶晚霞"))

        val cameraPhoto = photo(1L, caption = "20261109_091925", locationName = "京都")
        val cameraTitle = formatEditorialPhotoTitle(cameraPhoto)
        assertTrue("Expected location in editorial title, got: $cameraTitle", cameraTitle.contains("京都"))
        assertFalse("Raw timestamp filename should be replaced: $cameraTitle", cameraTitle.contains("20261109"))

        val screenshotPhoto = photo(2L, caption = "Screenshot_20261018", locationName = null, bucketName = "Screenshots")
        assertEquals("屏幕截图 · 10月18日", formatEditorialPhotoTitle(screenshotPhoto))

        val curatedPhoto = photo(3L, caption = "清水寺石阶晚霞")
        assertEquals("清水寺石阶晚霞", formatEditorialPhotoTitle(curatedPhoto))
    }

    @Test
    fun libraryDerivationEngine_buildArcQueueSupportsCustomPhotoIdsAndBurstGrouping() {
        val b1 = photo(10L, burstGroupId = "b_kyoto", status = PhotoStatus.KEPT)
        val b2 = photo(11L, burstGroupId = "b_kyoto", status = PhotoStatus.KEPT)
        val s1 = photo(12L, status = PhotoStatus.UNREVIEWED)
        val excluded = photo(13L, status = PhotoStatus.UNREVIEWED)

        // Default NEWEST_FIRST: s1 (id=12) precedes burst b_kyoto (id=10,11), while burst internals stay chronological
        val newestQueue = LibraryDerivationEngine.buildArcQueue(
            allPhotos = listOf(b1, b2, s1, excluded),
            includeReviewed = true,
            customPhotoIds = setOf(10L, 11L, 12L),
            sortOrder = TimelineSortOrder.NEWEST_FIRST
        )
        assertEquals(2, newestQueue.size)
        assertTrue(newestQueue[0] is CleanupCardItem.SinglePhoto)
        assertEquals(12L, (newestQueue[0] as CleanupCardItem.SinglePhoto).photo.id)
        assertTrue(newestQueue[1] is CleanupCardItem.BurstStack)
        assertEquals(listOf(10L, 11L), (newestQueue[1] as CleanupCardItem.BurstStack).photos.map { it.id })

        // OLDEST_FIRST: burst b_kyoto precedes s1
        val oldestQueue = LibraryDerivationEngine.buildArcQueue(
            allPhotos = listOf(b1, b2, s1, excluded),
            includeReviewed = true,
            customPhotoIds = setOf(10L, 11L, 12L),
            sortOrder = TimelineSortOrder.OLDEST_FIRST
        )
        assertEquals(2, oldestQueue.size)
        assertTrue(oldestQueue[0] is CleanupCardItem.BurstStack)
        assertEquals(listOf(10L, 11L), (oldestQueue[0] as CleanupCardItem.BurstStack).photos.map { it.id })
        assertTrue(oldestQueue[1] is CleanupCardItem.SinglePhoto)
        assertEquals(12L, (oldestQueue[1] as CleanupCardItem.SinglePhoto).photo.id)
    }

    @Test
    fun unifiedTimelineOrdering_alignsYearbooksAndMonthPagesAndEmptyMonthToggle() {
        val pJan2025 = photo(1L, year = 2025, month = 1, day = 5)
        val pMar2026 = photo(2L, year = 2026, month = 3, day = 8)
        val pNov2026 = photo(3L, year = 2026, month = 11, day = 18)
        val all = listOf(pJan2025, pMar2026, pNov2026)

        // Yearbooks order
        val yearsDesc = LibraryDerivationEngine.buildYearbooksLinear(all, emptyMap(), emptyMap(), TimelineSortOrder.NEWEST_FIRST)
        val yearsAsc = LibraryDerivationEngine.buildYearbooksLinear(all, emptyMap(), emptyMap(), TimelineSortOrder.OLDEST_FIRST)
        assertEquals(listOf(2026, 2025), yearsDesc.map { it.year })
        assertEquals(listOf(2025, 2026), yearsAsc.map { it.year })

        // Month pages default: populated only, newest first
        val monthsDescPopulated = LibraryDerivationEngine.buildMonthPagesForYear(
            year = 2026,
            allPhotos = all,
            monthStamps = emptyMap(),
            sortOrder = TimelineSortOrder.NEWEST_FIRST,
            showEmptyMonths = false
        )
        assertEquals(listOf(11, 3), monthsDescPopulated.map { it.month })

        // Month pages populated only, oldest first
        val monthsAscPopulated = LibraryDerivationEngine.buildMonthPagesForYear(
            year = 2026,
            allPhotos = all,
            monthStamps = emptyMap(),
            sortOrder = TimelineSortOrder.OLDEST_FIRST,
            showEmptyMonths = false
        )
        assertEquals(listOf(3, 11), monthsAscPopulated.map { it.month })

        // Month pages with empty months enabled, newest first (12..1)
        val monthsDescAll = LibraryDerivationEngine.buildMonthPagesForYear(
            year = 2026,
            allPhotos = all,
            monthStamps = emptyMap(),
            sortOrder = TimelineSortOrder.NEWEST_FIRST,
            showEmptyMonths = true
        )
        assertEquals((12 downTo 1).toList(), monthsDescAll.map { it.month })
    }

    @Test
    fun userPreferences_persistsChronologyAndViewSettings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferences(context)
        prefs.setTimelineSortOrder(TimelineSortOrder.OLDEST_FIRST)
        prefs.setShowEmptyMonths(true)
        prefs.setFlipbookDefaultView(FlipbookDefaultView.GALLERY)
        prefs.setDefaultStartTab(DefaultStartTab.GALLERY)
        prefs.setGalleryGridDensity(GalleryGridDensity.COMPACT)

        val reloaded = ThemePreferences(context).userPreferences.value
        assertEquals(TimelineSortOrder.OLDEST_FIRST, reloaded.timelineSortOrder)
        assertTrue(reloaded.showEmptyMonths)
        assertEquals(FlipbookDefaultView.GALLERY, reloaded.flipbookDefaultView)
        assertEquals(DefaultStartTab.GALLERY, reloaded.defaultStartTab)
        assertEquals(GalleryGridDensity.COMPACT, reloaded.galleryGridDensity)
    }

    @Test
    fun arcTimeScaleScrubber_followsGlobalSortOrderEndpointsAndProgress() {
        val marMillis = java.util.Calendar.getInstance().apply { set(2026, 2, 10, 10, 0, 0) }.timeInMillis
        val novMillis = java.util.Calendar.getInstance().apply { set(2026, 10, 20, 18, 0, 0) }.timeInMillis
        val pMar = photo(201L, year = 2026, month = 3, day = 10).copy(takenAtMillis = marMillis)
        val pNov = photo(202L, year = 2026, month = 11, day = 20).copy(takenAtMillis = novMillis)

        val sortOrderState = mutableStateOf(TimelineSortOrder.NEWEST_FIRST)
        composeRule.setContent {
            val order = sortOrderState.value
            val queue = LibraryDerivationEngine.buildArcQueue(
                allPhotos = listOf(pMar, pNov),
                sortOrder = order
            )
            FlipYearTheme {
                ArcTimeScaleScrubber(
                    queue = queue,
                    currentIndex = 0,
                    sortOrder = order,
                    onJumpToIndex = {}
                )
            }
        }

        // NEWEST_FIRST: index 0 is Nov -> left label = 11月, right label = 3月, progress starts at 0f (left)
        composeRule.onNodeWithTag("arc_scrubber_start_label").assertTextEquals("11月")
        composeRule.onNodeWithTag("arc_scrubber_end_label").assertTextEquals("3月")
        composeRule.onNodeWithTag("arc_time_scale_scrubber")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        // Switch to OLDEST_FIRST: index 0 is Mar -> left label = 3月, right label = 11月, progress starts at 0f (left)
        composeRule.runOnIdle { sortOrderState.value = TimelineSortOrder.OLDEST_FIRST }
        composeRule.onNodeWithTag("arc_scrubber_start_label").assertTextEquals("3月")
        composeRule.onNodeWithTag("arc_scrubber_end_label").assertTextEquals("11月")
        composeRule.onNodeWithTag("arc_time_scale_scrubber")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun galleryScreen_quickSmartFilterAndSwipeCleanBridgeAndGlobalFilterBanner() {
        val b1 = photo(1L, burstGroupId = "g1", fileSizeBytes = 1_500_000L)
        val b2 = photo(2L, burstGroupId = "g1", fileSizeBytes = 1_500_000L)
        val large = photo(3L, fileSizeBytes = 6_000_000L, locationName = null)

        val entries = LibraryDerivationEngine.buildGalleryEntries(listOf(b1, b2, large))
        val uiState = FlipYearUiState(
            isLoading = false,
            mediaAccessLevel = MediaAccessLevel.FULL,
            mediaFilter = MediaFilter.PHOTOS,
            allPhotos = listOf(b1, b2, large),
            galleryEntries = entries
        )

        var launchedCustomTitle: String? = null
        var launchedCustomIds: Set<Long> = emptySet()
        var resetGlobalFilterCalled = false

        composeRule.setContent {
            FlipYearTheme {
                GalleryScreen(
                    uiState = uiState,
                    onSelectBestInBurst = { _, _ -> },
                    onKeepBestInBurst = { _, _ -> },
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = {},
                    onLoadDemoArchive = {},
                    onStartCustomCleanup = { title, ids, _ ->
                        launchedCustomTitle = title
                        launchedCustomIds = ids
                    },
                    onResetGlobalMediaFilter = { resetGlobalFilterCalled = true }
                )
            }
        }

        composeRule.waitForIdle()
        // Global MediaFilter banner is visible when Bookshelf filter != ALL
        composeRule.onNodeWithTag("gallery_global_filter_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_reset_global_filter_button").performClick()
        composeRule.waitForIdle()
        assertTrue(resetGlobalFilterCalled)

        // Quick smart filter strip lets user jump to Bursts in 1 tap
        composeRule.onNodeWithTag("gallery_quick_smart_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_quick_bursts").performClick()
        composeRule.waitForIdle()

        // Swipe-clean button sends the filtered burst photos straight into ArcCleaner
        composeRule.onNodeWithTag("gallery_swipe_clean_button").performClick()
        composeRule.waitForIdle()
        assertEquals("整理 · 连拍组", launchedCustomTitle)
        assertEquals(setOf(1L, 2L), launchedCustomIds)
    }

    @Test
    fun bookshelfScreen_exposesBothFlipbookAndCleanupCta() {
        composeRule.mainClock.autoAdvance = false
        val p1 = photo(100L, year = 2026)
        val books = LibraryDerivationEngine.buildYearbooksLinear(listOf(p1), emptyMap(), emptyMap())
        val uiState = FlipYearUiState(
            isLoading = false,
            mediaAccessLevel = MediaAccessLevel.FULL,
            allPhotos = listOf(p1),
            yearbooks = books,
            totalImageCount = 1,
            totalVideoCount = 0
        )
        var openedFlipbookYear = -1
        var startedCleanupYear = -1

        composeRule.setContent {
            FlipYearTheme {
                BookshelfScreen(
                    uiState = uiState,
                    onOpenYearbook = { openedFlipbookYear = it },
                    onStartYearCleanup = { y, _ -> startedCleanupYear = y },
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = {},
                    onLoadDemoArchive = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("open_flipbook_button").assertDoesNotExist()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("open_yearbook_button").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()

        assertEquals(2026, openedFlipbookYear)
        assertEquals(2026, startedCleanupYear)
    }

    @Test
    fun footprintMap_chinaFocusTarget_framesChinaEvenWithOverseasClusters() {
        fun makeCluster(name: String, lat: Double, lon: Double, count: Int): LocationCluster {
            val items = (1..count).map { photo(it.toLong() + name.hashCode(), locationName = name) }
            return LocationCluster(
                locationName = name,
                latitude = lat,
                longitude = lon,
                photos = items,
                unreviewedCount = count,
                totalBytes = items.sumOf { it.fileSizeBytes },
                coverPhoto = items.first()
            )
        }

        // Empty -> canonical China overview
        val emptyTarget = computeChinaFocusTarget(emptyList())
        assertEquals(CHINA_DEFAULT_LAT, emptyTarget.lat, 0.01)
        assertEquals(CHINA_DEFAULT_LON, emptyTarget.lon, 0.01)
        assertEquals(CHINA_DEFAULT_ZOOM, emptyTarget.zoom, 0.01f)

        // Overseas-only -> still frames China cleanly on Chinese layer
        val overseasOnly = listOf(
            makeCluster("巴黎", 48.8566, 2.3522, 6),
            makeCluster("旧金山", 37.7749, -122.4194, 4)
        )
        val overseasTarget = computeChinaFocusTarget(overseasOnly)
        assertEquals(CHINA_DEFAULT_LAT, overseasTarget.lat, 0.01)
        assertEquals(CHINA_DEFAULT_LON, overseasTarget.lon, 0.01)
        assertTrue(overseasTarget.zoom >= CHINA_DEFAULT_ZOOM)

        // Mixed China + Overseas -> focuses China cities without zooming out to world level (2.0f)
        val mixed = listOf(
            makeCluster("北京", 39.9042, 116.4074, 15),
            makeCluster("上海", 31.2304, 121.4737, 12),
            makeCluster("巴黎", 48.8566, 2.3522, 9),
            makeCluster("纽约", 40.7128, -74.0060, 8)
        )
        val mixedTarget = computeChinaFocusTarget(mixed)
        assertTrue(isInsideChinaMainland(mixedTarget.lat, mixedTarget.lon))
        assertTrue("Expected China zoom >= 3.45f, got ${mixedTarget.zoom}", mixedTarget.zoom >= CHINA_DEFAULT_ZOOM)
    }

    @Test
    fun footprintMap_radialPinClustering_preventsOverlapAcrossGridBoundaries() {
        fun makeMeta(name: String, count: Int, screenX: Float, screenY: Float): Pair<ProjectedClusterMeta, Offset> {
            val items = (1..count).map { photo(it.toLong() + name.hashCode(), locationName = name) }
            val cluster = LocationCluster(
                locationName = name,
                latitude = 31.2,
                longitude = 121.4,
                photos = items,
                unreviewedCount = count,
                totalBytes = items.sumOf { it.fileSizeBytes },
                coverPhoto = items.first()
            )
            // Encode screenX/screenY via normX/normY for deterministic test projection
            return ProjectedClusterMeta(cluster, screenX.toDouble(), screenY.toDouble()) to Offset(screenX, screenY)
        }

        // Two clusters sitting right across a 58px grid edge (57px vs 59px) plus nearby & distant clusters
        val entries = listOf(
            makeMeta("陆家嘴", 20, 57f, 100f),
            makeMeta("外滩", 8, 59f, 101f),
            makeMeta("静安寺", 5, 76f, 112f),
            makeMeta("西湖", 14, 165f, 190f),
            makeMeta("灵隐寺", 6, 178f, 198f),
            makeMeta("夫子庙", 11, 260f, 120f)
        )
        val minSep = 48f
        val pins = buildVisibleAggregatedPins(
            metas = entries.map { it.first },
            selectedLocationName = null,
            viewportWidthPx = 400f,
            viewportHeightPx = 600f,
            minSeparationPx = minSep,
            selectedSeparationPx = 56f,
            maxVisiblePins = 12,
            project = { nx, ny -> Offset(nx.toFloat(), ny.toFloat()) }
        )

        // Should cleanly merge into 3 well-separated stacks: 陆家嘴(+2), 西湖(+1), 夫子庙
        assertEquals(3, pins.size)
        val shanghaiPin = pins.first { it.primaryCluster.locationName == "陆家嘴" }
        assertEquals(3, shanghaiPin.mergedClustersCount)
        assertEquals(33, shanghaiPin.totalPhotosCount)

        // Every pair of rendered pins must strictly respect minSeparationPx (zero overlaps)
        for (i in pins.indices) {
            for (j in i + 1 until pins.size) {
                val dist = hypot(
                    (pins[i].screenX - pins[j].screenX).toDouble(),
                    (pins[i].screenY - pins[j].screenY).toDouble()
                ).toFloat()
                assertTrue("Pins overlapped: dist=$dist < minSep=$minSep", dist >= minSep)
            }
        }
    }
}
