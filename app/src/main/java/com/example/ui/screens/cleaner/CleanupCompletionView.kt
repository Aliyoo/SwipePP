package com.example.ui.screens.cleaner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.CleanerEntrySource
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfacePure

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
        if (monthNumber != null && !isSingleDayFinishedWithMoreInMonth) {
            CleanedCompletionBadge(
                month = monthNumber,
                freedBytes = sessionTrashedBytes,
                animateEntry = true
            )
        } else {
            CleanedCompletionBadge(
                month = monthNumber ?: 12,
                freedBytes = sessionTrashedBytes,
                animateEntry = true
            )
        }

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
            color = InkPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfacePure,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag("completion_session_stats")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "本次整理统计",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Text(
                    text = "保留 $sessionKeptCount · 收藏 $sessionFavoritedCount · 移入回收站 $sessionTrashedCount（待释放 ${formatStorageSize(sessionTrashedBytes)}）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSingleDayFinishedWithMoreInMonth) {
                "当天媒体已全部处理完毕，本月其余日期还有 $remainingInFullQueue 项待整理。"
            } else if (sessionTrashedCount > 0) {
                "移入回收站的项目尚未从设备删除，前往回收站确认后即可释放空间。"
            } else {
                "本次整理的照片和视频均已妥善保留在影集中。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (isSingleDayFinishedWithMoreInMonth) {
            GalleryActionButton(
                text = "继续整理本月剩余 $remainingInFullQueue 项",
                onClick = onContinueFullMonth,
                tone = GalleryButtonTone.OBSIDIAN,
                height = 50.dp,
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
                Spacer(modifier = Modifier.height(10.dp))
            }
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
            if (sessionTrashedCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(sessionTrashedBytes)})",
                    leadingIcon = Icons.Filled.DeleteOutline,
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 50.dp,
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
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 50.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        }
    }
}
