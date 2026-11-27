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
 * smart scene classification, and orientation/type signals.
 *
 * `searchAliases` are matched strictly (exact token or explicit Chinese compound peeling) rather
 * than via unbounded substring checks, preventing single letters or unrelated nouns from matching.
 */
enum class DescriptiveTag(
    val label: String,
    val searchAliases: List<String>
) {
    FAVORITE("精选收藏", listOf("收藏", "精选", "精选收藏", "喜欢", "favorite", "star")),
    VIDEO("视频", listOf("视频", "video", "mov", "mp4")),
    UHD_VIDEO("4K 视频", listOf("4k", "4k视频", "uhd", "超清视频")),
    LIVE_PHOTO("Live 图", listOf("live", "live图", "live photo", "实况", "实况照片", "动态照片", "motion photo")),
    ANIMATED_IMAGE("动图", listOf("动图", "gif", "animated", "webp")),
    BURST("连拍组", listOf("连拍", "连拍组", "burst")),
    SCREENSHOT("截图", listOf("截图", "截屏", "screenshot")),
    LONG_SCREENSHOT("长截图", listOf("长截图", "长图", "scrollshot")),
    SCREEN_RECORD("录屏", listOf("录屏", "screenrecord", "screen record")),
    DOCUMENT("文档 / 票据", listOf("文档", "扫描", "票据", "发票", "二维码", "document", "scan", "receipt")),
    CHAT_SAVE("聊天保存", listOf("聊天保存", "微信", "wechat", "qq", "telegram", "whatsapp")),
    // Deliberately excludes bare "相机" so searching "相机" finds photos OF cameras via CLIP
    // instead of dumping 80% of DCIM/Camera photos.
    CAMERA("相机原片", listOf("相机原片", "原片", "dcim", "camera原片")),
    HEIC_IMAGE("HEIC", listOf("heic", "heif")),
    RAW_IMAGE("RAW", listOf("raw", "dng", "cr2", "nef", "arw")),
    HIGH_QUALITY("高分辨率候选", listOf("高清", "佳作", "最佳", "高分辨率", "hd")),
    LANDSCAPE("横屏构图", listOf("横屏", "横构图", "横屏构图", "宽幅", "landscape")),
    PORTRAIT("竖屏构图", listOf("竖屏", "竖构图", "竖屏构图", "portrait")),
    WARM_TONE("黄昏 / 暖调", listOf("暖调", "胶片", "黄昏", "晚霞", "落日", "日出", "sunset")),
    NEEDS_REVIEW("待整理", listOf("待整理", "未整理", "unreviewed")),
    EXPOSURE_OR_TILT("瑕疵 / 曝光", listOf("过曝", "欠曝", "偏暗", "倾斜", "瑕疵", "模糊", "blur")),
    LARGE_FILE("大体积 ≥3MB", listOf("大图", "大体积", "large"));

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
            photo.hasSmartContentFor(SmartCategory.SUNSET.key) ||
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

        // Longest-first lookup table for exact match & safe Chinese compound peeling
        // (e.g. "高清视频" -> [HIGH_QUALITY, VIDEO] with AND semantics).
        internal val exactAliasLookup: Map<String, DescriptiveTag> = buildMap {
            for (tag in DescriptiveTag.entries) {
                put(tag.label.lowercase(Locale.ROOT), tag)
                for (alias in tag.searchAliases) {
                    putIfAbsent(alias.lowercase(Locale.ROOT), tag)
                }
            }
        }

        // Only unambiguous >= 2-char Chinese/ASCII structural qualifiers participate in
        // compound splitting (e.g., "高清视频", "收藏截图", "微信截图").
        internal val compoundPeelAliases: List<Pair<String, DescriptiveTag>> = listOf(
            "4k视频" to UHD_VIDEO,
            "超清视频" to UHD_VIDEO,
            "长截图" to LONG_SCREENSHOT,
            "相机原片" to CAMERA,
            "聊天保存" to CHAT_SAVE,
            "live图" to LIVE_PHOTO,
            "实况照片" to LIVE_PHOTO,
            "动态照片" to LIVE_PHOTO,
            "待整理" to NEEDS_REVIEW,
            "未整理" to NEEDS_REVIEW,
            "连拍组" to BURST,
            "精选" to FAVORITE,
            "收藏" to FAVORITE,
            "高清" to HIGH_QUALITY,
            "视频" to VIDEO,
            "截图" to SCREENSHOT,
            "截屏" to SCREENSHOT,
            "录屏" to SCREEN_RECORD,
            "动图" to ANIMATED_IMAGE,
            "实况" to LIVE_PHOTO,
            "连拍" to BURST,
            "微信" to CHAT_SAVE,
            "横屏" to LANDSCAPE,
            "竖屏" to PORTRAIT,
            "大图" to LARGE_FILE,
            "4k" to UHD_VIDEO
        ).sortedByDescending { it.first.length }
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

private val CHINESE_MONTH_MAP = mapOf(
    "一月" to 1, "二月" to 2, "三月" to 3, "四月" to 4,
    "五月" to 5, "六月" to 6, "七月" to 7, "八月" to 8,
    "九月" to 9, "十月" to 10, "十一月" to 11, "十二月" to 12
)

/**
 * Exact, structured date constraint parsed from a free-text query token.
 */
data class ParsedDateConstraint(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
) {
    fun matches(y: Int, m: Int, d: Int): Boolean {
        if (year != null && y != year) return false
        if (month != null && m != month) return false
        if (day != null && d != day) return false
        return true
    }
}

/**
 * Structured decomposition of a user's free-text keyword into hard AND constraints (dates,
 * descriptive attributes, unlocated, and matched library locations/albums) plus the clean
 * visual residual query to send to Chinese-CLIP semantic search.
 */
data class ParsedSearchKeyword(
    val dateConstraints: List<ParsedDateConstraint> = emptyList(),
    val tagConstraints: List<DescriptiveTag> = emptyList(),
    val requireUnlocated: Boolean = false,
    val locationOrAlbumTokens: List<String> = emptyList(),
    val visualTokens: List<String> = emptyList()
) {
    /**
     * Clean visual phrase for CLIP text encoding, stripped of dates, attribute qualifiers,
     * and library-matched location names so embeddings stay focused on visual semantics.
     */
    val semanticQuery: String
        get() = visualTokens.joinToString(" ").trim()
}

private val FULL_YMD_REGEX = Regex("""^(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?$""")
private val FULL_YM_REGEX = Regex("""^(\d{4})[-/.年](\d{1,2})月?$""")
private val FULL_MD_REGEX = Regex("""^(\d{1,2})[-/.月](\d{1,2})日?$""")
private val FULL_YEAR_REGEX = Regex("""^((?:19|20)\d{2})年?$""")
private val FULL_MONTH_REGEX = Regex("""^(\d{1,2})月$""")
private val LEADING_YEAR_PREFIX_REGEX = Regex("""^((?:19|20)\d{2})年?(.+)$""")
private val LEADING_YM_PREFIX_REGEX = Regex("""^((?:19|20)\d{2})[-/.年](\d{1,2})月(.+)$""")

internal fun parseDateToken(
    token: String,
    nowMillis: Long = System.currentTimeMillis()
): ParsedDateConstraint? {
    val t = token.trim().lowercase(Locale.ROOT)
    if (t.isEmpty()) return null

    // Relative Chinese dates
    when (t) {
        "今天", "今日" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return ParsedDateConstraint(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
        "昨天", "昨日" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.DAY_OF_YEAR, -1) }
            return ParsedDateConstraint(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
        "前天" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.DAY_OF_YEAR, -2) }
            return ParsedDateConstraint(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
        "本月", "这个月" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return ParsedDateConstraint(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, null)
        }
        "上月", "上个月" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.MONTH, -1) }
            return ParsedDateConstraint(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, null)
        }
        "今年" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return ParsedDateConstraint(c.get(Calendar.YEAR), null, null)
        }
        "去年" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return ParsedDateConstraint(c.get(Calendar.YEAR) - 1, null, null)
        }
        "前年" -> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return ParsedDateConstraint(c.get(Calendar.YEAR) - 2, null, null)
        }
    }

    CHINESE_MONTH_MAP[t]?.let { m ->
        return ParsedDateConstraint(year = null, month = m, day = null)
    }

    val engMonthIdx = MONTH_ENGLISH_SHORT.indexOf(t)
    if (engMonthIdx in 1..12) {
        return ParsedDateConstraint(year = null, month = engMonthIdx, day = null)
    }

    FULL_YMD_REGEX.matchEntire(t)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val mo = m.groupValues[2].toIntOrNull()
        val d = m.groupValues[3].toIntOrNull()
        if (y != null && mo in 1..12 && d in 1..31) {
            return ParsedDateConstraint(y, mo, d)
        }
    }

    FULL_YM_REGEX.matchEntire(t)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val mo = m.groupValues[2].toIntOrNull()
        if (y != null && mo in 1..12) {
            return ParsedDateConstraint(y, mo, null)
        }
    }

    FULL_MD_REGEX.matchEntire(t)?.let { m ->
        val mo = m.groupValues[1].toIntOrNull()
        val d = m.groupValues[2].toIntOrNull()
        if (mo in 1..12 && d in 1..31) {
            return ParsedDateConstraint(null, mo, d)
        }
    }

    FULL_YEAR_REGEX.matchEntire(t)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        if (y != null) return ParsedDateConstraint(y, null, null)
    }

    FULL_MONTH_REGEX.matchEntire(t)?.let { m ->
        val mo = m.groupValues[1].toIntOrNull()
        if (mo in 1..12) return ParsedDateConstraint(null, mo, null)
    }

    return null
}

/**
 * Peels leading/trailing structural [DescriptiveTag] qualifiers off a compound Chinese token
 * (e.g. `"高清视频"` -> `([HIGH_QUALITY, VIDEO], "")`, `"收藏咖啡"` -> `([FAVORITE], "咖啡")`).
 */
private fun peelCompoundDescriptiveTags(token: String): Pair<List<DescriptiveTag>, String> {
    DescriptiveTag.exactAliasLookup[token]?.let { return listOf(it) to "" }

    var remaining = token
    val found = mutableListOf<DescriptiveTag>()
    var progressed = true
    while (progressed && remaining.isNotEmpty()) {
        progressed = false
        for ((alias, tag) in DescriptiveTag.compoundPeelAliases) {
            if (remaining.length <= alias.length) continue
            when {
                remaining.startsWith(alias) -> {
                    if (tag !in found) found.add(tag)
                    remaining = remaining.removePrefix(alias)
                    progressed = true
                    break
                }
                remaining.endsWith(alias) -> {
                    if (tag !in found) found.add(tag)
                    remaining = remaining.removeSuffix(alias)
                    progressed = true
                    break
                }
            }
        }
        if (remaining.isNotEmpty()) {
            DescriptiveTag.exactAliasLookup[remaining]?.let { tailTag ->
                if (tailTag !in found) found.add(tailTag)
                remaining = ""
                progressed = false
            }
        }
    }
    return found to remaining
}

/**
 * Decomposes [rawKeyword] into structured AND qualifiers (dates, descriptive tags, unlocated,
 * library-matched locations/albums) plus residual visual tokens for Chinese-CLIP semantic search.
 */
fun parseTimelineKeyword(
    rawKeyword: String,
    knownLocations: Collection<String> = emptyList(),
    knownAlbums: Collection<String> = emptyList(),
    nowMillis: Long = System.currentTimeMillis()
): ParsedSearchKeyword {
    val rawTokens = rawKeyword
        .trim()
        .lowercase(Locale.ROOT)
        .split(' ', '　', ',', '，', '、')
        .filter { it.isNotBlank() }
    if (rawTokens.isEmpty()) return ParsedSearchKeyword()

    val lowerLocations = knownLocations.map { it.lowercase(Locale.ROOT) }
    val lowerAlbums = knownAlbums.map { it.lowercase(Locale.ROOT) }

    val dates = mutableListOf<ParsedDateConstraint>()
    val tags = mutableListOf<DescriptiveTag>()
    var requireUnlocated = false
    val locationOrAlbum = mutableListOf<String>()
    val visual = mutableListOf<String>()

    fun matchesKnownLocationOrAlbum(token: String): Boolean {
        if (token.length < 2) return false
        return lowerLocations.any { it.contains(token) } || lowerAlbums.any { it.contains(token) }
    }

    for (rawToken in rawTokens) {
        var token = rawToken

        if (token == "无定位" || token == "缺定位") {
            requireUnlocated = true
            continue
        }

        // Exact date token
        parseDateToken(token, nowMillis)?.let { dc ->
            dates.add(dc)
            continue
        }

        // Leading year/year-month prefix glued to Chinese text without space (e.g. "2025雪山", "2026年5月咖啡")
        val ymPrefix = LEADING_YM_PREFIX_REGEX.matchEntire(token)
        if (ymPrefix != null) {
            val y = ymPrefix.groupValues[1].toIntOrNull()
            val m = ymPrefix.groupValues[2].toIntOrNull()
            val rest = ymPrefix.groupValues[3]
            if (y != null && m in 1..12 && rest.isNotBlank()) {
                dates.add(ParsedDateConstraint(y, m, null))
                token = rest
            }
        } else {
            val yPrefix = LEADING_YEAR_PREFIX_REGEX.matchEntire(token)
            if (yPrefix != null) {
                val y = yPrefix.groupValues[1].toIntOrNull()
                val rest = yPrefix.groupValues[2]
                if (y != null && rest.isNotBlank()) {
                    dates.add(ParsedDateConstraint(y, null, null))
                    token = rest
                }
            }
        }

        // Exact or compound descriptive tag peeling (e.g. "视频", "收藏", "高清视频", "收藏雪山")
        val (peeledTags, residual) = peelCompoundDescriptiveTags(token)
        if (peeledTags.isNotEmpty()) {
            for (t in peeledTags) {
                if (t !in tags) tags.add(t)
            }
            if (residual.isBlank()) continue
            token = residual
        }

        // If this token matches a real location or album in the library, promote it to a strict
        // location/album AND constraint so multi-token queries like "京都 咖啡" cannot be bypassed
        // by semantic hits from other cities.
        if (matchesKnownLocationOrAlbum(token)) {
            locationOrAlbum.add(token)
            continue
        }

        visual.add(token)
    }

    return ParsedSearchKeyword(
        dateConstraints = dates,
        tagConstraints = tags,
        requireUnlocated = requireUnlocated,
        locationOrAlbumTokens = locationOrAlbum,
        visualTokens = visual
    )
}

/**
 * Exact category-level smart content matching. Rejects 1-char tokens and partial substrings so
 * searching "上海" never hits WATER, "物" never hits 7 categories, and concrete subjects like
 * "猫咪" or "飞机" are handled by fine-grained debiased semantic search rather than coarse buckets.
 */
private fun smartContentMatchesToken(photo: PhotoItemEntity, token: String): Boolean {
    if (photo.contentLabels.isBlank() || token.length < 2) return false
    for (cat in photo.smartCategories()) {
        if (cat.zhLabel == token ||
            cat.exactAliases.any { it == token } ||
            cat.searchAliases.any { it == token }
        ) {
            return true
        }
    }
    return false
}

/**
 * Extracts real descriptive scene keywords present in the user's library (captions & smart
 * categories) sorted by frequency so tag chips always reflect actual photos in the timeline.
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
 * Computes how many entries in [entries] match each [DescriptiveTag], so the filter drawer can
 * hide tags that have zero matches in the user's real library (e.g. demo-only visual variants).
 */
fun countDescriptiveTagMatches(entries: List<GalleryEntry>): Map<DescriptiveTag, Int> {
    if (entries.isEmpty()) return emptyMap()
    val counts = HashMap<DescriptiveTag, Int>(DescriptiveTag.entries.size)
    for (tag in DescriptiveTag.entries) {
        var c = 0
        for (entry in entries) {
            val matched = when (entry) {
                is GalleryEntry.Single -> tag.matches(entry.photo)
                is GalleryEntry.Burst -> entry.photos.any { tag.matches(it) }
            }
            if (matched) c++
        }
        if (c > 0) counts[tag] = c
    }
    return counts
}

/**
 * High-speed O(N) filter & relevance ranker for [GalleryEntry] items across date range, locations,
 * descriptive tags, structured keyword decomposition, and debiased semantic search scores.
 *
 * When [query.keyword] is active:
 * - Hard qualifiers (dates, descriptive tags, unlocated, library-matched locations/albums) are
 *   always enforced with strict AND semantics and can NEVER be bypassed by semantic search.
 * - Results are ranked by composite relevance (lexical precision + calibrated semantic lift),
 *   tie-broken by recency.
 * - Matching [GalleryEntry.Burst] groups surface their highest-scoring matching photo as
 *   [GalleryEntry.Burst.bestPhoto] so the grid tile shows the actual matched image.
 */
fun filterGalleryEntriesBySearch(
    entries: List<GalleryEntry>,
    query: TimelineSearchQuery,
    semanticScores: Map<Long, Float> = emptyMap(),
    nowMillis: Long = System.currentTimeMillis()
): List<GalleryEntry> {
    if (!query.isActive) return entries
    if (entries.isEmpty()) return emptyList()

    val dateRange = query.dateRange?.takeIf { it.isActive }
    val selectedLocs = query.selectedLocations
    val includeUnlocated = SEARCH_UNLOCATED_TOKEN in selectedLocs
    val selectedTags = query.selectedTags
    val selectedKeywordTags = query.selectedKeywordTags

    val hasKeyword = query.keyword.isNotBlank()
    val parsed = if (hasKeyword) {
        val knownLocs = HashSet<String>(32)
        val knownAlbums = HashSet<String>(32)
        for (entry in entries) {
            when (entry) {
                is GalleryEntry.Single -> {
                    entry.photo.locationName?.takeIf { it.isNotBlank() }?.let { knownLocs.add(it) }
                    entry.photo.bucketName?.takeIf { it.isNotBlank() }?.let { knownAlbums.add(it) }
                }
                is GalleryEntry.Burst -> {
                    for (p in entry.photos) {
                        p.locationName?.takeIf { it.isNotBlank() }?.let { knownLocs.add(it) }
                        p.bucketName?.takeIf { it.isNotBlank() }?.let { knownAlbums.add(it) }
                    }
                }
            }
        }
        parseTimelineKeyword(
            rawKeyword = query.keyword,
            knownLocations = knownLocs,
            knownAlbums = knownAlbums,
            nowMillis = nowMillis
        )
    } else {
        null
    }

    /**
     * Returns null if [photo] fails any constraint, or a non-negative relevance score if it passes.
     */
    fun scoreSinglePhoto(photo: PhotoItemEntity): Float? {
        // 1. Structured Date range check
        if (dateRange != null && !dateRange.matches(photo.year, photo.month, photo.day)) {
            return null
        }

        // 2. Structured Location chip filter (OR across selected locations)
        if (selectedLocs.isNotEmpty()) {
            val isUnlocated = photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null
            val locMatched = (includeUnlocated && isUnlocated) ||
                (!photo.locationName.isNullOrBlank() && photo.locationName in selectedLocs)
            if (!locMatched) return null
        }

        // 3. Structured descriptive tags (AND)
        if (selectedTags.isNotEmpty()) {
            for (tag in selectedTags) {
                if (!tag.matches(photo)) return null
            }
        }

        // 4. Extracted scene keyword chips (AND)
        if (selectedKeywordTags.isNotEmpty()) {
            for (kw in selectedKeywordTags) {
                val hit = photo.caption.contains(kw, ignoreCase = true) ||
                    (photo.bucketName?.contains(kw, ignoreCase = true) == true) ||
                    photo.hasSmartContentFor(kw)
                if (!hit) return null
            }
        }

        if (parsed == null) return 0f

        // 5. Hard free-text qualifiers (strict AND, NEVER bypassed by semantic scores)
        if (parsed.requireUnlocated) {
            val isUnlocated = photo.locationName.isNullOrBlank() || photo.latitude == null || photo.longitude == null
            if (!isUnlocated) return null
        }
        for (dc in parsed.dateConstraints) {
            if (!dc.matches(photo.year, photo.month, photo.day)) return null
        }
        for (tag in parsed.tagConstraints) {
            if (!tag.matches(photo)) return null
        }

        var relevance = 0f
        val locLower = photo.locationName?.lowercase(Locale.ROOT)
        val capLower = photo.caption.lowercase(Locale.ROOT)
        val bucketLower = photo.bucketName?.lowercase(Locale.ROOT)

        for (locToken in parsed.locationOrAlbumTokens) {
            val hitLoc = locLower?.contains(locToken) == true
            val hitBucket = bucketLower?.contains(locToken) == true
            val hitCap = capLower.contains(locToken)
            if (!hitLoc && !hitBucket && !hitCap) return null
            relevance += when {
                hitLoc -> 1.0f
                hitBucket -> 0.85f
                else -> 0.6f
            }
        }

        // 6. Residual visual tokens: satisfied either by all tokens matching lexically OR by
        // calibrated Chinese-CLIP semantic lift.
        if (parsed.visualTokens.isNotEmpty()) {
            var lexicalMatchedCount = 0
            var lexicalBonus = 0f
            for (token in parsed.visualTokens) {
                val hitLoc = locLower?.contains(token) == true
                val hitCap = capLower.contains(token)
                val hitBucket = bucketLower?.contains(token) == true
                val hitSmartCat = smartContentMatchesToken(photo, token)
                if (hitLoc || hitCap || hitBucket || hitSmartCat) {
                    lexicalMatchedCount++
                    lexicalBonus += when {
                        hitLoc -> 0.9f
                        hitCap -> 0.75f
                        hitBucket -> 0.65f
                        else -> 0.55f
                    }
                }
            }
            val allLexicalMatched = lexicalMatchedCount == parsed.visualTokens.size
            val semScore = semanticScores[photo.id]
            if (!allLexicalMatched && semScore == null) {
                return null
            }
            if (allLexicalMatched) {
                relevance += lexicalBonus
            }
            if (semScore != null) {
                // Calibrated lift is typically +0.025..+0.120; scale to 0.25..1.20 so top visual
                // matches surface cleanly above weak edges and combine smoothly with lexical hits.
                relevance += (semScore * 10f).coerceAtLeast(0.1f)
            }
        }

        return relevance
    }

    val scored = ArrayList<Pair<GalleryEntry, Float>>(entries.size.coerceAtMost(256))
    for (entry in entries) {
        when (entry) {
            is GalleryEntry.Single -> {
                val s = scoreSinglePhoto(entry.photo)
                if (s != null) scored.add(entry to s)
            }
            is GalleryEntry.Burst -> {
                var bestPhotoMatch: PhotoItemEntity? = null
                var bestScore = Float.NEGATIVE_INFINITY
                for (p in entry.photos) {
                    val s = scoreSinglePhoto(p) ?: continue
                    if (bestPhotoMatch == null || s > bestScore ||
                        (s == bestScore && p.id == entry.bestPhoto.id)
                    ) {
                        bestPhotoMatch = p
                        bestScore = s
                    }
                }
                if (bestPhotoMatch != null) {
                    val surfaced = if (bestPhotoMatch.id == entry.bestPhoto.id) {
                        entry
                    } else {
                        entry.copy(bestPhoto = bestPhotoMatch)
                    }
                    scored.add(surfaced to bestScore)
                }
            }
        }
    }

    // Rank by relevance when free-text visual/location search is active; preserve chronological
    // timeline order for pure structured filters (date range, chips, albums).
    val shouldRankByRelevance = hasKeyword && parsed != null &&
        (parsed.visualTokens.isNotEmpty() || parsed.locationOrAlbumTokens.isNotEmpty())
    if (shouldRankByRelevance) {
        scored.sortWith(
            compareByDescending<Pair<GalleryEntry, Float>> { it.second }
                .thenByDescending { it.first.latestTimestamp }
        )
    }

    return scored.map { it.first }
}

/**
 * Convenience overload accepting a binary match ID set (retained for call sites without scores).
 */
fun filterGalleryEntriesBySearch(
    entries: List<GalleryEntry>,
    query: TimelineSearchQuery,
    semanticMatchIds: Set<Long>
): List<GalleryEntry> {
    val scoreMap = if (semanticMatchIds.isEmpty()) {
        emptyMap()
    } else {
        semanticMatchIds.associateWith { 0.05f }
    }
    return filterGalleryEntriesBySearch(entries, query, scoreMap)
}
