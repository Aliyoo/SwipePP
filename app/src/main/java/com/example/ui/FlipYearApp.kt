package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.MediaAccessLevel
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.monthChineseName
import com.example.ui.screens.ArcCleanerScreen
import com.example.ui.screens.BookshelfScreen
import com.example.ui.screens.FlipbookScreen
import com.example.ui.screens.FootprintMapScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.TrashVaultScreen
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.SurfacePure

val LocalFloatingDockInset = compositionLocalOf { 0.dp }

object FlipYearRoutes {
    const val BOOKSHELF = "bookshelf"
    const val GALLERY = "gallery"
    const val MAP = "footprint_map"
    const val VAULT = "trash_vault"
    const val FLIPBOOK = "flipbook/{year}"
    const val CLEANER = "arc_cleaner?year={year}&month={month}&day={day}&location={location}&includeReviewed={includeReviewed}"

    fun flipbook(year: Int) = "flipbook/$year"

    fun cleanerForYear(year: Int, includeReviewed: Boolean) =
        "arc_cleaner?year=$year&month=-1&day=-1&location=&includeReviewed=$includeReviewed"

    fun cleanerForMonth(year: Int, month: Int, includeReviewed: Boolean, focusDay: Int? = null) =
        "arc_cleaner?year=$year&month=$month&day=${focusDay ?: -1}&location=&includeReviewed=$includeReviewed"

    fun cleanerForLocation(location: String, includeReviewed: Boolean) =
        "arc_cleaner?year=-1&month=-1&day=-1&location=${Uri.encode(location)}&includeReviewed=$includeReviewed"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun FlipYearApp(
    viewModel: FlipYearViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            viewModel.refreshPermissionAndScan(silent = false)
        }
    )

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.finalizePurgeTrashed()
            }
        }
    )

    val systemWriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.confirmPendingBatchWrite(result.resultCode == Activity.RESULT_OK)
        }
    )

    val launchWriteIfNeeded: (PendingIntent?) -> Unit = { pendingIntent ->
        if (pendingIntent != null) {
            systemWriteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

    var promptedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.mediaAccessLevel) {
        if (!promptedOnce && uiState.mediaAccessLevel == MediaAccessLevel.NONE) {
            promptedOnce = true
            mediaPermissionLauncher.launch(viewModel.requiredMediaPermissions())
        }
    }

    val requestMediaAccess = {
        mediaPermissionLauncher.launch(viewModel.requiredMediaPermissions())
    }

    val confirmVaultPurge = {
        val pendingIntent = viewModel.buildSystemTrashIntentForVault()
        if (pendingIntent != null) {
            systemTrashLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else {
            viewModel.finalizePurgeTrashed()
        }
    }

    val tabs = remember {
        listOf(
            TopLevelTab(
                route = FlipYearRoutes.BOOKSHELF,
                labelRes = R.string.nav_bookshelf,
                selectedIcon = Icons.Filled.AutoStories,
                unselectedIcon = Icons.Outlined.AutoStories,
                testTag = "nav_tab_bookshelf"
            ),
            TopLevelTab(
                route = FlipYearRoutes.GALLERY,
                labelRes = R.string.nav_gallery,
                selectedIcon = Icons.Filled.BurstMode,
                unselectedIcon = Icons.Outlined.BurstMode,
                testTag = "nav_tab_gallery"
            ),
            TopLevelTab(
                route = FlipYearRoutes.MAP,
                labelRes = R.string.nav_map,
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                testTag = "nav_tab_map"
            ),
            TopLevelTab(
                route = FlipYearRoutes.VAULT,
                labelRes = R.string.nav_vault,
                selectedIcon = Icons.Filled.DeleteSweep,
                unselectedIcon = Icons.Outlined.DeleteSweep,
                testTag = "nav_tab_vault"
            )
        )
    }

    val showTopLevelNav = currentRoute in setOf(
        FlipYearRoutes.BOOKSHELF,
        FlipYearRoutes.GALLERY,
        FlipYearRoutes.MAP,
        FlipYearRoutes.VAULT
    )

    var isGallerySelectionMode by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != FlipYearRoutes.GALLERY) {
            isGallerySelectionMode = false
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioCanvas)
    ) {
        val useNavigationRail = maxWidth >= 640.dp
        val floatingDockInset = if (showTopLevelNav && !useNavigationRail) 82.dp else 0.dp

        CompositionLocalProvider(LocalFloatingDockInset provides floatingDockInset) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-bleed edge-to-edge content layer (extends completely behind the floating dock)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail(
                            containerColor = SurfacePure,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        trashedCount = uiState.trashedPhotos.size,
                                        tint = if (selected) CobaltPrimary else InkSecondary
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CobaltPrimary,
                                    selectedTextColor = CobaltPrimary,
                                    indicatorColor = CobaltSoft,
                                    unselectedIconColor = InkSecondary,
                                    unselectedTextColor = InkSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = FlipYearRoutes.BOOKSHELF,
                        enterTransition = {
                            fadeIn(animationSpec = tween(220)) + scaleIn(
                                initialScale = 0.97f,
                                animationSpec = tween(220)
                            )
                        },
                        exitTransition = { fadeOut(animationSpec = tween(160)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                        popExitTransition = { fadeOut(animationSpec = tween(160)) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(FlipYearRoutes.BOOKSHELF) {
                            BookshelfScreen(
                                uiState = uiState,
                                onOpenYearbook = { year ->
                                    navController.navigate(FlipYearRoutes.flipbook(year))
                                },
                                onStartYearCleanup = { year, includeReviewed ->
                                    navController.navigate(FlipYearRoutes.cleanerForYear(year, includeReviewed))
                                },
                                onOpenVault = {
                                    navigateToTab(FlipYearRoutes.VAULT)
                                },
                                onRequestMediaPermission = requestMediaAccess,
                                onRescanDeviceMedia = { viewModel.scanDeviceMedia(silent = false) },
                                onSelectFilter = viewModel::setMediaFilter,
                                onLoadDemoArchive = viewModel::loadDemoArchive
                            )
                        }

                        composable(FlipYearRoutes.GALLERY) {
                            GalleryScreen(
                                uiState = uiState,
                                onSelectBestInBurst = viewModel::selectBestInStack,
                                onKeepBestInBurst = viewModel::keepBestInGalleryBurst,
                                onKeepPhoto = viewModel::keepPhoto,
                                onTrashPhoto = viewModel::swipeTrashPhoto,
                                onLoadDemoArchive = viewModel::loadDemoArchive,
                                onBulkTrashPhotos = viewModel::bulkTrashPhotos,
                                onBulkKeepPhotos = viewModel::bulkKeepPhotos,
                                onBulkMovePhotos = viewModel::bulkMovePhotos,
                                onBatchCompress = { photos, preset ->
                                    launchWriteIfNeeded(viewModel.requestBatchCompress(photos, preset))
                                },
                                onBatchManualLocation = { photos, name, lat, lon, writeExif ->
                                    launchWriteIfNeeded(
                                        viewModel.requestBatchManualLocation(photos, name, lat, lon, writeExif)
                                    )
                                },
                                onBatchSameDayLocation = { matches, writeExif ->
                                    launchWriteIfNeeded(
                                        viewModel.requestBatchSameDayLocation(matches, writeExif)
                                    )
                                },
                                onGeocodeLocation = viewModel::geocodeLocationQuery,
                                onSelectionModeChanged = { isGallerySelectionMode = it }
                            )
                        }

                        composable(FlipYearRoutes.MAP) {
                            FootprintMapScreen(
                                uiState = uiState,
                                onCleanLocation = { locName, includeReviewed ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForLocation(locName, includeReviewed)
                                    )
                                },
                                onKeepPhoto = viewModel::keepPhoto,
                                onTrashPhoto = viewModel::swipeTrashPhoto,
                                onRequestMediaLocationPermission = requestMediaAccess,
                                onTriggerGpsBackfill = { forceRescan ->
                                    viewModel.startFullGpsBackfill(
                                        notifyResult = forceRescan,
                                        forceRescan = forceRescan
                                    )
                                },
                                onBatchManualLocation = { photos, name, lat, lon, writeExif ->
                                    launchWriteIfNeeded(
                                        viewModel.requestBatchManualLocation(photos, name, lat, lon, writeExif)
                                    )
                                },
                                onBatchSameDayLocation = { matches, writeExif ->
                                    launchWriteIfNeeded(
                                        viewModel.requestBatchSameDayLocation(matches, writeExif)
                                    )
                                },
                                onGeocodeLocation = viewModel::geocodeLocationQuery
                            )
                        }

                        composable(FlipYearRoutes.VAULT) {
                            TrashVaultScreen(
                                uiState = uiState,
                                onRestorePhoto = viewModel::restoreFromVault,
                                onRestoreAll = viewModel::restoreAllFromVault,
                                onConfirmPurgeAll = confirmVaultPurge
                            )
                        }

                        composable(
                            route = FlipYearRoutes.FLIPBOOK,
                            arguments = listOf(navArgument("year") { type = NavType.IntType })
                        ) { entry ->
                            val year = entry.arguments?.getInt("year") ?: 2026
                            val pages = remember(year, uiState.allPhotos, uiState.monthStamps) {
                                viewModel.getMonthPagesForYear(year, uiState)
                            }
                            FlipbookScreen(
                                year = year,
                                monthPages = pages,
                                recentlyStampedMonthKey = uiState.recentlyStampedMonthKey,
                                onBack = { navController.popBackStack() },
                                onStartMonthCleanup = { y, m, includeReviewed, focusDay ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForMonth(y, m, includeReviewed, focusDay)
                                    )
                                },
                                onSelectBestInBurst = viewModel::selectBestInStack,
                                onKeepBestInBurst = viewModel::keepBestInGalleryBurst,
                                onKeepPhoto = viewModel::keepPhoto,
                                onTrashPhoto = viewModel::swipeTrashPhoto
                            )
                        }

                        composable(
                            route = FlipYearRoutes.CLEANER,
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("month") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("day") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("location") { type = NavType.StringType; defaultValue = "" },
                                navArgument("includeReviewed") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { entry ->
                            val yearArg = entry.arguments?.getInt("year")?.takeIf { it > 0 }
                            val monthArg = entry.arguments?.getInt("month")?.takeIf { it in 1..12 }
                            val dayArg = entry.arguments?.getInt("day")?.takeIf { it in 1..31 }
                            val locArg = entry.arguments?.getString("location")?.takeIf { it.isNotBlank() }
                            val includeReviewed = entry.arguments?.getBoolean("includeReviewed") ?: false

                            val queue = remember(yearArg, monthArg, locArg, includeReviewed, uiState.allPhotos) {
                                viewModel.buildArcQueue(
                                    year = yearArg,
                                    month = monthArg,
                                    locationName = locArg,
                                    includeReviewed = includeReviewed,
                                    state = uiState
                                )
                            }

                            val headerTitle = when {
                                locArg == UNLOCATED_LOCATION_KEY -> "清理无定位媒体"
                                locArg != null -> "清理足迹 · $locArg"
                                yearArg != null && monthArg != null -> "整理 ${yearArg}年${monthChineseName(monthArg)}"
                                yearArg != null -> if (includeReviewed) "重温 ${yearArg}年影集" else "整理 ${yearArg}年影集"
                                else -> "整理媒体"
                            }

                            ArcCleanerScreen(
                                title = headerTitle,
                                monthNumber = monthArg,
                                initialFocusDay = dayArg,
                                queue = queue,
                                pendingTrashBytes = uiState.pendingTrashBytes,
                                undoStackDepth = uiState.undoStackDepth,
                                onBack = { navController.popBackStack() },
                                onKeepPhoto = viewModel::keepPhoto,
                                onTrashPhoto = viewModel::swipeTrashPhoto,
                                onKeepBestInStack = viewModel::keepBestAndTrashStackOthers,
                                onTrashEntireStack = viewModel::trashEntireStack,
                                onSelectBestInStack = viewModel::selectBestInStack,
                                onUndo = viewModel::undoLastAction
                            )
                        }
                    }

                    // Live Batch Task Progress Pill (compression / EXIF GPS write)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.batchTaskMessage != null,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                            .testTag("batch_task_progress_pill")
                    ) {
                        uiState.batchTaskMessage?.let { taskMsg ->
                            ObsidianGlassCard(
                                shape = RoundedCornerShape(22.dp),
                                shadowElevation = 14.dp,
                                modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = CobaltPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = taskMsg,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                    if (uiState.batchTaskTotal > 0) {
                                        val fraction = (uiState.batchTaskProcessed.toFloat() / uiState.batchTaskTotal.toFloat())
                                            .coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            color = CobaltPrimary,
                                            trackColor = Color.White.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.feedbackBanner != null && uiState.batchTaskMessage == null,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                    ) {
                        uiState.feedbackBanner?.let { message ->
                            ObsidianGlassCard(
                                onClick = viewModel::dismissBanner,
                                shape = RoundedCornerShape(50),
                                shadowElevation = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = if (uiState.undoStackDepth > 0) 8.dp else 16.dp,
                                        top = 8.dp,
                                        bottom = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (uiState.undoStackDepth > 0) {
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.undoLastAction(triggeredByShake = false)
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = CobaltPrimary,
                                            modifier = Modifier.testTag("banner_undo_button")
                                        ) {
                                            Text(
                                                text = stringResource(R.string.action_undo),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Truly floating pill bar overlaid directly above full-bleed content (zero opaque dock background)
                AnimatedVisibility(
                    visible = showTopLevelNav && !useNavigationRail && !isGallerySelectionMode,
                    enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(160)) + slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(160)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    FloatingGalleryDock(
                        tabs = tabs,
                        currentRoute = currentRoute,
                        trashedCount = uiState.trashedPhotos.size,
                        onSelectTab = navigateToTab
                    )
                }
            }
        }
    }
}

/**
 * Truly floating translucent frosted-glass capsule navigation dock overlaid directly on top of
 * full-bleed maps, photo grids, and backdrops—no opaque dock strip behind it, and side margins
 * pass touches straight through to the underlying canvas.
 */
@Composable
private fun FloatingGalleryDock(
    tabs: List<TopLevelTab>,
    currentRoute: String?,
    trashedCount: Int,
    onSelectTab: (String) -> Unit
) {
    val activeObsidianBrush = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF262A36), Color(0xFF10131A)))
    }

    FrostedGlassCard(
        shape = RoundedCornerShape(32.dp),
        topColor = Color(0xDEFFFFFF),
        bottomColor = Color(0xCCF7F5F0),
        shadowElevation = 18.dp,
        enableAmbientBlur = true,
        modifier = Modifier
            .widthIn(max = 396.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Color.White else InkSecondary,
                    animationSpec = tween(200),
                    label = "dock_content_color"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.background(activeObsidianBrush)
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onSelectTab(tab.route) }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        .padding(horizontal = if (selected) 16.dp else 14.dp)
                        .testTag(tab.testTag)
                ) {
                    TabIconWithBadge(
                        tab = tab,
                        selected = selected,
                        trashedCount = trashedCount,
                        tint = contentColor
                    )
                    AnimatedVisibility(visible = selected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: TopLevelTab,
    selected: Boolean,
    trashedCount: Int,
    tint: Color
) {
    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab.route == FlipYearRoutes.VAULT && trashedCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = CoralDanger,
                    contentColor = Color.White
                ) {
                    Text(trashedCount.toString())
                }
            }
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = stringResource(tab.labelRes),
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = stringResource(tab.labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
