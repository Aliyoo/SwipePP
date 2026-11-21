package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.MediaAccessLevel
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.CleanupCardItem
import com.example.ui.FlipYearUiState
import com.example.ui.MediaFilter
import com.example.ui.components.formatEditorialPhotoTitle
import com.example.ui.components.isMachineGeneratedFilename
import com.example.ui.domain.LibraryDerivationEngine
import com.example.ui.screens.BookshelfScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.theme.FlipYearTheme
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

        val queue = LibraryDerivationEngine.buildArcQueue(
            allPhotos = listOf(b1, b2, s1, excluded),
            includeReviewed = true,
            customPhotoIds = setOf(10L, 11L, 12L)
        )

        assertEquals(2, queue.size)
        assertTrue(queue[0] is CleanupCardItem.BurstStack)
        assertEquals(2, (queue[0] as CleanupCardItem.BurstStack).photos.size)
        assertTrue(queue[1] is CleanupCardItem.SinglePhoto)
        assertEquals(12L, (queue[1] as CleanupCardItem.SinglePhoto).photo.id)
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
}
