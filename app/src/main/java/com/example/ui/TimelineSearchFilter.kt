package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.ImageFormat
import com.example.data.MediaSubtype
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.smarttag.SmartCategory
import com.example.data.smarttag.hasSmartContentFor
import com.example.data.smarttag.smartCategories
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Timezone-safe calendar date key encoded as `YYYYMMDD` (e.g., `20261018`) for zero-allocation
 * O(N) date range filtering across 10,000+ timeline photos.
 */
@Immutable
data class DateRangeFilter(
    val startDateKey: Int? = null,
    val endDateKey: Int? = null,
    val presetLabel: String? = null
) {
    val isActive: Boolean
        get() = startDateKey != null || endDateKey != null

    fun matches(year: Int, month: Int, day: Int): Boolean {
        if (!isActive) return true
        val key = encodeDateKey(year, month, day)
        val afterStart = startDateKey == null || key >= startDateKey
        val beforeEnd = endDateKey == null || key <= endDateKey
        return afterStart && beforeEnd
    }

    fun formatDisplayLabel(): String {
        if (!presetLabel.isNullOrBlank()) return presetLabel
        val startStr = startDateKey?.let(::formatDateKey)
        val endStr = endDateKey?.let(::formatDateKey)
        return when {
            startStr != null && endStr != null && startStr == endStr -> startStr
            startStr != null && endStr != null -> "$startStr - $endStr"
            startStr != null -> "$startStr 起"
            endStr != null -> "至 $endStr"
            else -> "全部日期"
        }
    }

    companion object {
        fun encodeDateKey(year: Int, month: Int, day: Int): Int =
            year * 10_000 + month.coerceIn(1, 12) * 100 + day.coerceIn(1, 31)

        fun fromUtcMillisRange(startUtcMillis: Long?, endUtcMillis: Long?): DateRangeFilter? {
            if (startUtcMillis == null && endUtcMillis == null) return null
            val startKey = startUtcMillis?.let(::utcMillisToDateKey)
            val endKey = endUtcMillis?.let(::utcMillisToDateKey)
            return if (startKey != null && endKey != null && startKey > endKey) {
                DateRangeFilter(startDateKey = endKey, endDateKey = startKey)
            } else {
                DateRangeFilter(startDateKey = startKey, endDateKey = endKey)
            }
        }

        fun utcMillisToDateKey(utcMillis: Long): Int {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            return encodeDateKey(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun dateKeyToUtcMillis(dateKey: Int): Long {
            val year = dateKey / 10_000
            val month = (dateKey % 10_000) / 100
            val day = dateKey % 100
            return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31), 0, 0, 0)
            }.timeInMillis
        }

        fun formatDateKey(dateKey: Int): String {
            val year = dateKey / 10_000
            val month = (dateKey % 10_000) / 100
            val day = dateKey % 100
            return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
        }

        fun lastDaysPreset(days: Int, label: String, nowMillis: Long = System.currentTimeMillis()): DateRangeFilter {
            val endCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val startCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -(days - 1).coerceAtLeast(0))
            }
            return DateRangeFilter(
                startDateKey = encodeDateKey(
                    startCal.get(Calendar.YEAR),
                    startCal.get(Calendar.MONTH) + 1,
                    startCal.get(Calendar.DAY_OF_MONTH)
                ),
                endDateKey = encodeDateKey(
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH) + 1,
                    endCal.get(Calendar.DAY_OF_MONTH)
                ),
                presetLabel = label
            )
        }

        fun yearPreset(year: Int): DateRangeFilter = DateRangeFilter(
            startDateKey = encodeDateKey(year, 1, 1),
            endDateKey = encodeDateKey(year, 12, 31),
            presetLabel = "${year}年"
        )
    }
}

/**
 * Structured descriptive tags derived directly from photo/video metadata, visual quality analysis,
 * and orientation/type signals.
 */
enum class DescriptiveTag(
    val label: String,
    val searchAliases: List<String>
) {
    FAVORITE("精选收藏", listOf("收藏", "精选", "喜欢", "favorite", "star")),
    VIDEO("视频", listOf("视频", "video", "mv", "mov", "mp4")),
    UHD_VIDEO("4K 视频", listOf("4k", "uhd", "超清视频")),
    LIVE_PHOTO("Live 图", listOf("live", "live图", "live photo", "动态照片", "motion", "motion photo")),
    ANIMATED_IMAGE("动图", listOf("动图", "gif", "animated", "webp")),
    BURST("连拍组", listOf("连拍", "burst", "相似")),
    SCREENSHOT("截图", listOf("截图", "截屏", "screenshot")),
    LONG_SCREENSHOT("长截图", listOf("长截图", "长图", "scrollshot")),
    SCREEN_RECORD("录屏", listOf("录屏", "screenrecord", "screen record")),
    DOCUMENT("文档 / 票据", listOf("文档", "扫描", "票据", "发票", "二维码", "document", "scan", "receipt")),
    CHAT_SAVE("聊天保存", listOf("聊天保存", "微信", "wechat", "qq", "telegram", "whatsapp")),
    CAMERA("相机原片", listOf("相机", "原片", "camera", "dcim")),
    HEIC_IMAGE("HEIC", listOf("heic", "heif")),
    RAW_IMAGE("RAW", listOf("raw", "dng", "cr2", "nef", "arw")),
    HIGH_QUALITY("高分辨率候选", listOf("高清", "佳作", "最佳", "清晰", "crisp", "hd")),
    LANDSCAPE("横屏构图", listOf("横屏", "宽幅", "全景构图", "landscape")),
    PORTRAIT("竖屏构图", listOf("竖屏", "竖构图", "portrait")),
    WARM_TONE("黄昏 / 暖调", listOf("暖调", "胶片", "黄昏", "晚霞", "落日", "日出", "film", "sunset")),
    NEEDS_REVIEW("待整理", listOf("待整理", "未整理", "unreviewed")),
    EXPOSURE_OR_TILT("瑕疵 / 曝光", listOf("过曝", "欠曝", "偏暗", "倾斜", "瑕疵", "模糊", "blur")),
    LARGE_FILE("大体积 ≥3MB", listOf("大图", "大体积", "压缩", "large"));

    fun matches(photo: PhotoItemEntity): Boolean = when (this) {
        FAVORITE -> photo.isFavorite
        VIDEO -> photo.isVideo
        UHD_VIDEO -> photo.isVideo && photo.heightPx >= 2160
        LIVE_PHOTO -> photo.isLivePhoto
        ANIMATED_IMAGE -> photo.isAnimated
        BURST -> !photo.burstGroupId.isNullOrBlank()
        SCREENSHOT -> photo.mediaSubtypeKind == MediaSubtype.SCREENSHOT
        LONG_SCREENSHOT -> photo.mediaSubtypeKind == MediaSubtype.LONG_SCREENSHOT
        SCREEN_RECORD -> photo.mediaSubtypeKind == MediaSubtype.SCREEN_RECORD
        DOCUMENT -> photo.mediaSubtypeKind == MediaSubtype.DOCUMENT ||
            photo.hasSmartContentFor(SmartCategory.DOCUMENT.key)
        CHAT_SAVE -> photo.mediaSubtypeKind == MediaSubtype.CHAT_SAVE
        CAMERA -> photo.mediaSubtypeKind == MediaSubtype.CAMERA
        HEIC_IMAGE -> photo.imageFormatKind == ImageFormat.HEIC
        RAW_IMAGE -> photo.imageFormatKind == ImageFormat.RAW
        HIGH_QUALITY -> photo.qualityScore >= 90 || photo.isBestInStack
        LANDSCAPE -> photo.widthPx > photo.heightPx
        PORTRAIT -> photo.heightPx > photo.widthPx
        WARM_TONE -> photo.visualVariant == "WARM_FILM" ||
            WARM_CAPTION_KEYWORDS.any { photo.caption.contains(it, ignoreCase = true) }
        NEEDS_REVIEW -> photo.photoStatus == PhotoStatus.UNREVIEWED
        EXPOSURE_OR_TILT -> photo.visualVariant in ISSUE_VISUAL_VARIANTS ||
            ISSUE_CAPTION_KEYWORDS.any { photo.caption.contains(it, ignoreCase = true) }
        LARGE_FILE -> !photo.isVideo && photo.fileSizeBytes >= 3_000_000L
    }

    companion object {
        private val ISSUE_VISUAL_VARIANTS = setOf("SLIGHT_TILT", "DARK_EXPOSURE", "OVEREXPOSED")
        private val WARM_CAPTION_KEYWORDS = listOf("黄昏", "晚霞", "落日", "日出", "胶片", "红叶", "暖")
        private val ISSUE_CAPTION_KEYWORDS = listOf("过曝", "欠曝", "偏暗", "倾斜", "模糊")
    }
}

@Immutable
data class TimelineSearchQuery(
    val keyword: String = "",
    val dateRange: DateRangeFilter? = null,
    val selectedLocations: Set<String> = emptySet(),
    val selectedTags: Set<DescriptiveTag> = emptySet(),
    val selectedKeywordTags: Set<String> = emptySet()
) {
    val hasStructuredFilters: Boolean
        get() = (dateRange?.isActive == true) ||
            selectedLocations.isNotEmpty() ||
            selectedTags.isNotEmpty() ||
            selectedKeywordTags.isNotEmpty()

    val isActive: Boolean
        get() = keyword.isNotBlank() || hasStructuredFilters

    val structuredFilterCount: Int
        get() = (if (dateRange?.isActive == true) 1 else 0) +
            selectedLocations.size +
            selectedTags.size +
            selectedKeywordTags.size
}

const val SEARCH_UNLOCATED_TOKEN = "__UNLOCATED__"

private val CURATED_SCENE_DESCRIPTORS = listOf(
    "红叶", "落日", "晚霞", "日出", "黄昏", "晨雾",
    "湖", "海", "雪", "山", "咖啡", "拿铁", "手冲",
    "灯笼", "悬崖", "雨", "航拍", "慢动作", "4K"
)

private val MONTH_ENGLISH_SHORT = arrayOf(
    "", "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec"
)

private fun smartContentMatchesToken(photo: PhotoItemEntity, token: String): Boolean {
    if (photo.contentLabels.isBlank()) return false
    for (cat in photo.smartCategories()) {
        if (cat.zhLabel.contains(token) || cat.searchAliases.any { it.contains(token) || token.contains(it) }) {
            return true
        }
    }
    return false
}

/**
 * Extracts real descriptive scene keywords present in the user's library (captions & albums)
 * sorted by frequency so tag chips always reflect actual photos in the timeline.
 * Smart ML content categories present in the library are surfaced with a "#"-style count too,
 * so the chips stay truthful to what the indexer actually tagged.
 */
fun extractDescriptiveKeywordTags(
    entries: List<GalleryEntry>,
    limit: Int = 10
): List<String> {
    if (entries.isEmpty()) return emptyList()
    val counts = HashMap<String, Int>(24)

    fun inspectPhoto(photo: PhotoItemEntity) {
        val cap = photo.caption
        if (cap.isNotBlank()) {
            for (word in CURATED_SCENE_DESCRIPTORS) {
                if (cap.contains(word, ignoreCase = true)) {
                    counts[word] = (counts[word] ?: 0) + 1
                }
            }
        }
        for (cat in photo.smartCategories()) {
            counts[cat.zhLabel] = (counts[cat.zhLabel] ?: 0) + 1
        }
    }

    for (entry in entries) {
        when (entry) {
            is GalleryEntry.Single -> inspectPhoto(entry.photo)
            is GalleryEntry.Burst -> {
                for (p in entry.photos) inspectPhoto(p)
            }
        }
    }
    return counts.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key }
}

/**
 * High-speed O(N) filter for [GalleryEntry] items across date range, locations, descriptive tags,
 * and multi-token free-text queries (supporting dates, locations, captions, albums, and tag aliases).
 */
fun filterGalleryEntriesBySearch(
    entries: List<GalleryEntry>,
    query: TimelineSearchQuery
): List<GalleryEntry> {
    if (!query.isActive) return entries
    if (entries.isEmpty()) return emptyList()

    val tokens = query.keyword
        .trim()
        .lowercase(Locale.ROOT)
        .split(' ', '　', ',', '，', '、')
        .filter { it.isNotBlank() }

    val dateRange = query.dateRange?.takeIf { it.isActive }
    val selectedLocs = query.selectedLocations
    val includeUnlocated = SEARCH_UNLOCATED_TOKEN in selectedLocs
    val selectedTags = query.selectedTags
    val selectedKeywordTags = query.selectedKeywordTags

    // Pre-match tokens to descriptive tags once so free-text queries like "视频" or "收藏" work naturally
    val tokenMatchedTags: List<List<DescriptiveTag>> = tokens.map { token ->
        DescriptiveTag.entries.filter { tag ->
            tag.label.lowercase(Locale.ROOT).contains(token) ||
                tag.searchAliases.any { alias -> alias.contains(token) || token.contains(alias) }
        }
    }

    fun matchesSinglePhoto(photo: PhotoItemEntity): Boolean {
        // 1. Date range check
        if (dateRange != null && !dateRange.matches(photo.year, photo.month, photo.day)) {
            return false
        }

        // 2. Location chip filter (OR across selected locations)
        if (selectedLocs.isNotEmpty()) {
            val isUnlocated = photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null
            val locMatched = (includeUnlocated && isUnlocated) ||
                (!photo.locationName.isNullOrBlank() && photo.locationName in selectedLocs)
            if (!locMatched) return false
        }

        // 3. Structured descriptive tags (AND across selected tags for precise narrowing)
        if (selectedTags.isNotEmpty()) {
            for (tag in selectedTags) {
                if (!tag.matches(photo)) return false
            }
        }

        // 4. Extracted scene keyword tags (AND)
        if (selectedKeywordTags.isNotEmpty()) {
            for (kw in selectedKeywordTags) {
                val hit = photo.caption.contains(kw, ignoreCase = true) ||
                    (photo.bucketName?.contains(kw, ignoreCase = true) == true) ||
                    photo.hasSmartContentFor(kw)
                if (!hit) return false
            }
        }

        // 5. Free-text multi-token query (every token must match at least one metadata field)
        if (tokens.isNotEmpty()) {
            for (i in tokens.indices) {
                val token = tokens[i]
                val hitLocation = photo.locationName?.lowercase(Locale.ROOT)?.contains(token) == true
                val hitCaption = photo.caption.lowercase(Locale.ROOT).contains(token)
                val hitBucket = photo.bucketName?.lowercase(Locale.ROOT)?.contains(token) == true
                val hitDate = matchesDateToken(photo.year, photo.month, photo.day, token)
                val hitTagAlias = tokenMatchedTags[i].any { it.matches(photo) }
                val hitSmartContent = smartContentMatchesToken(photo, token)
                val hitUnlocatedKeyword = (token == "无定位" || token == "缺定位") &&
                    (photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null)

                if (!hitLocation && !hitCaption && !hitBucket && !hitDate && !hitTagAlias && !hitUnlocatedKeyword && !hitSmartContent) {
                    return false
                }
            }
        }

        return true
    }

    val filtered = ArrayList<GalleryEntry>(entries.size.coerceAtMost(256))
    for (entry in entries) {
        val matched = when (entry) {
            is GalleryEntry.Single -> matchesSinglePhoto(entry.photo)
            is GalleryEntry.Burst -> entry.photos.any { matchesSinglePhoto(it) }
        }
        if (matched) {
            filtered.add(entry)
        }
    }
    return filtered
}

private fun matchesDateToken(year: Int, month: Int, day: Int, token: String): Boolean {
    val yearStr = year.toString()
    if (token == yearStr || token == "${yearStr}年") return true
    if (token == "${month}月" || token == String.format(Locale.US, "%02d月", month)) return true
    val engMonth = if (month in 1..12) MONTH_ENGLISH_SHORT[month] else ""
    if (engMonth.isNotEmpty() && token == engMonth) return true

    val ymDash = String.format(Locale.US, "%04d-%02d", year, month)
    val ymSlash = String.format(Locale.US, "%04d/%02d", year, month)
    val ymDot = String.format(Locale.US, "%04d.%02d", year, month)
    val ymCn = "${year}年${month}月"
    val ymdDash = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    val ymdSlash = String.format(Locale.US, "%04d/%02d/%02d", year, month, day)

    return ymDash.contains(token) ||
        ymSlash.contains(token) ||
        ymDot.contains(token) ||
        ymCn.contains(token) ||
        ymdDash.contains(token) ||
        ymdSlash.contains(token)
}
