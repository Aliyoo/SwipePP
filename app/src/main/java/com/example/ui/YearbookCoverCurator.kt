package com.example.ui

import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverConfigEntity
import com.example.data.YearbookCoverMode
import java.util.Locale

data class ResolvedYearbookCover(
    val coverPhoto: PhotoItemEntity?,
    val slides: List<PhotoItemEntity>,
    val mode: YearbookCoverMode,
    val intervalSeconds: Int,
    val customPhotoIds: List<Long>
)

/**
 * Fast, deterministic, zero-decode aesthetic curation engine for annual monograph covers.
 *
 * Replaces naive "latest photo" selection (which often surfaces screenshots, chat saves, or
 * low-quality frames) with a multi-signal ranking that elevates favorites, high-resolution
 * camera captures, portrait-friendly cover compositions, and month-diverse highlight slides.
 */
object YearbookCoverCurator {

    const val MAX_COVER_SLIDES = 6
    const val DEFAULT_INTERVAL_SECONDS = 5

    private val utilityBucketKeywords = listOf(
        "screenshot", "screenshots", "screen_record", "screenrecord",
        "截屏", "截图", "录屏",
        "wechat", "weixin", "micromsg", "qq", "tim", "telegram", "whatsapp",
        "download", "downloads", "cache", "temp", "tmp", "sticker", "meme", "表情",
        "alipay", "taobao", "jd", "meituan", "receipt", "invoice", "发票", "二维码", "qrcode"
    )

    private val cameraBucketKeywords = listOf(
        "camera", "dcim", "raw", "dng", "lightroom", "snapseed", "vsco",
        "相机", "摄影", "旅行", "精选", "人像", "风景"
    )

    fun isLikelyScreenshotOrUtility(photo: PhotoItemEntity): Boolean {
        val bucket = photo.bucketName?.lowercase(Locale.ROOT).orEmpty()
        val caption = photo.caption.lowercase(Locale.ROOT)
        if (utilityBucketKeywords.any { kw -> bucket.contains(kw) || caption.contains(kw) }) {
            return true
        }
        if (photo.widthPx > 0 && photo.heightPx > 0) {
            val ratio = photo.widthPx.toFloat() / photo.heightPx.toFloat()
            // Ultra-tall scrolling screenshots or ultra-thin banner strips rarely make good covers
            if (ratio < 0.45f || ratio > 2.45f) {
                return true
            }
        }
        return false
    }

    fun scoreCoverCandidate(photo: PhotoItemEntity): Int {
        var score = 50

        if (photo.isFavorite) score += 42
        if (photo.photoStatus == PhotoStatus.KEPT) score += 16
        if (photo.isBestInStack) score += 12

        // Quality score signal (centered at 50 -> -24..+24)
        score += ((photo.qualityScore.coerceIn(0, 100) - 50) * 0.48f).toInt()

        // Prefer crisp photography over video frames for hardcover book aesthetics
        score += if (!photo.isVideo) 8 else -6

        // Aspect ratio fit for 0.70 portrait monograph book cover
        if (photo.widthPx > 0 && photo.heightPx > 0) {
            val ratio = photo.widthPx.toFloat() / photo.heightPx.toFloat()
            score += when {
                ratio in 0.55f..0.88f -> 18 // Portrait 2:3, 3:4, 4:5, 9:16
                ratio in 0.88f..1.45f -> 10 // Square, 4:3, 3:2
                ratio in 0.45f..0.55f -> 2
                ratio in 1.45f..2.20f -> 4
                else -> -24
            }

            val minDim = minOf(photo.widthPx, photo.heightPx)
            score += when {
                minDim >= 1800 -> 10
                minDim >= 1000 -> 6
                minDim in 1..599 -> -22
                else -> 0
            }
        }

        val bucket = photo.bucketName?.lowercase(Locale.ROOT).orEmpty()
        if (cameraBucketKeywords.any { bucket.contains(it) }) {
            score += 10
        }

        if (!photo.locationName.isNullOrBlank()) {
            score += 12
        }

        if (isLikelyScreenshotOrUtility(photo)) {
            score -= 48
        }

        if (photo.visualVariant.equals("BLURRY", ignoreCase = true)) {
            score -= 30
        }

        return score
    }

    /**
     * Ranks all visible photos of a year for the "智能推荐" tab in the Cover Studio sheet,
     * placing cover-worthy photographs at the very top and demoting screenshots/utilities.
     */
    fun rankPhotosForCoverPicker(yearPhotos: List<PhotoItemEntity>): List<PhotoItemEntity> {
        val visible = yearPhotos.filter {
            it.photoStatus != PhotoStatus.TRASHED && it.photoStatus != PhotoStatus.PURGED
        }
        if (visible.isEmpty()) return emptyList()
        return visible
            .map { it to scoreCoverCandidate(it) }
            .sortedWith(
                compareByDescending<Pair<PhotoItemEntity, Int>> { it.second }
                    .thenByDescending { it.first.takenAtMillis }
            )
            .map { it.first }
    }

    /**
     * Curates up to [maxSlides] visually diverse, high-scoring highlight photos across the year:
     * - Deduplicates burst stacks (at most 1 frame per burstGroupId).
     * - Prioritizes month diversity so the slideshow spans the full year's seasons.
     * - Always anchors index 0 to the single highest-scoring hero photograph.
     */
    fun curateSmartCoverSlides(
        yearPhotos: List<PhotoItemEntity>,
        maxSlides: Int = MAX_COVER_SLIDES
    ): List<PhotoItemEntity> {
        if (yearPhotos.isEmpty() || maxSlides <= 0) return emptyList()

        val visible = yearPhotos.filter {
            it.photoStatus != PhotoStatus.TRASHED && it.photoStatus != PhotoStatus.PURGED
        }
        if (visible.isEmpty()) {
            return listOf(yearPhotos.first())
        }

        val scored = visible
            .map { it to scoreCoverCandidate(it) }
            .sortedWith(
                compareByDescending<Pair<PhotoItemEntity, Int>> { it.second }
                    .thenByDescending { it.first.takenAtMillis }
            )

        val selected = ArrayList<PhotoItemEntity>(maxSlides)
        val usedIds = HashSet<Long>(maxSlides * 2)
        val usedBursts = HashSet<String>(maxSlides * 2)
        val usedMonths = HashSet<Int>(16)
        val usedMonthDays = HashSet<Int>(maxSlides * 2)

        fun canUse(photo: PhotoItemEntity): Boolean {
            if (photo.id in usedIds) return false
            val burst = photo.burstGroupId
            if (!burst.isNullOrBlank() && burst in usedBursts) return false
            return true
        }

        fun pick(photo: PhotoItemEntity) {
            selected.add(photo)
            usedIds.add(photo.id)
            photo.burstGroupId?.takeIf { it.isNotBlank() }?.let { usedBursts.add(it) }
            usedMonths.add(photo.month)
            usedMonthDays.add(photo.month * 100 + photo.day)
        }

        // Pass 1: Best non-utility photo per distinct month (seasonally balanced highlights)
        for ((photo, _) in scored) {
            if (selected.size >= maxSlides) break
            if (!canUse(photo)) continue
            if (photo.month in usedMonths) continue
            if (isLikelyScreenshotOrUtility(photo)) continue
            pick(photo)
        }

        // Pass 2: Best non-utility photo per distinct day to fill remaining slots
        if (selected.size < maxSlides) {
            for ((photo, _) in scored) {
                if (selected.size >= maxSlides) break
                if (!canUse(photo)) continue
                if ((photo.month * 100 + photo.day) in usedMonthDays) continue
                if (isLikelyScreenshotOrUtility(photo)) continue
                pick(photo)
            }
        }

        // Pass 3: Remaining top non-utility photos (still deduplicating burst groups)
        if (selected.size < maxSlides) {
            for ((photo, _) in scored) {
                if (selected.size >= maxSlides) break
                if (!canUse(photo)) continue
                if (isLikelyScreenshotOrUtility(photo)) continue
                pick(photo)
            }
        }

        // Pass 4: Fallback if the entire year only contains screenshots/utilities
        if (selected.isEmpty()) {
            for ((photo, _) in scored) {
                if (selected.size >= maxSlides) break
                if (!canUse(photo)) continue
                pick(photo)
            }
        }

        return selected
    }

    /**
     * Resolves the effective cover slides & mode for a year by combining user customization
     * (if saved) with graceful fallback to smart curation when pinned photos are trashed/deleted.
     */
    fun resolveCoverPresentation(
        yearPhotos: List<PhotoItemEntity>,
        config: YearbookCoverConfigEntity?,
        maxSlides: Int = MAX_COVER_SLIDES
    ): ResolvedYearbookCover {
        val smartSlides = curateSmartCoverSlides(yearPhotos, maxSlides)
        val interval = (config?.intervalSeconds ?: DEFAULT_INTERVAL_SECONDS).coerceIn(3, 12)

        if (config == null || config.coverMode == YearbookCoverMode.SMART_CAROUSEL) {
            return ResolvedYearbookCover(
                coverPhoto = smartSlides.firstOrNull(),
                slides = smartSlides,
                mode = YearbookCoverMode.SMART_CAROUSEL,
                intervalSeconds = interval,
                customPhotoIds = emptyList()
            )
        }

        val visibleById = HashMap<Long, PhotoItemEntity>(yearPhotos.size)
        for (p in yearPhotos) {
            if (p.photoStatus != PhotoStatus.TRASHED && p.photoStatus != PhotoStatus.PURGED) {
                visibleById[p.id] = p
            }
        }

        val requestedIds = config.selectedPhotoIds
        val resolvedCustom = requestedIds
            .mapNotNull { visibleById[it] }
            .take(maxSlides)

        if (resolvedCustom.isEmpty()) {
            // Graceful fallback if all pinned photos were deleted/trashed
            return ResolvedYearbookCover(
                coverPhoto = smartSlides.firstOrNull(),
                slides = smartSlides,
                mode = YearbookCoverMode.SMART_CAROUSEL,
                intervalSeconds = interval,
                customPhotoIds = emptyList()
            )
        }

        return when (config.coverMode) {
            YearbookCoverMode.SINGLE_CUSTOM -> {
                val single = resolvedCustom.first()
                ResolvedYearbookCover(
                    coverPhoto = single,
                    slides = listOf(single),
                    mode = YearbookCoverMode.SINGLE_CUSTOM,
                    intervalSeconds = interval,
                    customPhotoIds = listOf(single.id)
                )
            }
            YearbookCoverMode.CUSTOM_CAROUSEL -> {
                val effectiveMode = if (resolvedCustom.size == 1) {
                    YearbookCoverMode.SINGLE_CUSTOM
                } else {
                    YearbookCoverMode.CUSTOM_CAROUSEL
                }
                ResolvedYearbookCover(
                    coverPhoto = resolvedCustom.first(),
                    slides = resolvedCustom,
                    mode = effectiveMode,
                    intervalSeconds = interval,
                    customPhotoIds = resolvedCustom.map { it.id }
                )
            }
            YearbookCoverMode.SMART_CAROUSEL -> {
                ResolvedYearbookCover(
                    coverPhoto = smartSlides.firstOrNull(),
                    slides = smartSlides,
                    mode = YearbookCoverMode.SMART_CAROUSEL,
                    intervalSeconds = interval,
                    customPhotoIds = emptyList()
                )
            }
        }
    }
}
