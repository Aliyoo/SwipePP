package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.CompressionPreset
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.CleanerEntrySource
import com.example.ui.CleanupCardItem
import com.example.ui.GalleryEntry
import com.example.ui.TimelineSearchQuery
import com.example.ui.components.GallerySmartFilter
import com.example.ui.components.TimelineSearchFilterSheet
import com.example.ui.components.daysStagedInVault
import com.example.ui.components.formatVaultStagedLabel
import com.example.ui.components.monthEnglishName
import com.example.ui.filterGalleryEntriesBySearch
import com.example.ui.screens.ArcCleanerScreen
import com.example.ui.screens.filterGalleryEntriesBySmartAndAlbum
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.FlipYearTheme
import com.example.ui.theme.ThemePreferences
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
class OverhaulVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun samplePhoto(
        id: Long,
        ts: Long,
        status: PhotoStatus = PhotoStatus.UNREVIEWED,
        burstGroupId: String? = null
    ): PhotoItemEntity = PhotoItemEntity(
        id = id,
        year = 2025,
        month = 8,
        day = 12,
        takenAtMillis = ts,
        fileSizeBytes = 3_000_000L,
        widthPx = 4000,
        heightPx = 3000,
        drawableName = "img_photo_kyoto",
        status = status.name,
        burstGroupId = burstGroupId,
        caption = "Photo $id",
        bucketName = "Camera"
    )

    @Test
    fun stage1_vaultStagedLabel_replacesFakeCountdown() {
        val now = 1_750_000_000_000L
        assertEquals(0, daysStagedInVault(now - 3_600_000L, now))
        assertEquals("已暂存 <1 天", formatVaultStagedLabel(now - 3_600_000L, now))
        assertEquals(5, daysStagedInVault(now - 5 * 86_400_000L, now))
        assertEquals("已暂存 5 天", formatVaultStagedLabel(now - 5 * 86_400_000L, now))
        assertFalse(formatVaultStagedLabel(now - 2 * 86_400_000L, now).contains("剩"))
    }

    @Test
    fun stage1_and_stage4_compressionCopyAndChineseMonthsAndThemeNames() {
        assertTrue(CompressionPreset.COMPACT.subtitle.contains("1920px"))
        assertEquals("一月", monthEnglishName(1))
        assertEquals("十二月", monthEnglishName(12))
        assertEquals("日光画廊", AppThemeMode.LIGHT.label)
        assertEquals("暗夜暗房", AppThemeMode.DARK.label)

        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("flipyear_theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val prefs = ThemePreferences(context)
        assertFalse(prefs.hasSeenOnboardingGuide.value)
        prefs.markOnboardingGuideSeen()
        assertTrue(prefs.hasSeenOnboardingGuide.value)
        assertTrue(ThemePreferences(context).hasSeenOnboardingGuide.value)
    }

    @Test
    fun stage2_and_stage3_revisitModeProgressesToSessionCompletionWithEntrySpecificBackLabel() {
        val p1 = samplePhoto(1L, 1_000L, status = PhotoStatus.KEPT)
        val p2 = samplePhoto(2L, 2_000L, status = PhotoStatus.KEPT)
        val queue = listOf(
            CleanupCardItem.SinglePhoto(p1),
            CleanupCardItem.SinglePhoto(p2)
        )
        var openedVaultIds by mutableStateOf<Set<Long>?>(null)

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "重温足迹 · 杭州",
                    monthNumber = 8,
                    queue = queue,
                    undoStackDepth = 0,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = {},
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { null },
                    onOpenVaultForSession = { openedVaultIds = it },
                    includeReviewed = true,
                    entrySource = CleanerEntrySource.MAP
                )
            }
        }

        composeRule.waitForIdle()
        // Keep first card via button
        composeRule.onNodeWithTag("arc_keep_button").performClick()
        composeRule.waitForIdle()
        // Trash second card via button -> should reach session completion peak
        composeRule.onNodeWithTag("arc_trash_button").performClick()
        composeRule.waitForIdle()

        // Completion screen displays session-scoped stats and Footprint-specific back label
        composeRule.onNodeWithTag("completion_session_stats")
            .assertTextContains("保留 1", substring = true)
            .assertTextContains("移入回收站 1", substring = true)
        composeRule.onNodeWithText("返回足迹", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("completion_open_vault_button").assertIsDisplayed()
        composeRule.onNodeWithTag("completion_open_vault_button").performClick()
        composeRule.waitForIdle()

        assertEquals(setOf(2L), openedVaultIds)
    }

    @Test
    fun stage2_burstKeepAllAndKeepBestExplicitButtonSemantics() {
        val b1 = samplePhoto(11L, 1_000L, burstGroupId = "g1")
        val b2 = samplePhoto(12L, 1_200L, burstGroupId = "g1")
        val burst = CleanupCardItem.BurstStack("g1", listOf(b1, b2), b1)
        var keptEntireStack = false
        var keptBestOnly = false

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年8月",
                    monthNumber = 8,
                    queue = listOf(burst),
                    undoStackDepth = 0,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = {},
                    onKeepEntireStack = { _, _ -> keptEntireStack = true },
                    onKeepBestInStack = { _, _ -> keptBestOnly = true },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { null }
                )
            }
        }

        composeRule.waitForIdle()
        // Explicit "全部保留" and "仅保留最佳" buttons exist for burst stacks
        composeRule.onNodeWithTag("keep_all_burst_button").assertIsDisplayed()
        composeRule.onNodeWithTag("keep_best_burst_button").assertIsDisplayed()
        composeRule.onNodeWithTag("keep_best_burst_button").performClick()
        composeRule.waitForIdle()
        assertTrue(keptBestOnly)
        assertFalse(keptEntireStack)
    }

    @Test
    fun galleryFilterSheet_smartViewAndAlbumUpdateFloatingApplyButtonCountLive() {
        val burstPhoto1 = samplePhoto(101L, 1_000L, burstGroupId = "b1").copy(
            bucketName = "旅行日记",
            fileSizeBytes = 5_000_000L,
            locationName = "杭州",
            latitude = 30.27,
            longitude = 120.15
        )
        val burstPhoto2 = samplePhoto(102L, 1_100L, burstGroupId = "b1").copy(
            bucketName = "旅行日记",
            fileSizeBytes = 4_500_000L,
            locationName = "杭州",
            latitude = 30.27,
            longitude = 120.15
        )
        val cameraPhoto = samplePhoto(103L, 2_000L).copy(
            bucketName = "Camera",
            fileSizeBytes = 1_500_000L,
            locationName = null,
            latitude = null,
            longitude = null
        )
        val travelSingle = samplePhoto(104L, 3_000L).copy(
            bucketName = "旅行日记",
            fileSizeBytes = 1_200_000L,
            locationName = "京都",
            latitude = 35.01,
            longitude = 135.76
        )

        val entries = listOf(
            GalleryEntry.Burst("b1", listOf(burstPhoto1, burstPhoto2), burstPhoto1),
            GalleryEntry.Single(cameraPhoto),
            GalleryEntry.Single(travelSingle)
        )

        var appliedSmartFilter by mutableStateOf(GallerySmartFilter.ALL)
        var appliedAlbum by mutableStateOf<String?>(null)

        composeRule.setContent {
            FlipYearTheme {
                TimelineSearchFilterSheet(
                    currentQuery = TimelineSearchQuery(),
                    availableYears = listOf(2025),
                    locationClusters = emptyList(),
                    hasUnlocatedMedia = true,
                    extractedKeywordTags = emptyList(),
                    previewResultCount = { draftQuery, draftSmart, draftAlbum ->
                        val base = filterGalleryEntriesBySmartAndAlbum(
                            allEntries = entries,
                            smartFilter = draftSmart,
                            selectedAlbum = draftAlbum
                        )
                        filterGalleryEntriesBySearch(base, draftQuery).size
                    },
                    onApplyQuery = {},
                    onOpenCustomDatePicker = { _, _, _ -> },
                    onDismiss = {},
                    currentSmartFilter = GallerySmartFilter.ALL,
                    currentAlbum = null,
                    existingAlbums = listOf("旅行日记", "Camera"),
                    burstCount = 1,
                    largePhotoCount = 1,
                    missingLocationCount = 1,
                    onSelectSmartFilter = { appliedSmartFilter = it },
                    onSelectAlbum = { appliedAlbum = it }
                )
            }
        }

        composeRule.waitForIdle()
        // Floating footer & apply button start with all 3 entries
        composeRule.onNodeWithTag("filter_sheet_floating_footer").assertIsDisplayed()
        composeRule.onNodeWithTag("filter_sheet_apply_button")
            .assertIsDisplayed()
            .assertTextContains("应用筛选 (3 项)", substring = true)

        // Select Smart View "连拍组" -> live count updates from 3 to 1
        composeRule.onNodeWithTag("gallery_filter_bursts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_sheet_apply_button")
            .assertTextContains("应用筛选 (1 项)", substring = true)

        // Also select album "Camera" (has no bursts) -> live count updates from 1 to 0
        composeRule.onNodeWithTag("gallery_album_chip_Camera").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_sheet_apply_button")
            .assertTextContains("应用筛选 (0 项)", substring = true)

        // Switch album to "旅行日记" (has 1 burst) -> live count updates from 0 to 1
        composeRule.onNodeWithTag("gallery_album_chip_旅行日记").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_sheet_apply_button")
            .assertTextContains("应用筛选 (1 项)", substring = true)

        // Switch Smart View back to "全部" while keeping "旅行日记" -> live count updates to 2
        composeRule.onNodeWithTag("gallery_filter_all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_sheet_apply_button")
            .assertTextContains("应用筛选 (2 项)", substring = true)

        // Tap Apply -> commits smart filter & album
        composeRule.onNodeWithTag("filter_sheet_apply_button").performClick()
        composeRule.waitForIdle()
        assertEquals(GallerySmartFilter.ALL, appliedSmartFilter)
        assertEquals("旅行日记", appliedAlbum)
    }
}
