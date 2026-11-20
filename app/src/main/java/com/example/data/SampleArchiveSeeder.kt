package com.example.data

import java.util.Calendar

object SampleArchiveSeeder {

    fun buildSamplePhotos(): List<PhotoItemEntity> {
        val list = mutableListOf<PhotoItemEntity>()

        fun ts(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long {
            return Calendar.getInstance().apply {
                set(year, month - 1, day, hour, minute, second)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        // === 2026 ===
        list += PhotoItemEntity(
            year = 2026, month = 10, day = 18,
            takenAtMillis = ts(2026, 10, 18, 17, 42, 10),
            fileSizeBytes = 5_420_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_kyoto", status = PhotoStatus.UNREVIEWED.name,
            isFavorite = true, burstGroupId = "burst_2026_10_kyoto_alley",
            qualityScore = 96, isBestInStack = true, visualVariant = "CRISP",
            caption = "祇园黄昏灯笼与红叶 · 最佳构图",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )
        list += PhotoItemEntity(
            year = 2026, month = 10, day = 18,
            takenAtMillis = ts(2026, 10, 18, 17, 42, 12),
            fileSizeBytes = 5_180_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_kyoto", status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = "burst_2026_10_kyoto_alley",
            qualityScore = 71, isBestInStack = false, visualVariant = "SLIGHT_TILT",
            caption = "祇园黄昏连拍 #2 · 轻微倾斜",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )
        list += PhotoItemEntity(
            year = 2026, month = 10, day = 18,
            takenAtMillis = ts(2026, 10, 18, 17, 42, 14),
            fileSizeBytes = 4_960_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_kyoto", status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = "burst_2026_10_kyoto_alley",
            qualityScore = 64, isBestInStack = false, visualVariant = "DARK_EXPOSURE",
            caption = "祇园黄昏连拍 #3 · 欠曝偏暗",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )
        // Sample 4K Video clip in 2026-10
        list += PhotoItemEntity(
            mediaType = MediaType.VIDEO.name, durationMs = 24_000L,
            year = 2026, month = 10, day = 21,
            takenAtMillis = ts(2026, 10, 21, 14, 15, 0),
            fileSizeBytes = 48_600_000L, widthPx = 3840, heightPx = 2160,
            drawableName = "img_photo_cafe", status = PhotoStatus.UNREVIEWED.name,
            qualityScore = 92, visualVariant = "CRISP",
            caption = "4K 60fps · 东京雨后手冲拉花实拍",
            locationName = "东京 · 表参道", latitude = 35.6652, longitude = 139.7121
        )
        list += PhotoItemEntity(
            year = 2026, month = 10, day = 24,
            takenAtMillis = ts(2026, 10, 24, 18, 5, 0),
            fileSizeBytes = 3_840_000L, widthPx = 3024, heightPx = 4032,
            drawableName = "img_photo_kyoto", status = PhotoStatus.UNREVIEWED.name,
            qualityScore = 88, visualVariant = "WARM_FILM",
            caption = "清水寺石阶晚霞",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )

        // 2026-07
        list += PhotoItemEntity(
            year = 2026, month = 7, day = 11,
            takenAtMillis = ts(2026, 7, 11, 18, 36, 1),
            fileSizeBytes = 6_140_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_amalfi", status = PhotoStatus.UNREVIEWED.name,
            isFavorite = true, burstGroupId = "burst_2026_07_amalfi_cliff",
            qualityScore = 95, isBestInStack = true, visualVariant = "CRISP",
            caption = "阿马尔菲悬崖落日与地中海碧波",
            locationName = "意大利 · 阿马尔菲海岸", latitude = 40.6333, longitude = 14.6029
        )
        list += PhotoItemEntity(
            year = 2026, month = 7, day = 11,
            takenAtMillis = ts(2026, 7, 11, 18, 36, 4),
            fileSizeBytes = 5_820_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_amalfi", status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = "burst_2026_07_amalfi_cliff",
            qualityScore = 68, isBestInStack = false, visualVariant = "OVEREXPOSED",
            caption = "阿马尔菲连拍 #2 · 高光过曝",
            locationName = "意大利 · 阿马尔菲海岸", latitude = 40.6333, longitude = 14.6029
        )
        list += PhotoItemEntity(
            mediaType = MediaType.VIDEO.name, durationMs = 42_000L,
            year = 2026, month = 7, day = 14,
            takenAtMillis = ts(2026, 7, 14, 9, 20, 0),
            fileSizeBytes = 84_200_000L, widthPx = 3840, heightPx = 2160,
            drawableName = "img_photo_alps", status = PhotoStatus.UNREVIEWED.name,
            qualityScore = 94, visualVariant = "CRISP",
            caption = "阿尔卑斯湖畔延时航拍片段",
            locationName = "瑞士 · 因特拉肯", latitude = 46.6863, longitude = 7.8632
        )

        // 2026-05 (Cleaned & badged)
        list += PhotoItemEntity(
            year = 2026, month = 5, day = 3,
            takenAtMillis = ts(2026, 5, 3, 10, 30, 0),
            fileSizeBytes = 4_600_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_alps", status = PhotoStatus.KEPT.name,
            isFavorite = true, qualityScore = 94, visualVariant = "CRISP",
            caption = "少女峰脚下晨雾与湖水",
            locationName = "瑞士 · 劳特布龙嫩", latitude = 46.5935, longitude = 7.9091
        )
        list += PhotoItemEntity(
            year = 2026, month = 5, day = 12,
            takenAtMillis = ts(2026, 5, 12, 15, 22, 0),
            fileSizeBytes = 3_950_000L, widthPx = 3024, heightPx = 4032,
            drawableName = "img_photo_cafe", status = PhotoStatus.KEPT.name,
            qualityScore = 89, visualVariant = "CRISP",
            caption = "五月雨天窗边拿铁",
            locationName = "东京 · 表参道", latitude = 35.6652, longitude = 139.7121
        )

        // === 2025 ===
        list += PhotoItemEntity(
            year = 2025, month = 11, day = 8,
            takenAtMillis = ts(2025, 11, 8, 7, 50, 0),
            fileSizeBytes = 6_420_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_alps", status = PhotoStatus.UNREVIEWED.name,
            isFavorite = true, burstGroupId = "burst_2025_11_alps_dawn",
            qualityScore = 97, isBestInStack = true, visualVariant = "CRISP",
            caption = "劳特布龙嫩初雪日出 · 镜面湖泊",
            locationName = "瑞士 · 劳特布龙嫩", latitude = 46.5935, longitude = 7.9091
        )
        list += PhotoItemEntity(
            year = 2025, month = 11, day = 8,
            takenAtMillis = ts(2025, 11, 8, 7, 50, 3),
            fileSizeBytes = 6_050_000L, widthPx = 4032, heightPx = 3024,
            drawableName = "img_photo_alps", status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = "burst_2025_11_alps_dawn",
            qualityScore = 69, isBestInStack = false, visualVariant = "OVEREXPOSED",
            caption = "雪峰连拍 #2 · 雪面过曝",
            locationName = "瑞士 · 劳特布龙嫩", latitude = 46.5935, longitude = 7.9091
        )
        list += PhotoItemEntity(
            mediaType = MediaType.VIDEO.name, durationMs = 15_000L,
            year = 2025, month = 11, day = 16,
            takenAtMillis = ts(2025, 11, 16, 17, 10, 0),
            fileSizeBytes = 32_400_000L, widthPx = 1920, heightPx = 1080,
            drawableName = "img_photo_kyoto", status = PhotoStatus.UNREVIEWED.name,
            qualityScore = 88, visualVariant = "CRISP",
            caption = "深秋红叶微风慢动作",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )

        return list
    }

    fun buildInitialStamps(): List<MonthStampEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            MonthStampEntity(
                yearMonthKey = "2026-05",
                year = 2026,
                month = 5,
                cleanedAtMillis = now - 86_400_000L * 12,
                freedBytes = 28_600_000L
            )
        )
    }

    fun buildInitialStats(): CleanupStatsEntity = CleanupStatsEntity(
        id = 1,
        totalPurgedBytes = 28_600_000L,
        totalPurgedCount = 7,
        cleanedStreakDays = 3
    )
}
