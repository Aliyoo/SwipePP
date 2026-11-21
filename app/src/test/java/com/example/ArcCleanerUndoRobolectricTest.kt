package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.CleanupCardItem
import com.example.ui.screens.ArcCleanerScreen
import com.example.ui.theme.FlipYearTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ArcCleanerUndoRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun samplePhoto(
        id: Long,
        ts: Long,
        day: Int = 15,
        burstGroupId: String? = null
    ): PhotoItemEntity {
        return PhotoItemEntity(
            id = id,
            year = 2025,
            month = 6,
            day = day,
            takenAtMillis = ts,
            fileSizeBytes = 2_000_000L,
            widthPx = 4000,
            heightPx = 3000,
            drawableName = "img_photo_kyoto",
            status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = burstGroupId,
            caption = "Photo $id",
            bucketName = "Camera"
        )
    }

    private fun swipeCardUpToDelete(tag: String) {
        composeRule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(0f, -100f))
            moveBy(Offset(0f, -150f))
            moveBy(Offset(0f, -180f))
            up()
        }
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()
    }

    @Test
    fun swipeUpDeleteThenUndo_firstItem_doesNotCrashAndRestoresCard() {
        val p1 = samplePhoto(1L, 1_000L)
        val p2 = samplePhoto(2L, 2_000L)
        val p3 = samplePhoto(3L, 3_000L)
        val initial = listOf(
            CleanupCardItem.SinglePhoto(p1),
            CleanupCardItem.SinglePhoto(p2),
            CleanupCardItem.SinglePhoto(p3)
        )

        var queue by mutableStateOf<List<CleanupCardItem>>(initial)
        val deletedHistory = mutableListOf<Pair<Int, CleanupCardItem>>()
        var undoDepth by mutableStateOf(0)

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年6月",
                    monthNumber = 6,
                    queue = queue,
                    undoStackDepth = undoDepth,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = { photo ->
                        val idx = queue.indexOfFirst { it.primaryPhoto.id == photo.id }
                        if (idx >= 0) {
                            deletedHistory.add(idx to queue[idx])
                            queue = queue.filterNot { it.primaryPhoto.id == photo.id }
                            undoDepth = deletedHistory.size
                        }
                    },
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { _ ->
                        if (deletedHistory.isEmpty()) {
                            null
                        } else {
                            val (idx, item) = deletedHistory.removeAt(deletedHistory.lastIndex)
                            val mutable = queue.toMutableList()
                            mutable.add(idx.coerceIn(0, mutable.size), item)
                            queue = mutable
                            undoDepth = deletedHistory.size
                            item.stableKey
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()
        swipeCardUpToDelete("arc_card_single_1")
        assertEquals(2, queue.size)

        // Tap Undo immediately after swipe-up delete
        composeRule.onNodeWithTag("cleaner_undo_button").performClick()
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        assertEquals(3, queue.size)

        // Swipe up again on the restored card to verify dragOffsetY was reset cleanly & visible
        swipeCardUpToDelete("arc_card_single_1")
        assertEquals(2, queue.size)
    }

    @Test
    fun swipeUpDeleteLastOnlyItem_thenUndo_doesNotCrash() {
        val p1 = samplePhoto(10L, 1_000L)
        var queue by mutableStateOf<List<CleanupCardItem>>(listOf(CleanupCardItem.SinglePhoto(p1)))
        val deletedHistory = mutableListOf<CleanupCardItem>()
        var undoDepth by mutableStateOf(0)

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年6月",
                    monthNumber = 6,
                    queue = queue,
                    undoStackDepth = undoDepth,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = { photo ->
                        val item = queue.firstOrNull { it.primaryPhoto.id == photo.id } ?: return@ArcCleanerScreen
                        deletedHistory.add(item)
                        queue = emptyList()
                        undoDepth = deletedHistory.size
                    },
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { _ ->
                        if (deletedHistory.isEmpty()) null else {
                            val restored = deletedHistory.removeAt(deletedHistory.lastIndex)
                            queue = listOf(restored)
                            undoDepth = deletedHistory.size
                            restored.stableKey
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()
        swipeCardUpToDelete("arc_card_single_10")
        assertEquals(0, queue.size)

        composeRule.onNodeWithTag("cleaner_undo_button").performClick()
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        assertEquals(1, queue.size)
    }

    @Test
    fun swipeUpDeleteLastPageInMultiItemQueue_thenUndo_doesNotCrash() {
        val p1 = samplePhoto(21L, 1_000L)
        val p2 = samplePhoto(22L, 2_000L)
        var queue by mutableStateOf<List<CleanupCardItem>>(
            listOf(CleanupCardItem.SinglePhoto(p1), CleanupCardItem.SinglePhoto(p2))
        )
        val deletedHistory = mutableListOf<Pair<Int, CleanupCardItem>>()
        var undoDepth by mutableStateOf(0)

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年6月",
                    monthNumber = 6,
                    queue = queue,
                    undoStackDepth = undoDepth,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = { photo ->
                        val idx = queue.indexOfFirst { it.primaryPhoto.id == photo.id }
                        if (idx >= 0) {
                            deletedHistory.add(idx to queue[idx])
                            queue = queue.filterNot { it.primaryPhoto.id == photo.id }
                            undoDepth = deletedHistory.size
                        }
                    },
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { _ ->
                        if (deletedHistory.isEmpty()) null else {
                            val (idx, item) = deletedHistory.removeAt(deletedHistory.lastIndex)
                            val mutable = queue.toMutableList()
                            mutable.add(idx.coerceIn(0, mutable.size), item)
                            queue = mutable
                            undoDepth = deletedHistory.size
                            item.stableKey
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // Scroll horizontally to last page (p2)
        composeRule.onNodeWithTag("arc_timeline_pager").performTouchInput {
            down(centerRight - Offset(20f, 0f))
            moveBy(Offset(-600f, 0f))
            up()
        }
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()

        // Delete last page
        swipeCardUpToDelete("arc_card_single_22")
        assertEquals(1, queue.size)

        // Undo
        composeRule.onNodeWithTag("cleaner_undo_button").performClick()
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        assertEquals(2, queue.size)
    }

    @Test
    fun deleteBurstAndUndo_doesNotCrash() {
        val b1 = samplePhoto(101L, 1_000L, burstGroupId = "g1")
        val b2 = samplePhoto(102L, 1_100L, burstGroupId = "g1")
        val p3 = samplePhoto(103L, 2_000L)
        val burstItem = CleanupCardItem.BurstStack("g1", listOf(b1, b2), b1)

        var queue by mutableStateOf<List<CleanupCardItem>>(listOf(burstItem, CleanupCardItem.SinglePhoto(p3)))
        var undoDepth by mutableStateOf(0)

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年6月",
                    monthNumber = 6,
                    queue = queue,
                    undoStackDepth = undoDepth,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = {},
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {
                        queue = listOf(CleanupCardItem.SinglePhoto(p3))
                        undoDepth = 1
                    },
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { _ ->
                        queue = listOf(burstItem, CleanupCardItem.SinglePhoto(p3))
                        undoDepth = 0
                        burstItem.stableKey
                    }
                )
            }
        }

        composeRule.waitForIdle()
        swipeCardUpToDelete("arc_card_burst_g1")
        assertEquals(1, queue.size)

        composeRule.onNodeWithTag("cleaner_undo_button").performClick()
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        assertEquals(2, queue.size)
    }

    @Test
    fun initialFocusDay_landsDirectlyOnTappedDayAndTogglesDayFilter() {
        val d3 = samplePhoto(id = 301L, ts = 1_000L, day = 3)
        val d9 = samplePhoto(id = 302L, ts = 2_000L, day = 9)
        val d15a = samplePhoto(id = 303L, ts = 3_000L, day = 15)
        val d15b = samplePhoto(id = 304L, ts = 4_000L, day = 15)
        val d22 = samplePhoto(id = 305L, ts = 5_000L, day = 22)

        val queue = listOf(
            CleanupCardItem.SinglePhoto(d3),
            CleanupCardItem.SinglePhoto(d9),
            CleanupCardItem.SinglePhoto(d15a),
            CleanupCardItem.SinglePhoto(d15b),
            CleanupCardItem.SinglePhoto(d22)
        )

        composeRule.setContent {
            FlipYearTheme {
                ArcCleanerScreen(
                    title = "整理 2025年6月",
                    monthNumber = 6,
                    initialFocusDay = 15,
                    queue = queue,
                    undoStackDepth = 0,
                    onBack = {},
                    onKeepPhoto = { _, _ -> },
                    onTrashPhoto = {},
                    onKeepEntireStack = { _, _ -> },
                    onKeepBestInStack = { _, _ -> },
                    onTrashEntireStack = {},
                    onSelectBestInStack = { _, _ -> },
                    onUndoSession = { _ -> null }
                )
            }
        }

        composeRule.waitForIdle()
        // Must land directly on day 15 (3rd card of 5), NOT index 0 (day 3)
        composeRule.onNodeWithTag("cleaner_active_date_pill")
            .assertTextContains("6月15日 · 3/5", substring = true)

        // Tap day filter pill to isolate only day 15 (2 items)
        composeRule.onNodeWithTag("cleaner_day_filter_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cleaner_active_date_pill")
            .assertTextContains("6月15日 · 1/2", substring = true)

        // Tap again to return to full-month timeline while staying on day 15
        composeRule.onNodeWithTag("cleaner_day_filter_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cleaner_active_date_pill")
            .assertTextContains("6月15日 · 3/5", substring = true)
    }
}
