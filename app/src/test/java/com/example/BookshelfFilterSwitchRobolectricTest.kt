package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.MediaAccessLevel
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.FlipYearUiState
import com.example.ui.MediaFilter
import com.example.ui.YearbookCoverCurator
import com.example.ui.YearbookSummary
import com.example.ui.screens.BookshelfScreen
import com.example.ui.theme.FlipYearTheme
import com.example.ui.theme.AppThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class BookshelfFilterSwitchRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleItem(
        id: Long,
        year: Int,
        month: Int,
        isVideo: Boolean
    ): PhotoItemEntity = PhotoItemEntity(
        id = id,
        year = year,
        month = month,
        day = 10,
        takenAtMillis = year * 10_000L + month * 100L + id,
        mediaType = if (isVideo) "VIDEO" else "IMAGE",
        durationMs = if (isVideo) 12_000L else 0L,
        drawableName = "img_photo_kyoto",
        caption = if (isVideo) "Video $id" else "Photo $id",
        locationName = "Tokyo",
        fileSizeBytes = 2_000_000L,
        widthPx = 3000,
        heightPx = 4000,
        qualityScore = 88,
        status = PhotoStatus.UNREVIEWED.name
    )

    private fun buildState(
        allItems: List<PhotoItemEntity>,
        filter: MediaFilter
    ): FlipYearUiState {
        val filtered = when (filter) {
            MediaFilter.ALL -> allItems
            MediaFilter.PHOTOS -> allItems.filter { !it.isVideo }
            MediaFilter.VIDEOS -> allItems.filter { it.isVideo }
        }
        val books = filtered
            .groupBy { it.year }
            .entries
            .sortedByDescending { it.key }
            .mapIndexed { idx, (year, yearPhotos) ->
                val resolved = YearbookCoverCurator.resolveCoverPresentation(yearPhotos, null)
                YearbookSummary(
                    year = year,
                    coverPhoto = resolved.coverPhoto,
                    coverSlides = resolved.slides,
                    coverMode = resolved.mode,
                    coverIntervalSeconds = resolved.intervalSeconds,
                    customCoverPhotoIds = resolved.customPhotoIds,
                    totalPhotos = yearPhotos.size,
                    videoCount = yearPhotos.count { it.isVideo },
                    unreviewedCount = yearPhotos.size,
                    keptCount = 0,
                    trashedCount = 0,
                    cleanedMonthsCount = 0,
                    totalMonthsCount = yearPhotos.map { it.month }.distinct().size,
                    reclaimableBytes = yearPhotos.sumOf { it.fileSizeBytes },
                    editionIndex = idx % 4
                )
            }
        return FlipYearUiState(
            isLoading = false,
            mediaAccessLevel = MediaAccessLevel.FULL,
            mediaFilter = filter,
            allPhotos = filtered,
            yearbooks = books,
            totalImageCount = allItems.count { !it.isVideo },
            totalVideoCount = allItems.count { it.isVideo }
        )
    }

    private fun pumpFrames() {
        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()
    }

    @Test
    fun switchAllToVideosSingleYearThenBackToPhotosMultiYear_inDarkTheme_doesNotCrash() {
        // Mirrors real Samsung library: 10 photos across 2026/2025/2024 + 3 videos only in 2025 (Dark Theme)
        // Manual clock avoids waiting forever on the intentional infinite Ken Burns cover slideshow loop.
        composeRule.mainClock.autoAdvance = false

        val allItems = listOf(
            sampleItem(1, 2026, 5, isVideo = false),
            sampleItem(2, 2026, 4, isVideo = false),
            sampleItem(3, 2026, 3, isVideo = false),
            sampleItem(4, 2025, 11, isVideo = false),
            sampleItem(5, 2025, 9, isVideo = false),
            sampleItem(6, 2025, 7, isVideo = false),
            sampleItem(7, 2025, 6, isVideo = true),
            sampleItem(8, 2025, 5, isVideo = true),
            sampleItem(9, 2025, 4, isVideo = true),
            sampleItem(10, 2024, 10, isVideo = false),
            sampleItem(11, 2024, 8, isVideo = false),
            sampleItem(12, 2024, 6, isVideo = false),
            sampleItem(13, 2024, 2, isVideo = false)
        )

        var filter by mutableStateOf(MediaFilter.ALL)

        composeRule.setContent {
            FlipYearTheme(themeMode = AppThemeMode.DARK) {
                BookshelfScreen(
                    uiState = buildState(allItems, filter),
                    onOpenYearbook = {},
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = { filter = it },
                    onLoadDemoArchive = {}
                )
            }
        }

        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()

        // Tap Videos (shrinks from 3 years to 1 year: 2025)
        composeRule.onNodeWithTag("filter_videos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2025").assertIsDisplayed()

        // Tap Photos (grows from 1 year to 3 years: 2026, 2025, 2024) — previously crashed with IndexOutOfBoundsException!
        composeRule.onNodeWithTag("filter_photos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()

        // Rapid toggle back and forth
        composeRule.onNodeWithTag("filter_videos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("filter_photos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("filter_all").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()
    }

    @Test
    fun switchAllToVideosDisjointYearsThenPhotos_doesNotCrash() {
        composeRule.mainClock.autoAdvance = false

        // Disjoint years: videos in 2026 only, photos in 2025 & 2024 only
        val allItems = listOf(
            sampleItem(1, 2026, 5, isVideo = true),
            sampleItem(2, 2026, 4, isVideo = true),
            sampleItem(3, 2026, 3, isVideo = true),
            sampleItem(4, 2025, 11, isVideo = false),
            sampleItem(5, 2025, 9, isVideo = false),
            sampleItem(6, 2024, 8, isVideo = false),
            sampleItem(7, 2024, 2, isVideo = false)
        )

        var filter by mutableStateOf(MediaFilter.ALL)

        composeRule.setContent {
            FlipYearTheme(themeMode = AppThemeMode.LIGHT) {
                BookshelfScreen(
                    uiState = buildState(allItems, filter),
                    onOpenYearbook = {},
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = { filter = it },
                    onLoadDemoArchive = {}
                )
            }
        }

        pumpFrames()
        composeRule.onNodeWithTag("filter_videos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()

        composeRule.onNodeWithTag("filter_photos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2025").assertIsDisplayed()
    }

    @Test
    fun switchFromNonZeroPageToFewerVideosThenPhotos_doesNotCrash() {
        val allItems = listOf(
            sampleItem(1, 2026, 5, isVideo = false),
            sampleItem(2, 2025, 8, isVideo = false),
            sampleItem(3, 2024, 6, isVideo = false),
            sampleItem(4, 2024, 5, isVideo = true),
            sampleItem(5, 2023, 3, isVideo = false)
        )

        var filter by mutableStateOf(MediaFilter.ALL)

        composeRule.setContent {
            FlipYearTheme {
                BookshelfScreen(
                    uiState = buildState(allItems, filter),
                    onOpenYearbook = {},
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = { filter = it },
                    onLoadDemoArchive = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Scroll to 2023 (page index 3) first
        composeRule.onNodeWithTag("year_wheel_2023").performClick()
        composeRule.waitForIdle()
        // Switch to Videos (only 1 year: 2024)
        composeRule.onNodeWithTag("filter_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("yearbook_card_2024").assertIsDisplayed()
        // Switch to Photos (4 years: 2026, 2025, 2024, 2023)
        composeRule.onNodeWithTag("filter_photos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()
    }

    @Test
    fun switchToEmptyVideosThenBackToPhotos_doesNotCrash() {
        composeRule.mainClock.autoAdvance = false

        val allItems = listOf(
            sampleItem(1, 2026, 5, isVideo = false),
            sampleItem(2, 2025, 8, isVideo = false)
        )

        var filter by mutableStateOf(MediaFilter.ALL)

        composeRule.setContent {
            FlipYearTheme(themeMode = AppThemeMode.DARK) {
                BookshelfScreen(
                    uiState = buildState(allItems, filter),
                    onOpenYearbook = {},
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = { filter = it },
                    onLoadDemoArchive = {}
                )
            }
        }

        pumpFrames()
        composeRule.onNodeWithTag("filter_videos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("filter_photos").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()
    }

    @Test
    fun sameFilterDynamicBooksGrowth_fromOneYearToThreeYears_doesNotCrash() {
        var items by mutableStateOf(
            listOf(sampleItem(1, 2025, 6, isVideo = false))
        )

        composeRule.setContent {
            FlipYearTheme(themeMode = AppThemeMode.DARK) {
                BookshelfScreen(
                    uiState = buildState(items, MediaFilter.ALL),
                    onOpenYearbook = {},
                    onRequestMediaPermission = {},
                    onRescanDeviceMedia = {},
                    onSelectFilter = {},
                    onLoadDemoArchive = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("yearbook_card_2025").assertIsDisplayed()

        // Background scan finishes and adds 2026 & 2024 within the SAME MediaFilter
        composeRule.runOnIdle {
            items = listOf(
                sampleItem(10, 2026, 8, isVideo = false),
                sampleItem(1, 2025, 6, isVideo = false),
                sampleItem(20, 2024, 3, isVideo = false)
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("yearbook_card_2026").assertIsDisplayed()
    }
}
