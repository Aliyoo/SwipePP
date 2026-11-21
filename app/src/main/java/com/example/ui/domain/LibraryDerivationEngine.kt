package com.example.ui.domain

import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.PhotoRepository
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverConfigEntity
import com.example.ui.CleanupCardItem
import com.example.ui.GalleryEntry
import com.example.ui.LocationCluster
import com.example.ui.MonthPageModel
import com.example.ui.UNLOCATED_LOCATION_KEY
import com.example.ui.YearbookCoverCurator
import com.example.ui.YearbookSummary
import com.example.ui.theme.TimelineSortOrder

/**
 * Pure, stateless O(N) domain derivation engine for FlipYear.
 * Centralizes burst stack grouping, yearbook aggregation, monthly calendar page modeling,
 * location clustering, and cleanup queue construction off the main thread.
 */
object LibraryDerivationEngine {

    private class YearAcc(val year: Int) {
        var total = 0
        var videoCount = 0
        var unreviewedCount = 0
        var keptCount = 0
        var trashedCount = 0
        var reclaimableBytes = 0L
        val yearPhotos = ArrayList<PhotoItemEntity>(32)
        val monthPresent = BooleanArray(13)
        val monthUnreviewed = IntArray(13)
    }

    private class ClusterAcc(val name: String, firstPhoto: PhotoItemEntity) {
        val photos = ArrayList<PhotoItemEntity>(16)
        var latSum = 0.0
        var lonSum = 0.0
        var coordCount = 0
        var unreviewedCount = 0
        var totalBytes = 0L
        var favoriteCover: PhotoItemEntity? = null
        var bestQualityCover: PhotoItemEntity = firstPhoto
    }

    fun hasValidLocation(photo: PhotoItemEntity): Boolean {
        val lat = photo.latitude
        val lon = photo.longitude
        return lat != null && lon != null &&
            lat.isFinite() && lon.isFinite() &&
            lat in -85.05..85.05 && lon in -180.0..180.0 &&
            !photo.locationName.isNullOrBlank()
    }

    /**
     * Single-pass O(N) Yearbook aggregator using fixed IntArray(13) month accumulators per year.
     */
    fun buildYearbooksLinear(
        photos: List<PhotoItemEntity>,
        stamps: Map<String, MonthStampEntity>,
        coverConfigs: Map<Int, YearbookCoverConfigEntity>,
        sortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST
    ): List<YearbookSummary> {
        if (photos.isEmpty()) return emptyList()
        val byYear = HashMap<Int, YearAcc>(16)

        for (p in photos) {
            val acc = byYear.getOrPut(p.year) { YearAcc(p.year) }
            acc.total++
            acc.yearPhotos.add(p)
            if (p.isVideo) acc.videoCount++

            val m = p.month
            if (m in 1..12) acc.monthPresent[m] = true

            when (p.photoStatus) {
                PhotoStatus.UNREVIEWED -> {
                    acc.unreviewedCount++
                    acc.reclaimableBytes += p.fileSizeBytes
                    if (m in 1..12) acc.monthUnreviewed[m]++
                }
                PhotoStatus.KEPT -> acc.keptCount++
                PhotoStatus.TRASHED -> acc.trashedCount++
                PhotoStatus.PURGED -> Unit
            }
        }

        val sortedYears = if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
            byYear.keys.sortedDescending()
        } else {
            byYear.keys.sorted()
        }
        return sortedYears.mapIndexed { idx, year ->
            val acc = byYear[year]!!
            var totalMonths = 0
            var cleanedMonths = 0
            for (m in 1..12) {
                if (acc.monthPresent[m]) {
                    totalMonths++
                    val key = PhotoRepository.yearMonthKey(year, m)
                    if (acc.monthUnreviewed[m] == 0 || stamps.containsKey(key)) {
                        cleanedMonths++
                    }
                }
            }
            val resolvedCover = YearbookCoverCurator.resolveCoverPresentation(
                yearPhotos = acc.yearPhotos,
                config = coverConfigs[year]
            )

            YearbookSummary(
                year = year,
                coverPhoto = resolvedCover.coverPhoto,
                coverSlides = resolvedCover.slides,
                coverMode = resolvedCover.mode,
                coverIntervalSeconds = resolvedCover.intervalSeconds,
                customCoverPhotoIds = resolvedCover.customPhotoIds,
                totalPhotos = acc.total,
                videoCount = acc.videoCount,
                unreviewedCount = acc.unreviewedCount,
                keptCount = acc.keptCount,
                trashedCount = acc.trashedCount,
                cleanedMonthsCount = cleanedMonths,
                totalMonthsCount = totalMonths,
                reclaimableBytes = acc.reclaimableBytes,
                editionIndex = idx % 4
            )
        }
    }

    /**
     * Strictly O(N) linear gallery builder for 10,000+ items.
     * Pre-groups burst clusters in a single HashMap pass instead of scanning all items per burst group.
     */
    fun buildGalleryEntries(
        photos: List<PhotoItemEntity>,
        sortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST
    ): List<GalleryEntry> {
        if (photos.isEmpty()) return emptyList()
        val ordered = if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
            photos.sortedByDescending { it.takenAtMillis }
        } else {
            photos.sortedBy { it.takenAtMillis }
        }
        return buildGalleryEntriesLinear(ordered)
    }

    fun buildGalleryEntriesLinear(alreadySortedPhotos: List<PhotoItemEntity>): List<GalleryEntry> {
        if (alreadySortedPhotos.isEmpty()) return emptyList()

        val burstMembers = HashMap<String, ArrayList<PhotoItemEntity>>()
        for (photo in alreadySortedPhotos) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                burstMembers.getOrPut(gid) { ArrayList(4) }.add(photo)
            }
        }

        val entries = ArrayList<GalleryEntry>(alreadySortedPhotos.size)
        val consumedGroups = HashSet<String>(burstMembers.size * 2)

        for (photo in alreadySortedPhotos) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                if (consumedGroups.add(gid)) {
                    val rawMembers = burstMembers[gid]
                    if (rawMembers != null && rawMembers.size >= 2) {
                        val members = if (rawMembers.size <= 2) {
                            if (rawMembers[0].takenAtMillis <= rawMembers[1].takenAtMillis) rawMembers else rawMembers.asReversed()
                        } else {
                            rawMembers.sortedBy { it.takenAtMillis }
                        }
                        var best = members.first()
                        for (m in members) {
                            if (m.isBestInStack) {
                                best = m
                                break
                            }
                            if (m.qualityScore > best.qualityScore) {
                                best = m
                            }
                        }
                        entries.add(
                            GalleryEntry.Burst(
                                groupId = gid,
                                photos = members,
                                bestPhoto = best
                            )
                        )
                    } else {
                        entries.add(GalleryEntry.Single(photo))
                    }
                }
            } else {
                entries.add(GalleryEntry.Single(photo))
            }
        }
        return entries
    }

    /**
     * Single-pass O(N) cluster + unlocated split with primitive accumulators.
     */
    fun buildLocationClustersAndUnlocatedLinear(
        photos: List<PhotoItemEntity>
    ): Pair<List<LocationCluster>, List<PhotoItemEntity>> {
        if (photos.isEmpty()) return emptyList<LocationCluster>() to emptyList()

        val clusterMap = HashMap<String, ClusterAcc>(64)
        val unlocated = ArrayList<PhotoItemEntity>()

        for (p in photos) {
            if (!hasValidLocation(p)) {
                unlocated.add(p)
                continue
            }
            val name = p.locationName!!
            val acc = clusterMap.getOrPut(name) { ClusterAcc(name, p) }
            acc.photos.add(p)
            acc.latSum += p.latitude!!
            acc.lonSum += p.longitude!!
            acc.coordCount++
            if (p.photoStatus == PhotoStatus.UNREVIEWED) acc.unreviewedCount++
            acc.totalBytes += p.fileSizeBytes
            if (p.isFavorite && acc.favoriteCover == null) acc.favoriteCover = p
            if (p.qualityScore > acc.bestQualityCover.qualityScore) acc.bestQualityCover = p
        }

        val clusters = ArrayList<LocationCluster>(clusterMap.size)
        for (acc in clusterMap.values) {
            if (acc.coordCount <= 0) continue
            val avgLat = (acc.latSum / acc.coordCount).coerceIn(-85.0, 85.0)
            val avgLon = (acc.lonSum / acc.coordCount).coerceIn(-180.0, 180.0)
            clusters.add(
                LocationCluster(
                    locationName = acc.name,
                    latitude = avgLat,
                    longitude = avgLon,
                    photos = acc.photos,
                    coverPhoto = acc.favoriteCover ?: acc.bestQualityCover,
                    unreviewedCount = acc.unreviewedCount,
                    totalBytes = acc.totalBytes
                )
            )
        }
        clusters.sortWith(compareByDescending<LocationCluster> { it.unreviewedCount }.thenByDescending { it.photos.size })
        return clusters to unlocated
    }

    /**
     * Builds monthly calendar spread models for [year] unified with global [sortOrder]:
     * - [TimelineSortOrder.NEWEST_FIRST]: 12 downTo 1 (matches Bookshelf newest-first swipe direction)
     * - [TimelineSortOrder.OLDEST_FIRST]: 1..12 (matches Bookshelf oldest-first swipe direction)
     * When [showEmptyMonths] is false (default), months without media are omitted.
     */
    fun buildMonthPagesForYear(
        year: Int,
        allPhotos: List<PhotoItemEntity>,
        monthStamps: Map<String, MonthStampEntity>,
        sortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST,
        showEmptyMonths: Boolean = false
    ): List<MonthPageModel> {
        val byMonth = HashMap<Int, ArrayList<PhotoItemEntity>>(16)
        for (photo in allPhotos) {
            if (photo.year == year) {
                byMonth.getOrPut(photo.month) { ArrayList() }.add(photo)
            }
        }
        val monthSequence = if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
            (12 downTo 1).toList()
        } else {
            (1..12).toList()
        }
        val targetMonths = if (showEmptyMonths) {
            monthSequence
        } else {
            monthSequence.filter { !byMonth[it].isNullOrEmpty() }
        }

        return targetMonths.map { month ->
            val rawMonthPhotos = byMonth[month].orEmpty()
            val monthPhotos = if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
                rawMonthPhotos.sortedByDescending { it.takenAtMillis }
            } else {
                rawMonthPhotos.sortedBy { it.takenAtMillis }
            }
            val photosByDay = HashMap<Int, ArrayList<PhotoItemEntity>>(32)
            var videoCount = 0
            var unreviewedCount = 0
            var keptCount = 0
            var trashedCount = 0
            var reclaimableBytes = 0L
            val burstIds = HashSet<String>()
            var favoriteCover: PhotoItemEntity? = null
            var bestQualityCover: PhotoItemEntity? = null

            for (p in monthPhotos) {
                if (p.isVideo) videoCount++
                when (p.photoStatus) {
                    PhotoStatus.UNREVIEWED -> {
                        unreviewedCount++
                        reclaimableBytes += p.fileSizeBytes
                        p.burstGroupId?.let { burstIds.add(it) }
                    }
                    PhotoStatus.KEPT -> keptCount++
                    PhotoStatus.TRASHED -> trashedCount++
                    PhotoStatus.PURGED -> Unit
                }
                if (p.photoStatus != PhotoStatus.TRASHED) {
                    photosByDay.getOrPut(p.day) { ArrayList() }.add(p)
                    if (favoriteCover == null && p.isFavorite) {
                        favoriteCover = p
                    }
                    if (bestQualityCover == null || p.qualityScore > bestQualityCover.qualityScore) {
                        bestQualityCover = p
                    }
                }
            }

            val cleanedDays = HashSet<Int>(photosByDay.size)
            for ((day, dayList) in photosByDay) {
                if (dayList.isNotEmpty() && dayList.none { it.photoStatus == PhotoStatus.UNREVIEWED }) {
                    cleanedDays.add(day)
                }
            }

            val key = PhotoRepository.yearMonthKey(year, month)
            val cover = favoriteCover ?: bestQualityCover ?: monthPhotos.firstOrNull()

            MonthPageModel(
                year = year,
                month = month,
                photos = monthPhotos,
                photosByDay = photosByDay,
                cleanedDays = cleanedDays,
                totalPhotos = monthPhotos.size,
                videoCount = videoCount,
                unreviewedCount = unreviewedCount,
                keptCount = keptCount,
                trashedCount = trashedCount,
                burstGroupCount = burstIds.size,
                reclaimableBytes = reclaimableBytes,
                stamp = monthStamps[key],
                coverPhoto = cover
            )
        }
    }

    /**
     * Builds an ordered [CleanupCardItem] queue for [ArcCleanerScreen], supporting:
     * - Year / Month / Location filtering
     * - Custom photo ID sets (e.g. launched directly from a Gallery search or smart filter!)
     * - Global [sortOrder] (while keeping individual frames inside a BurstStack chronological)
     */
    fun buildArcQueue(
        allPhotos: List<PhotoItemEntity>,
        year: Int? = null,
        month: Int? = null,
        locationName: String? = null,
        includeReviewed: Boolean = false,
        customPhotoIds: Set<Long>? = null,
        sortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST
    ): List<CleanupCardItem> {
        val candidates = ArrayList<PhotoItemEntity>()
        for (photo in allPhotos) {
            if (customPhotoIds != null && photo.id !in customPhotoIds) continue
            if (year != null && photo.year != year) continue
            if (month != null && photo.month != month) continue
            val matchLocation = when (locationName) {
                null -> true
                UNLOCATED_LOCATION_KEY -> !hasValidLocation(photo)
                else -> photo.locationName == locationName
            }
            if (!matchLocation) continue
            val st = photo.photoStatus
            val matchStatus = if (includeReviewed) st != PhotoStatus.TRASHED else st == PhotoStatus.UNREVIEWED
            if (matchStatus) candidates.add(photo)
        }
        if (sortOrder == TimelineSortOrder.NEWEST_FIRST) {
            candidates.sortByDescending { it.takenAtMillis }
        } else {
            candidates.sortBy { it.takenAtMillis }
        }

        val burstGroups = HashMap<String, ArrayList<PhotoItemEntity>>()
        for (photo in candidates) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                burstGroups.getOrPut(gid) { ArrayList(4) }.add(photo)
            }
        }

        val result = ArrayList<CleanupCardItem>(candidates.size)
        val consumedGroupIds = HashSet<String>(burstGroups.size * 2)

        for (photo in candidates) {
            val gid = photo.burstGroupId
            if (gid != null && !photo.isVideo) {
                if (consumedGroupIds.add(gid)) {
                    val rawStack = burstGroups[gid].orEmpty()
                    if (rawStack.size >= 2) {
                        val stackPhotos = rawStack.sortedBy { it.takenAtMillis }
                        var best = stackPhotos.first()
                        for (m in stackPhotos) {
                            if (m.isBestInStack) {
                                best = m
                                break
                            }
                            if (m.qualityScore > best.qualityScore) {
                                best = m
                            }
                        }
                        result.add(
                            CleanupCardItem.BurstStack(
                                groupId = gid,
                                photos = stackPhotos,
                                bestPhoto = best
                            )
                        )
                    } else {
                        result.add(CleanupCardItem.SinglePhoto(photo))
                    }
                }
            } else {
                result.add(CleanupCardItem.SinglePhoto(photo))
            }
        }
        return result
    }
}
