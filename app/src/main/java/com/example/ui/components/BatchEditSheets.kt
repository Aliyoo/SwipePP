package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CURATED_LOCATION_PRESETS
import com.example.data.CompressionPreset
import com.example.data.LocationPreset
import com.example.data.PhotoItemEntity
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.ui.LocationCluster
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCompressBottomSheet(
    selectedPhotos: List<PhotoItemEntity>,
    onDismiss: () -> Unit,
    onConfirmCompress: (preset: CompressionPreset) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPreset by remember { mutableStateOf(CompressionPreset.NEAR_LOSSLESS) }

    val imageTargets = remember(selectedPhotos) { selectedPhotos.filter { !it.isVideo } }
    val skippedVideoCount = selectedPhotos.size - imageTargets.size
    val totalImageBytes = remember(imageTargets) { imageTargets.sumOf { it.fileSizeBytes } }
    val estimatedSavedBytes = (totalImageBytes * selectedPreset.estimatedSavingsRatio).toLong()
    val estimatedAfterBytes = (totalImageBytes - estimatedSavedBytes).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("batch_compress_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.batch_compress_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = if (skippedVideoCount > 0) {
                            "${imageTargets.size} 张可压缩照片 · 自动跳过 $skippedVideoCount 个视频"
                        } else {
                            "已选 ${imageTargets.size} 张照片 · 当前共 ${formatStorageSize(totalImageBytes)}"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        color = InkSecondary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EmeraldSoft,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Compress,
                            contentDescription = null,
                            tint = EmeraldKeep,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Live estimated savings highlight card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EmeraldSoft.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "预计可节省体积",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                        Text(
                            text = "约 ${formatStorageSize(estimatedSavedBytes)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = EmeraldKeep
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "压缩后预估",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                        Text(
                            text = "${formatStorageSize(totalImageBytes)} → ${formatStorageSize(estimatedAfterBytes)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = InkPrimary
                        )
                    }
                }
            }

            // 3 Curated Quality Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompressionPreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    val percentSaved = (preset.estimatedSavingsRatio * 100).toInt()
                    Surface(
                        onClick = { selectedPreset = preset },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) CobaltSoft.copy(alpha = 0.72f) else SurfaceMuted.copy(alpha = 0.48f),
                        border = BorderStroke(
                            width = if (isSelected) 1.4.dp else 1.dp,
                            color = if (isSelected) CobaltPrimary else HairlineBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compress_preset_${preset.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) CobaltPrimary else InkTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = InkPrimary
                                    )
                                    if (preset == CompressionPreset.NEAR_LOSSLESS) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = CobaltPrimary
                                        ) {
                                            Text(
                                                text = "推荐",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                            Text(
                                text = "-$percentSaved%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) CobaltDark else EmeraldKeep
                            )
                        }
                    }
                }
            }

            // Safety guarantee note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceMuted.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.batch_compress_safety_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }

            GalleryActionButton(
                text = if (imageTargets.isNotEmpty()) {
                    "开始压缩 ${imageTargets.size} 张照片 (${selectedPreset.title})"
                } else {
                    "所选项目中暂无可压缩照片"
                },
                leadingIcon = Icons.Filled.Compress,
                tone = GalleryButtonTone.OBSIDIAN,
                enabled = imageTargets.isNotEmpty(),
                onClick = { onConfirmCompress(selectedPreset) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_batch_compress_button")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchLocationEditorSheet(
    targetPhotos: List<PhotoItemEntity>,
    existingClusters: List<LocationCluster>,
    sameDayMatches: List<SameDayLocationMatch>,
    onResolveQuery: suspend (String) -> ResolvedGeoLocation?,
    onApplySameDayInference: (List<SameDayLocationMatch>, writeExif: Boolean) -> Unit,
    onConfirmManualLocation: (locationName: String, latitude: Double, longitude: Double, writeExif: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val quickSuggestions = remember(existingClusters) {
        val fromClusters = existingClusters.map {
            LocationPreset(it.locationName, it.latitude, it.longitude)
        }
        val existingNames = fromClusters.mapTo(HashSet()) { it.name }
        fromClusters + CURATED_LOCATION_PRESETS.filter { it.name !in existingNames }
    }

    val defaultPreset = quickSuggestions.firstOrNull() ?: CURATED_LOCATION_PRESETS.first()
    var locationInput by remember { mutableStateOf(defaultPreset.name) }
    var selectedLat by remember { mutableStateOf(defaultPreset.latitude) }
    var selectedLon by remember { mutableStateOf(defaultPreset.longitude) }
    var isResolving by remember { mutableStateOf(false) }
    var geocodeHint by remember { mutableStateOf<String?>(null) }
    var writeExifToFile by remember { mutableStateOf(true) }

    val missingLocationCount = remember(targetPhotos) {
        targetPhotos.count {
            it.locationName.isNullOrBlank() || it.latitude == null || it.longitude == null
        }
    }
    val hasRealDeviceImages = remember(targetPhotos) {
        targetPhotos.any { !it.isSample && !it.isVideo && !it.contentUri.isNullOrBlank() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("batch_location_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.batch_location_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = "共 ${targetPhotos.size} 项媒体 · 其中 $missingLocationCount 项缺失定位",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        color = InkSecondary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = SolarAmberSoft,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.EditLocationAlt,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Smart Same-Day GPS Inference Card (one-tap fill from nearest geotagged photo within 24h)
            if (sameDayMatches.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CobaltSoft.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "智能按同日照片推断定位",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = InkPrimary
                                )
                                val previewCities = sameDayMatches
                                    .map { it.matchedLocationName }
                                    .distinct()
                                    .take(3)
                                    .joinToString("、")
                                Text(
                                    text = "可自动为 ${sameDayMatches.size} 项匹配邻近24小时内拍摄地点（$previewCities）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary
                                )
                            }
                        }

                        GalleryActionButton(
                            text = "一键智能补全 ${sameDayMatches.size} 项定位",
                            leadingIcon = Icons.Filled.MyLocation,
                            tone = GalleryButtonTone.OBSIDIAN,
                            height = 44.dp,
                            shape = RoundedCornerShape(14.dp),
                            onClick = { onApplySameDayInference(sameDayMatches, writeExifToFile) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_same_day_location_button")
                        )
                    }
                }
            }

            // Quick-Pick Existing Footprints + Curated Cities
            Text(
                text = "快速选择地点（已有足迹与常用城市）",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickSuggestions, key = { it.name }) { preset ->
                    val selected = locationInput.trim() == preset.name
                    GallerySelectableChip(
                        selected = selected,
                        onClick = {
                            locationInput = preset.name
                            selectedLat = preset.latitude
                            selectedLon = preset.longitude
                            geocodeHint = null
                        },
                        modifier = Modifier.testTag("location_preset_${preset.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = if (selected) SolarAmber else CobaltPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else InkPrimary
                        )
                    }
                }
            }

            // Custom City / Landmark Search + Real Geocoder Resolution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = {
                        locationInput = it
                        geocodeHint = null
                    },
                    singleLine = true,
                    label = { Text("自定义地点或城市") },
                    placeholder = { Text("如：大理洱海、京都清水寺") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = CobaltPrimary
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CobaltPrimary,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = SurfaceMuted.copy(alpha = 0.45f),
                        unfocusedContainerColor = SurfaceMuted.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_location_input")
                )

                Surface(
                    onClick = {
                        if (!isResolving && locationInput.isNotBlank()) {
                            scope.launch {
                                isResolving = true
                                val resolved = onResolveQuery(locationInput)
                                isResolving = false
                                if (resolved != null) {
                                    selectedLat = resolved.latitude
                                    selectedLon = resolved.longitude
                                    geocodeHint = "已解析坐标"
                                } else {
                                    geocodeHint = "未联网解析到新坐标，将沿用当前选中经纬度"
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = CobaltSoft,
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("batch_location_resolve_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "解析坐标",
                                    tint = CobaltDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "定位",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CobaltDark
                                )
                            }
                        }
                    }
                }
            }

            // Coordinate readout
            val latDir = if (selectedLat >= 0) "N" else "S"
            val lonDir = if (selectedLon >= 0) "E" else "W"
            Text(
                text = buildString {
                    append(
                        String.format(
                            Locale.US,
                            "目标坐标：%.4f°%s, %.4f°%s",
                            abs(selectedLat),
                            latDir,
                            abs(selectedLon),
                            lonDir
                        )
                    )
                    if (!geocodeHint.isNullOrBlank()) {
                        append(" · $geocodeHint")
                    }
                },
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = InkSecondary
            )

            if (hasRealDeviceImages) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceMuted.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { writeExifToFile = !writeExifToFile }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "同步写入照片 EXIF 定位信息",
                                style = MaterialTheme.typography.labelLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = "EXIF 是照片自带的拍摄参数与经纬度信息；开启后会一并写入相册原图（系统将弹窗授权一次）",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = writeExifToFile,
                            onCheckedChange = { writeExifToFile = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltPrimary
                            ),
                            modifier = Modifier.testTag("batch_location_exif_switch")
                        )
                    }
                }
            }

            GalleryActionButton(
                text = "应用至 ${targetPhotos.size} 项媒体 (${locationInput.trim().ifBlank { "请选择地点" }})",
                leadingIcon = Icons.Filled.EditLocationAlt,
                tone = GalleryButtonTone.OBSIDIAN,
                enabled = locationInput.isNotBlank() && targetPhotos.isNotEmpty(),
                onClick = {
                    val trimmed = locationInput.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirmManualLocation(trimmed, selectedLat, selectedLon, writeExifToFile)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_batch_location_button")
            )
        }
    }
}
