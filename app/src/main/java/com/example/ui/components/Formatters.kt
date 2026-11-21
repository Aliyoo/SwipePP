package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PhotoItemEntity
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
    }
}

fun formatVideoDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(1L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun monthEnglishName(month: Int): String = when (month) {
    1 -> "一月"
    2 -> "二月"
    3 -> "三月"
    4 -> "四月"
    5 -> "五月"
    6 -> "六月"
    7 -> "七月"
    8 -> "八月"
    9 -> "九月"
    10 -> "十月"
    11 -> "十一月"
    12 -> "十二月"
    else -> "${month}月"
}

fun formatMonthSummaryStamp(year: Int, month: Int): String = "${year}年${month}月"

fun monthChineseName(month: Int): String = "${month}月"

fun formatPhotoDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("M月d日 · HH:mm", Locale.CHINA)
    return sdf.format(Date(millis))
}

fun formatShortTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
    return sdf.format(Date(millis))
}

fun daysStagedInVault(trashedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Int {
    if (trashedAtMillis == null) return 0
    return ((nowMillis - trashedAtMillis) / 86_400_000L).toInt().coerceAtLeast(0)
}

private val MACHINE_FILENAME_REGEX = Regex(
    pattern = "^(?:" +
        "(?:IMG|DSC|PXL|VID|MOV|MVIMG|Screenshot|Screenrecorder|wx_camera|mmexport|RPReplay|DJI)[-_\\d A-Za-z]*|" +
        "\\d{6,8}[_-]\\d{4,6}(?:[_-][A-Za-z0-9]+)*|" +
        "\\d{10,16}" +
        ")$",
    option = RegexOption.IGNORE_CASE
)

private val GENERIC_FOLDER_NAMES = setOf(
    "camera", "dcim", "screenshots", "pictures", "movies", "video", "videos", "download", "downloads"
)

fun isMachineGeneratedFilename(raw: String?): Boolean {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return true
    return MACHINE_FILENAME_REGEX.matches(trimmed)
}

fun formatTimeOfDayLabel(millis: Long): String {
    if (millis <= 0L) return "日常"
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 5..8 -> "清晨"
        in 9..11 -> "上午"
        in 12..16 -> "午后"
        in 17..19 -> "黄昏"
        else -> "夜晚"
    }
}

/**
 * Converts raw camera/system filenames (e.g. "20261109_091925", "IMG_20250812_183000")
 * into warm editorial titles while preserving user/curated captions untouched.
 */
fun formatEditorialPhotoTitle(photo: PhotoItemEntity): String {
    val raw = photo.caption.trim()
    if (raw.isNotEmpty() && !isMachineGeneratedFilename(raw)) {
        return raw
    }
    val lowerRaw = raw.lowercase(Locale.ROOT)
    val lowerBucket = photo.bucketName?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (lowerRaw.startsWith("screenshot") || lowerBucket == "screenshots") {
        return "屏幕截图 · ${photo.month}月${photo.day}日"
    }
    val timeMood = formatTimeOfDayLabel(photo.takenAtMillis)
    val mediaNoun = if (photo.isVideo) "动态影像" else "光影记录"
    val cleanLoc = photo.locationName?.trim()?.takeIf {
        it.isNotEmpty() && it.lowercase(Locale.ROOT) !in GENERIC_FOLDER_NAMES
    }
    return if (cleanLoc != null) {
        "$cleanLoc · ${timeMood}${mediaNoun}"
    } else {
        "${photo.month}月${photo.day}日 · ${timeMood}${mediaNoun}"
    }
}

fun formatVaultStagedLabel(trashedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
    val days = daysStagedInVault(trashedAtMillis, nowMillis)
    return if (days <= 0) "已暂存 <1 天" else "已暂存 $days 天"
}

/**
 * Clean, modern emerald completion seal badge for finished months (non-interactive status seal).
 */
@Composable
fun CleanedCompletionBadge(
    month: Int,
    freedBytes: Long,
    animateEntry: Boolean = false,
    showMonthPrefix: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale = remember(month, animateEntry) { Animatable(if (animateEntry) 1.6f else 1f) }
    val alpha = remember(month, animateEntry) { Animatable(if (animateEntry) 0f else 1f) }

    LaunchedEffect(month, animateEntry) {
        if (animateEntry) {
            alpha.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val statusDesc = if (freedBytes > 0L) {
        "${month}月已整理完成，待释放或已释放 ${formatStorageSize(freedBytes)}"
    } else {
        "${month}月已整理完成，全部保留"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EmeraldSoft,
        border = BorderStroke(1.2.dp, EmeraldKeep.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = statusDesc
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                rotationZ = -4f
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = EmeraldKeep,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = if (showMonthPrefix) "${month}月已整理" else "已整理",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = EmeraldKeep
                )
                Text(
                    text = if (freedBytes > 0L) "释放 ${formatStorageSize(freedBytes)}" else "全部保留",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = EmeraldKeep.copy(alpha = 0.85f)
                )
            }
        }
    }
}
