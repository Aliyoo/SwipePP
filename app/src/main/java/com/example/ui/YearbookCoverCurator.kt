package com.example.ui

import com.example.data.CollageBackdropStyle
import com.example.data.CollageSlotTransform
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverConfigEntity
import com.example.data.YearbookCoverMode
import com.example.ui.components.YearbookCollageTemplates
import com.example.ui.theme.YearbookCoverStyle
import java.util.Locale

data class ResolvedYearbookCover(
    val coverPhoto: PhotoItemEntity?,
    val slides: List<PhotoItemEntity>,
    val mode: YearbookCoverMode,
    val intervalSeconds: Int,
    val customPhotoIds: List<Long>,
    val effectiveIsCollage: Boolean,
    val collageTemplateId: String,
    val collageSpacingDp: Float,
    val collageBackdrop: CollageBackdropStyle,
    val collageSlotTransforms: List<CollageSlotTransform>
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
                compareByDescending<Pair<PhotoItemEntity, Int>> {
                    it.first.isFavorite && !isLikelyScreenshotOrUtility(it.first)
                }
                    .thenByDescending { it.second }
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
                compareByDescending<Pair<PhotoItemEntity, Int>> {
                    it.first.isFavorite && !isLikelyScreenshotOrUtility(it.first)
                }
                    .thenByDescending { it.second }
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

        fun fillTier(
            candidates: List<Pair<PhotoItemEntity, Int>>,
            requireUnusedMonth: Boolean,
            requireUnusedMonthDay: Boolean,
            excludeUtilities: Boolean = true
        ) {
            for ((photo, _) in candidates) {
                if (selected.size >= maxSlides) break
                if (!canUse(photo)) continue
                if (requireUnusedMonth && photo.month in usedMonths) continue
                if (requireUnusedMonthDay && (photo.month * 100 + photo.day) in usedMonthDays) continue
                if (excludeUtilities && isLikelyScreenshotOrUtility(photo)) continue
                pick(photo)
            }
        }

        // Explicit user curation outranks inferred diversity: exhaust the treasured pool first,
        // then backfill with the best non-treasured photography only when treasures are insufficient.
        val nonUtilityScored = scored.filterNot { isLikelyScreenshotOrUtility(it.first) }
        val treasuredTier = nonUtilityScored.filter { it.first.isFavorite }
        val fallbackTier = nonUtilityScored.filterNot { it.first.isFavorite }

        for (tier in listOf(treasuredTier, fallbackTier)) {
            fillTier(tier, requireUnusedMonth = true, requireUnusedMonthDay = false)
            fillTier(tier, requireUnusedMonth = false, requireUnusedMonthDay = true)
            fillTier(tier, requireUnusedMonth = false, requireUnusedMonthDay = false)
        }

        // Final fallback if the entire year only contains screenshots/utilities.
        if (selected.isEmpty()) {
            fillTier(
                scored,
                requireUnusedMonth = false,
                requireUnusedMonthDay = false,
                excludeUtilities = false
            )
        }

        return selected
    }

    /**
     * Convenience overload inferring year from config or first photo (preserves compatibility with tests).
     */
    fun resolveCoverPresentation(
        yearPhotos: List<PhotoItemEntity>,
        config: YearbookCoverConfigEntity?,
        defaultCoverStyle: YearbookCoverStyle = YearbookCoverStyle.COLLAGE,
        maxSlides: Int = MAX_COVER_SLIDES
    ): ResolvedYearbookCover = resolveCoverPresentation(
        year = config?.year ?: yearPhotos.firstOrNull()?.year ?: 2025,
        yearPhotos = yearPhotos,
        config = config,
        defaultCoverStyle = defaultCoverStyle,
        maxSlides = maxSlides
    )

    /**
     * Resolves the effective cover presentation for a year by combining user customization
     * (if saved) with global cover style preference (smart collage vs smart carousel) and
     * graceful fallback when pinned photos are deleted/trashed.
     */
    fun resolveCoverPresentation(
        year: Int,
        yearPhotos: List<PhotoItemEntity>,
        config: YearbookCoverConfigEntity?,
        defaultCoverStyle: YearbookCoverStyle = YearbookCoverStyle.COLLAGE,
        maxSlides: Int = MAX_COVER_SLIDES
    ): ResolvedYearbookCover {
        val smartSlides = curateSmartCoverSlides(yearPhotos, maxSlides)
        val interval = (config?.intervalSeconds ?: DEFAULT_INTERVAL_SECONDS).coerceIn(3, 12)
        val smartTemplate = YearbookCollageTemplates.defaultTemplateFor(year, smartSlides.size)
        val defaultSmartIsCollage = defaultCoverStyle == YearbookCoverStyle.COLLAGE && smartSlides.size >= 2

        fun smartDefaultResult(): ResolvedYearbookCover = ResolvedYearbookCover(
            coverPhoto = smartSlides.firstOrNull(),
            slides = smartSlides,
            mode = YearbookCoverMode.SMART_CAROUSEL,
            intervalSeconds = interval,
            customPhotoIds = emptyList(),
            effectiveIsCollage = defaultSmartIsCollage,
            collageTemplateId = smartTemplate.id,
            collageSpacingDp = smartTemplate.defaultSpacingDp,
            collageBackdrop = CollageBackdropStyle.EDITION,
            collageSlotTransforms = emptyList()
        )

        if (config == null || config.coverMode == YearbookCoverMode.SMART_CAROUSEL) {
            return smartDefaultResult()
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
            return smartDefaultResult()
        }

        return when (config.coverMode) {
            YearbookCoverMode.CUSTOM_COLLAGE -> {
                val chosenTemplate = YearbookCollageTemplates.resolveTemplate(
                    templateId = config.collageTemplateId.ifBlank { null },
                    year = year,
                    availablePhotoCount = maxOf(resolvedCustom.size, smartSlides.size)
                )
                // If user deleted some photos and fewer remain than slots, gracefully pad from smart pool
                val filledPhotos = buildList(chosenTemplate.slotCount) {
                    val usedIds = HashSet<Long>()
                    resolvedCustom.forEach {
                        if (size < chosenTemplate.slotCount && usedIds.add(it.id)) add(it)
                    }
                    smartSlides.forEach {
                        if (size < chosenTemplate.slotCount && usedIds.add(it.id)) add(it)
                    }
                }
                val canRenderCollage = filledPhotos.size >= 2
                ResolvedYearbookCover(
                    coverPhoto = filledPhotos.firstOrNull() ?: resolvedCustom.first(),
                    slides = filledPhotos.ifEmpty { resolvedCustom },
                    mode = YearbookCoverMode.CUSTOM_COLLAGE,
                    intervalSeconds = interval,
                    customPhotoIds = resolvedCustom.map { it.id },
                    effectiveIsCollage = canRenderCollage,
                    collageTemplateId = chosenTemplate.id,
                    collageSpacingDp = config.collageSpacingDp.coerceIn(0f, 16f),
                    collageBackdrop = config.backdropStyle,
                    collageSlotTransforms = config.slotTransforms
                )
            }
            YearbookCoverMode.SINGLE_CUSTOM -> {
                val single = resolvedCustom.first()
                ResolvedYearbookCover(
                    coverPhoto = single,
                    slides = listOf(single),
                    mode = YearbookCoverMode.SINGLE_CUSTOM,
                    intervalSeconds = interval,
                    customPhotoIds = listOf(single.id),
                    effectiveIsCollage = false,
                    collageTemplateId = smartTemplate.id,
                    collageSpacingDp = smartTemplate.defaultSpacingDp,
                    collageBackdrop = CollageBackdropStyle.EDITION,
                    collageSlotTransforms = emptyList()
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
                    customPhotoIds = resolvedCustom.map { it.id },
                    effectiveIsCollage = false,
                    collageTemplateId = smartTemplate.id,
                    collageSpacingDp = smartTemplate.defaultSpacingDp,
                    collageBackdrop = CollageBackdropStyle.EDITION,
                    collageSlotTransforms = emptyList()
                )
            }
            YearbookCoverMode.SMART_CAROUSEL -> smartDefaultResult()
        }
    }
}
