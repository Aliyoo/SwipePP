package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FlipYearApp
import com.example.ui.theme.FlipYearTheme
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.resolveIsDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferences = ThemePreferences(applicationContext)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            val userPreferences by themePreferences.userPreferences.collectAsStateWithLifecycle()
            val themeMode = userPreferences.themeMode
            val isDark = resolveIsDarkTheme(themeMode, isSystemInDarkTheme())

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            FlipYearTheme(themeMode = themeMode) {
                FlipYearApp(
                    themeMode = themeMode,
                    userPreferences = userPreferences,
                    onSelectThemeMode = themePreferences::setThemeMode,
                    onSelectTimelineSortOrder = themePreferences::setTimelineSortOrder,
                    onToggleShowEmptyMonths = themePreferences::setShowEmptyMonths,
                    onSelectFlipbookDefaultView = themePreferences::setFlipbookDefaultView,
                    onSelectDefaultStartTab = themePreferences::setDefaultStartTab,
                    onSelectGalleryGridDensity = themePreferences::setGalleryGridDensity,
                    onSelectYearbookCoverStyle = themePreferences::setYearbookCoverStyle
                )
            }
        }
    }
}
