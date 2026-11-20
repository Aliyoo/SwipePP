package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.components.FullScreenOverlayHost
import com.example.ui.components.FullScreenOverlayLayer
import com.example.ui.components.LocalFullScreenOverlayHost
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.FlipYearTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class PhotoDetailLightboxLayoutRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleMedia(isVideo: Boolean): PhotoItemEntity = PhotoItemEntity(
        id = if (isVideo) 202L else 101L,
        year = 2026,
        month = 11,
        day = 9,
        takenAtMillis = 1_794_215_940_000L,
        mediaType = if (isVideo) "VIDEO" else "IMAGE",
        durationMs = if (isVideo) 18_000L else 0L,
        drawableName = "img_photo_kyoto",
        caption = if (isVideo) "20261109_091925_VIDEO" else "20261109_091925",
        locationName = "Camera",
        fileSizeBytes = 691_200L,
        widthPx = 2252,
        heightPx = 4000,
        qualityScore = 73,
        status = PhotoStatus.UNREVIEWED.name
    )

    @Test
    fun photoLightbox_inFullScreenOverlayHost_bottomDockAndActionsAreFullyUnclipped() {
        var visible by mutableStateOf(true)
        var kept = false

        composeRule.setContent {
            val host = remember { FullScreenOverlayHost() }
            FlipYearTheme(themeMode = AppThemeMode.DARK) {
                CompositionLocalProvider(LocalFullScreenOverlayHost provides host) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("root_viewport")
                    ) {
                        if (visible) {
                            PhotoDetailLightbox(
                                photo = sampleMedia(isVideo = false),
                                onDismiss = { visible = false },
                                onKeep = { _, _ -> kept = true },
                                onTrash = {}
                            )
                        }
                        FullScreenOverlayLayer(host = host)
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag("root_viewport").getUnclippedBoundsInRoot()
        val dockBounds = composeRule.onNodeWithTag("lightbox_bottom_dock").getUnclippedBoundsInRoot()
        val trashBounds = composeRule.onNodeWithTag("lightbox_trash_button").getUnclippedBoundsInRoot()
        val favBounds = composeRule.onNodeWithTag("lightbox_favorite_button").getUnclippedBoundsInRoot()
        val keepBounds = composeRule.onNodeWithTag("lightbox_keep_button").getUnclippedBoundsInRoot()

        composeRule.onNodeWithTag("lightbox_bottom_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_trash_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_favorite_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_keep_button").assertIsDisplayed()

        // Dock bottom must stay strictly inside root viewport with >= 12dp breathing room
        assertTrue(
            "Expected bottom dock (${dockBounds.bottom}) to be >= 12dp above viewport bottom (${rootBounds.bottom})",
            dockBounds.bottom <= rootBounds.bottom - 12.dp
        )
        assertTrue(trashBounds.bottom <= dockBounds.bottom)
        assertTrue(favBounds.bottom <= dockBounds.bottom)
        assertTrue(keepBounds.bottom <= dockBounds.bottom)
        assertTrue(
            "Expected full 48dp action pill height, got ${keepBounds.bottom - keepBounds.top}",
            (keepBounds.bottom - keepBounds.top) >= 47.dp
        )

        // Clicking Keep triggers callback and unmounts overlay cleanly
        composeRule.onNodeWithTag("lightbox_keep_button").performClick()
        composeRule.waitForIdle()
        assertTrue(kept)
        composeRule.onNodeWithTag("photo_detail_lightbox").assertDoesNotExist()
    }

    @Test
    fun videoLightbox_inFullScreenOverlayHost_bottomDockAndActionsAreFullyUnclipped() {
        composeRule.mainClock.autoAdvance = false
        var visible by mutableStateOf(true)

        composeRule.setContent {
            val host = remember { FullScreenOverlayHost() }
            FlipYearTheme(themeMode = AppThemeMode.DARK) {
                CompositionLocalProvider(LocalFullScreenOverlayHost provides host) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("root_viewport")
                    ) {
                        if (visible) {
                            PhotoDetailLightbox(
                                photo = sampleMedia(isVideo = true),
                                onDismiss = { visible = false },
                                onKeep = { _, _ -> },
                                onTrash = {}
                            )
                        }
                        FullScreenOverlayLayer(host = host)
                    }
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag("root_viewport").getUnclippedBoundsInRoot()
        val dockBounds = composeRule.onNodeWithTag("lightbox_bottom_dock").getUnclippedBoundsInRoot()
        val keepBounds = composeRule.onNodeWithTag("lightbox_keep_button").getUnclippedBoundsInRoot()

        composeRule.onNodeWithTag("lightbox_bottom_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_trash_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_favorite_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_keep_button").assertIsDisplayed()

        assertTrue(
            "Expected video bottom dock (${dockBounds.bottom}) to sit >= 12dp above viewport bottom (${rootBounds.bottom})",
            dockBounds.bottom <= rootBounds.bottom - 12.dp
        )
        assertTrue(keepBounds.bottom <= dockBounds.bottom)

        composeRule.onNodeWithTag("lightbox_close_button").performClick()
        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("photo_detail_lightbox").assertDoesNotExist()
    }
}
