package com.example.ui.screens.cleaner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CleanerEntrySource
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySculptedHeroButton
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber

@Composable
internal fun CleanupCompletedCelebration(
    title: String,
    monthNumber: Int?,
    completedDay: Int?,
    remainingInFullQueue: Int,
    sessionKeptCount: Int,
    sessionFavoritedCount: Int,
    sessionTrashedCount: Int,
    sessionTrashedBytes: Long,
    entrySource: CleanerEntrySource,
    onContinueFullMonth: () -> Unit,
    onOpenVault: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSingleDayFinishedWithMoreInMonth = completedDay != null && remainingInFullQueue > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("cleanup_completion_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CleanedCompletionBadge(
            month = monthNumber ?: 12,
            freedBytes = sessionTrashedBytes,
            animateEntry = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = when {
                isSingleDayFinishedWithMoreInMonth && monthNumber != null ->
                    "${monthChineseName(monthNumber)} ${completedDay}日已整理完成"
                monthNumber != null ->
                    "${monthChineseName(monthNumber)}整理完成"
                else -> "$title 完成"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Column Monospace Telemetry Dashboard (aligned with BookshelfStorageLedgerSheet)
        FrostedGlassCard(
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag("completion_session_stats")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompletionMetricSlot(
                    label = "保留",
                    value = "$sessionKeptCount",
                    subLabel = "已入册",
                    accent = EmeraldKeep,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(34.dp),
                    thickness = 1.dp,
                    color = HairlineBorder.copy(alpha = 0.7f)
                )
                CompletionMetricSlot(
                    label = "收藏",
                    value = "$sessionFavoritedCount",
                    subLabel = "精选佳作",
                    accent = SolarAmber,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(34.dp),
                    thickness = 1.dp,
                    color = HairlineBorder.copy(alpha = 0.7f)
                )
                CompletionMetricSlot(
                    label = "移入回收站",
                    value = formatStorageSize(sessionTrashedBytes),
                    subLabel = "$sessionTrashedCount 项待清",
                    accent = CoralDanger,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isSingleDayFinishedWithMoreInMonth) {
                "当天媒体已全部处理完毕，本月其余日期还有 $remainingInFullQueue 项待整理。"
            } else if (sessionTrashedCount > 0) {
                "移入回收站的项目尚未从设备删除，前往回收站确认后即可释放空间。"
            } else {
                "本次整理的照片和视频均已妥善保留在影集中。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (isSingleDayFinishedWithMoreInMonth) {
            GallerySculptedHeroButton(
                title = "继续清理本月剩余",
                leadingIcon = Icons.Filled.CleaningServices,
                badgeText = "$remainingInFullQueue 项",
                onClick = onContinueFullMonth,
                tone = GalleryButtonTone.OBSIDIAN,
                height = 52.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_full_month_button")
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (sessionTrashedCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(sessionTrashedBytes)})",
                    leadingIcon = Icons.Filled.DeleteOutline,
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.DANGER_GLASS,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cleanup_done_button")
                ) {
                    Text(
                        text = entrySource.backLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = InkSecondary
                    )
                }
            } else {
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.GLASS,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        } else {
            if (sessionTrashedCount > 0) {
                GallerySculptedHeroButton(
                    title = "去回收站释放空间",
                    leadingIcon = Icons.Filled.DeleteOutline,
                    badgeText = formatStorageSize(sessionTrashedBytes),
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 52.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(10.dp))
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.GLASS,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            } else {
                GallerySculptedHeroButton(
                    title = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 52.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        }
    }
}

@Composable
private fun CompletionMetricSlot(
    label: String,
    value: String,
    subLabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = InkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            color = InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
