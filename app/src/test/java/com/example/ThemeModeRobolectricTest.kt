package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ThemeAppearanceSheet
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DarkFlipYearPalette
import com.example.ui.theme.FlipYearTheme
import com.example.ui.theme.LightFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.resolveIsDarkTheme
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
class ThemeModeRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resolveIsDarkTheme_handlesAllSystemAndManualCombinations() {
        assertFalse(resolveIsDarkTheme(AppThemeMode.SYSTEM, isSystemInDarkTheme = false))
        assertTrue(resolveIsDarkTheme(AppThemeMode.SYSTEM, isSystemInDarkTheme = true))

        assertFalse(resolveIsDarkTheme(AppThemeMode.LIGHT, isSystemInDarkTheme = false))
        assertFalse(resolveIsDarkTheme(AppThemeMode.LIGHT, isSystemInDarkTheme = true))

        assertTrue(resolveIsDarkTheme(AppThemeMode.DARK, isSystemInDarkTheme = false))
        assertTrue(resolveIsDarkTheme(AppThemeMode.DARK, isSystemInDarkTheme = true))
    }

    @Test
    fun themePreferences_persistsAndRestoresSelectedModeSynchronously() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("flipyear_theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val prefs = ThemePreferences(context)
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode.value)

        prefs.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, prefs.themeMode.value)

        // Fresh instance simulates app cold restart
        val reloadedPrefs = ThemePreferences(context)
        assertEquals(AppThemeMode.DARK, reloadedPrefs.themeMode.value)

        reloadedPrefs.setThemeMode(AppThemeMode.LIGHT)
        val reloadedLight = ThemePreferences(context)
        assertEquals(AppThemeMode.LIGHT, reloadedLight.themeMode.value)
    }

    @Test
    fun flipYearTheme_switchesPaletteTokensReactivelyBetweenLightAndDark() {
        val modeState = mutableStateOf(AppThemeMode.LIGHT)
        var observedIsDark = false
        var observedCanvas = Color.Unspecified

        composeRule.setContent {
            FlipYearTheme(themeMode = modeState.value) {
                observedIsDark = LocalIsDarkTheme.current
                observedCanvas = StudioCanvas
            }
        }

        composeRule.waitForIdle()
        assertFalse(observedIsDark)
        assertEquals(LightFlipYearPalette.studioCanvas, observedCanvas)

        modeState.value = AppThemeMode.DARK
        composeRule.waitForIdle()
        assertTrue(observedIsDark)
        assertEquals(DarkFlipYearPalette.studioCanvas, observedCanvas)
    }

    @Test
    fun themeAppearanceSheet_invokesCallbackForAllThreeThemeOptions() {
        val selectedModes = mutableListOf<AppThemeMode>()
        val currentMode = mutableStateOf(AppThemeMode.SYSTEM)

        composeRule.setContent {
            FlipYearTheme(themeMode = currentMode.value) {
                ThemeAppearanceSheet(
                    currentMode = currentMode.value,
                    onSelectMode = { mode ->
                        selectedModes += mode
                        currentMode.value = mode
                    },
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_option_dark").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_option_light").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_option_system").performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(AppThemeMode.DARK, AppThemeMode.LIGHT, AppThemeMode.SYSTEM),
            selectedModes
        )
    }
}
