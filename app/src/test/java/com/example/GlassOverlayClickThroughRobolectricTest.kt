package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.theme.FlipYearTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class GlassOverlayClickThroughRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun frostedGlassDockGap_doesNotPassClickThroughToUnderlyingContent() {
        var behindClicks = 0
        var leftClicks = 0
        var rightClicks = 0

        composeRule.setContent {
            FlipYearTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Underlying clickable content (e.g., photo grid cell behind floating dock)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { behindClicks++ }
                            .testTag("underlying_canvas")
                    )

                    // Floating dock mirroring FloatingGalleryDock layout with wide center gap
                    FrostedGlassCard(
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(340.dp)
                            .height(60.dp)
                            .testTag("glass_dock")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { leftClicks++ }
                                    .testTag("dock_btn_left")
                            ) { Text("A") }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { rightClicks++ }
                                    .testTag("dock_btn_right")
                            ) { Text("B") }
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // Tap dead-center of the glass dock (the empty gap between left & right buttons)
        composeRule.onNodeWithTag("glass_dock").performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()

        assertEquals("Tap in gap between dock buttons must NOT penetrate underneath", 0, behindClicks)
        assertEquals(0, leftClicks)
        assertEquals(0, rightClicks)

        // Tapping the actual buttons must still work
        composeRule.onNodeWithTag("dock_btn_left").performTouchInput { click(center) }
        composeRule.onNodeWithTag("dock_btn_right").performTouchInput { click(center) }
        composeRule.waitForIdle()

        assertEquals(0, behindClicks)
        assertEquals(1, leftClicks)
        assertEquals(1, rightClicks)

        // Tapping outside the floating dock must still reach the underlying content
        composeRule.onNodeWithTag("underlying_canvas").performTouchInput {
            click(topCenter)
        }
        composeRule.waitForIdle()

        assertEquals(1, behindClicks)
    }

    @Test
    fun obsidianGlassDockGap_doesNotPassClickThroughToUnderlyingContent() {
        var behindClicks = 0
        var leftClicks = 0

        composeRule.setContent {
            FlipYearTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { behindClicks++ }
                            .testTag("underlying_canvas")
                    )

                    ObsidianGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(340.dp)
                            .height(64.dp)
                            .testTag("obsidian_dock")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { leftClicks++ }
                                    .testTag("obsidian_btn_left")
                            ) { Text("A") }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { }
                                    .testTag("obsidian_btn_right")
                            ) { Text("B") }
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // Tap center gap between the two buttons on ObsidianGlassCard
        composeRule.onNodeWithTag("obsidian_dock").performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()

        assertEquals("Tap in ObsidianGlassCard gap must NOT penetrate underneath", 0, behindClicks)
        assertEquals(0, leftClicks)
    }

    @Test
    fun nestedObsidianGlassCardInsideClickableParent_stillTriggersParentClick() {
        var parentClicks = 0

        composeRule.setContent {
            FlipYearTheme {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clickable { parentClicks++ }
                        .testTag("clickable_book_cover"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ObsidianGlassCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("nested_progress_card")
                    ) {
                        Text("2025")
                    }
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nested_progress_card", useUnmergedTree = true).performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()

        assertEquals("Nested glass card inside clickable book cover must still bubble click to parent", 1, parentClicks)
    }

    @Test
    fun floatingRowGapBetweenSeparateButtons_doesNotPassClickThrough() {
        var behindClicks = 0
        var leftClicks = 0
        var rightClicks = 0

        composeRule.setContent {
            FlipYearTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { behindClicks++ }
                            .testTag("underlying_grid")
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(340.dp)
                            .height(52.dp)
                            .pointerInput(Unit) {}
                            .testTag("floating_batch_strip"),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ObsidianGlassCard(
                            onClick = { leftClicks++ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("strip_left_btn")
                        ) { Text("Compress") }

                        ObsidianGlassCard(
                            onClick = { rightClicks++ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("strip_right_btn")
                        ) { Text("Location") }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // Tap exact center of the Row, which lands squarely in the 16.dp gap between the two buttons
        composeRule.onNodeWithTag("floating_batch_strip").performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()

        assertEquals("Tap in 16.dp gap between paired floating buttons must NOT penetrate underneath", 0, behindClicks)
        assertEquals(0, leftClicks)
        assertEquals(0, rightClicks)

        composeRule.onNodeWithTag("strip_left_btn").performTouchInput { click(center) }
        composeRule.onNodeWithTag("strip_right_btn").performTouchInput { click(center) }
        composeRule.waitForIdle()

        assertEquals(0, behindClicks)
        assertEquals(1, leftClicks)
        assertEquals(1, rightClicks)
    }
}
