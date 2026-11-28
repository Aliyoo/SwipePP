package com.example.ui.domain

import com.example.data.MediaSubtype
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.smarttag.smartCategories
import com.example.ui.TreasureRecommendation
import com.example.ui.YearbookCoverCurator
import com.example.ui.theme.TimelineSortOrder
import java.util.Calendar

/**
 * Deterministic on-device treasure recommendation engine.
 *
 * Uses explicit user curation as the semantic prior: once at least [MIN_TREASURE_CORPUS]
 * treasures exist, smart content labels shared with those treasures become a boost. Quality,
 * location, burst-best status and utility filtering remain the hard safeguards; the engine only
 * recommends and never mutates favorite state itself.
 */
object TreasureRecommendationEngine {
    private data class Candidate(
        val photo: PhotoItemEntity,
        val baseScore: Int,
        val semanticScore: Int,
        val sharedLabel: com.example.data.smarttag.SmartCategory?
    )

    const val MIN_TREASURE_CORPUS = 5
    private const val MIN_QUALITY_SCORE = 72
    private const val MAX_SAME_DAY = 2

    fun recommend(
        year: Int,
        allPhotos: List<PhotoItemEntity>,
        maxRecommendations: Int = 9,
        sortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST
    ): List<TreasureRecommendation> {
        if (allPhotos.isEmpty() || maxRecommendations <= 0) return emptyList()
        val calendar = Calendar.getInstance()

        fun captureYearMonth(photo: PhotoItemEntity): Pair<Int, Int> {
            calendar.timeInMillis = photo.takenAtMillis
            return calendar.get(Calendar.YEAR) to (calendar.get(Calendar.MONTH) + 1)
        }

        val yearPhotos = allPhotos.filter { captureYearMonth(it).first == year }
        if (yearPhotos.isEmpty()) return emptyList()

        val visibleTreasures = yearPhotos.filter {
            it.isFavorite && it.photoStatus != PhotoStatus.TRASHED && it.photoStatus != PhotoStatus.PURGED
        }
        val labelFrequency = HashMap<com.example.data.smarttag.SmartCategory, Int>()
        if (visibleTreasures.size >= MIN_TREASURE_CORPUS) {
            for (treasure in visibleTreasures) {
                for (category in treasure.smartCategories()) {
                    if (category == com.example.data.smarttag.SmartCategory.DOCUMENT ||
                        category == com.example.data.smarttag.SmartCategory.SCREEN
                    ) continue
                    labelFrequency[category] = (labelFrequency[category] ?: 0) + 1
                }
            }
        }

        val usedBursts = HashSet<String>()
        val candidates = ArrayList<Candidate>()
        val sortedPool = yearPhotos.sortedWith(
            compareByDescending<PhotoItemEntity> { it.isBestInStack }
                .thenByDescending { it.qualityScore }
                .thenByDescending { it.takenAtMillis }
        )

        for (photo in sortedPool) {
            if (photo.isFavorite) continue
            if (photo.photoStatus == PhotoStatus.TRASHED || photo.photoStatus == PhotoStatus.PURGED) continue
            if (photo.qualityScore < MIN_QUALITY_SCORE) continue
            if (photo.mediaSubtypeKind != MediaSubtype.STANDARD && photo.mediaSubtypeKind != MediaSubtype.CAMERA) continue
            if (YearbookCoverCurator.isLikelyScreenshotOrUtility(photo)) continue
            val burst = photo.burstGroupId
            if (!burst.isNullOrBlank() && !usedBursts.add(burst)) continue

            var base = photo.qualityScore
            if (photo.photoStatus == PhotoStatus.KEPT) base += 8
            if (!photo.locationName.isNullOrBlank()) base += 6
            if (photo.isBestInStack) base += 6
            if (photo.isVideo) base -= 5
            if (photo.visualVariant.equals("BLURRY", ignoreCase = true)) base -= 28

            val shared = photo.smartCategories()
                .filter { labelFrequency.containsKey(it) }
                .maxByOrNull { labelFrequency[it] ?: 0 }
            val semantic = if (shared == null) {
                0
            } else {
                val overlapCount = photo.smartCategories().count { labelFrequency.containsKey(it) }
                (10 + overlapCount * 5 + (labelFrequency[shared] ?: 0).coerceAtMost(4)).coerceAtMost(24)
            }
            candidates.add(Candidate(photo, base, semantic, shared))
        }

        val ranked = candidates
            .sortedWith(
                compareByDescending<Candidate> { it.baseScore + it.semanticScore }
                    .thenByDescending {
                        if (sortOrder == TimelineSortOrder.NEWEST_FIRST) it.photo.takenAtMillis else -it.photo.takenAtMillis
                    }
            )

        val result = ArrayList<TreasureRecommendation>(maxRecommendations)
        val dayCounts = HashMap<Int, Int>()
        for (candidate in ranked) {
            if (result.size >= maxRecommendations) break
            val photo = candidate.photo
            val monthDay = run {
                calendar.timeInMillis = photo.takenAtMillis
                (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
            }
            val sameDay = dayCounts[monthDay] ?: 0
            if (sameDay >= MAX_SAME_DAY) continue
            dayCounts[monthDay] = sameDay + 1
            val total = candidate.baseScore + candidate.semanticScore
            val reason = when {
                candidate.sharedLabel != null -> "与你珍藏的${candidate.sharedLabel.zhLabel}相近"
                photo.isBestInStack -> "连拍优选"
                photo.qualityScore >= 90 -> "高画质摄影"
                !photo.locationName.isNullOrBlank() -> photo.locationName.orEmpty()
                else -> "年度高光候选"
            }
            result.add(TreasureRecommendation(photo = photo, score = total, reason = reason))
        }

        return result
    }
}
