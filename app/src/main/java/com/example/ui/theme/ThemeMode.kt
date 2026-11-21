package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.Immutable
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

enum class TimelineSortOrder(
    val storageKey: String,
    val label: String,
    val shortLabel: String,
    val previewSummary: String
) {
    NEWEST_FIRST(
        storageKey = "newest_first",
        label = "最新优先（倒序）",
        shortLabel = "最新优先",
        previewSummary = "2026 → 2025 · 11月 → 10月 · 新图在前"
    ),
    OLDEST_FIRST(
        storageKey = "oldest_first",
        label = "最早优先（正序）",
        shortLabel = "最早优先",
        previewSummary = "2022 → 2026 · 1月 → 2月 · 旧图在前"
    );

    companion object {
        fun fromStorageKey(key: String?): TimelineSortOrder =
            entries.firstOrNull { it.storageKey == key } ?: NEWEST_FIRST
    }
}

enum class DefaultStartTab(
    val storageKey: String,
    val label: String,
    val route: String
) {
    BOOKSHELF(storageKey = "bookshelf", label = "影集", route = "bookshelf"),
    GALLERY(storageKey = "gallery", label = "画廊", route = "gallery"),
    MAP(storageKey = "map", label = "足迹", route = "map");

    companion object {
        fun fromStorageKey(key: String?): DefaultStartTab =
            entries.firstOrNull { it.storageKey == key } ?: BOOKSHELF
    }
}

enum class FlipbookDefaultView(
    val storageKey: String,
    val label: String
) {
    CALENDAR(storageKey = "calendar", label = "月历网格"),
    GALLERY(storageKey = "gallery", label = "月内画廊");

    companion object {
        fun fromStorageKey(key: String?): FlipbookDefaultView =
            entries.firstOrNull { it.storageKey == key } ?: CALENDAR
    }
}

enum class GalleryGridDensity(
    val storageKey: String,
    val label: String,
    val columns: Int
) {
    STANDARD(storageKey = "standard", label = "3 列标准", columns = 3),
    COMPACT(storageKey = "compact", label = "4 列紧凑", columns = 4);

    companion object {
        fun fromStorageKey(key: String?): GalleryGridDensity =
            entries.firstOrNull { it.storageKey == key } ?: STANDARD
    }
}

@Immutable
data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val timelineSortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST,
    val defaultStartTab: DefaultStartTab = DefaultStartTab.BOOKSHELF,
    val flipbookDefaultView: FlipbookDefaultView = FlipbookDefaultView.CALENDAR,
    val showEmptyMonths: Boolean = false,
    val galleryGridDensity: GalleryGridDensity = GalleryGridDensity.STANDARD
)

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
 * Lightweight synchronous SharedPreferences store for appearance, timeline ordering,
 * default view preferences, and onboarding state so MainActivity resolves everything
 * before frame 1 with zero white flash or layout jump on cold launch.
 */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val initialThemeMode =
        AppThemeMode.fromStorageKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey))

    private val _themeMode = MutableStateFlow(initialThemeMode)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _userPreferences = MutableStateFlow(
        UserPreferences(
            themeMode = initialThemeMode,
            timelineSortOrder = TimelineSortOrder.fromStorageKey(
                prefs.getString(KEY_TIMELINE_SORT_ORDER, TimelineSortOrder.NEWEST_FIRST.storageKey)
            ),
            defaultStartTab = DefaultStartTab.fromStorageKey(
                prefs.getString(KEY_DEFAULT_START_TAB, DefaultStartTab.BOOKSHELF.storageKey)
            ),
            flipbookDefaultView = FlipbookDefaultView.fromStorageKey(
                prefs.getString(KEY_FLIPBOOK_DEFAULT_VIEW, FlipbookDefaultView.CALENDAR.storageKey)
            ),
            showEmptyMonths = prefs.getBoolean(KEY_SHOW_EMPTY_MONTHS, false),
            galleryGridDensity = GalleryGridDensity.fromStorageKey(
                prefs.getString(KEY_GALLERY_GRID_DENSITY, GalleryGridDensity.STANDARD.storageKey)
            )
        )
    )
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private val _hasSeenOnboardingGuide = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    )
    val hasSeenOnboardingGuide: StateFlow<Boolean> = _hasSeenOnboardingGuide.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        if (_themeMode.value == mode && _userPreferences.value.themeMode == mode) return
        _themeMode.value = mode
        _userPreferences.value = _userPreferences.value.copy(themeMode = mode)
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
    }

    fun setTimelineSortOrder(order: TimelineSortOrder) {
        if (_userPreferences.value.timelineSortOrder == order) return
        _userPreferences.value = _userPreferences.value.copy(timelineSortOrder = order)
        prefs.edit().putString(KEY_TIMELINE_SORT_ORDER, order.storageKey).apply()
    }

    fun setDefaultStartTab(tab: DefaultStartTab) {
        if (_userPreferences.value.defaultStartTab == tab) return
        _userPreferences.value = _userPreferences.value.copy(defaultStartTab = tab)
        prefs.edit().putString(KEY_DEFAULT_START_TAB, tab.storageKey).apply()
    }

    fun setFlipbookDefaultView(view: FlipbookDefaultView) {
        if (_userPreferences.value.flipbookDefaultView == view) return
        _userPreferences.value = _userPreferences.value.copy(flipbookDefaultView = view)
        prefs.edit().putString(KEY_FLIPBOOK_DEFAULT_VIEW, view.storageKey).apply()
    }

    fun setShowEmptyMonths(show: Boolean) {
        if (_userPreferences.value.showEmptyMonths == show) return
        _userPreferences.value = _userPreferences.value.copy(showEmptyMonths = show)
        prefs.edit().putBoolean(KEY_SHOW_EMPTY_MONTHS, show).apply()
    }

    fun setGalleryGridDensity(density: GalleryGridDensity) {
        if (_userPreferences.value.galleryGridDensity == density) return
        _userPreferences.value = _userPreferences.value.copy(galleryGridDensity = density)
        prefs.edit().putString(KEY_GALLERY_GRID_DENSITY, density.storageKey).apply()
    }

    fun markOnboardingGuideSeen() {
        if (_hasSeenOnboardingGuide.value) return
        _hasSeenOnboardingGuide.value = true
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "flipyear_theme_prefs"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_TIMELINE_SORT_ORDER = "timeline_sort_order"
        private const val KEY_DEFAULT_START_TAB = "default_start_tab"
        private const val KEY_FLIPBOOK_DEFAULT_VIEW = "flipbook_default_view"
        private const val KEY_SHOW_EMPTY_MONTHS = "show_empty_months"
        private const val KEY_GALLERY_GRID_DENSITY = "gallery_grid_density"
        private const val KEY_ONBOARDING_SEEN = "has_seen_onboarding_guide_v1"
    }
}
