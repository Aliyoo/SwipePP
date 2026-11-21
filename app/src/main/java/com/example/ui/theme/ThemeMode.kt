package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(
    val storageKey: String,
    val label: String,
    val subtitle: String
) {
    SYSTEM(
        storageKey = "system",
        label = "跟随系统",
        subtitle = "自动同步设备深浅外观"
    ),
    LIGHT(
        storageKey = "light",
        label = "日光画廊",
        subtitle = "暖白石膏日光画廊质感"
    ),
    DARK(
        storageKey = "dark",
        label = "暗夜暗房",
        subtitle = "深邃曜石暗房沉浸观片"
    );

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

fun resolveIsDarkTheme(mode: AppThemeMode, isSystemInDarkTheme: Boolean): Boolean =
    when (mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

/**
 * Reads Android system Animator Duration Scale so animations (slideshow, Ken Burns drift)
 * gracefully stop when the user enables "Remove animations" / animator scale = 0.
 */
fun isReducedMotionEnabled(context: Context): Boolean {
    val scale = runCatching {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)
    return scale == 0f
}

/**
 * Lightweight synchronous SharedPreferences store for appearance & onboarding preferences
 * so MainActivity resolves dark/light mode before frame 1 with zero white flash on cold launch.
 */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromStorageKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey))
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _hasSeenOnboardingGuide = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    )
    val hasSeenOnboardingGuide: StateFlow<Boolean> = _hasSeenOnboardingGuide.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
    }

    fun markOnboardingGuideSeen() {
        if (_hasSeenOnboardingGuide.value) return
        _hasSeenOnboardingGuide.value = true
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "flipyear_theme_prefs"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_ONBOARDING_SEEN = "has_seen_onboarding_guide_v1"
    }
}
