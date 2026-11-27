package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.State
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
import com.example.data.PhotoItemEntity
import com.example.ui.components.DecoPosterPanel
import com.example.ui.components.DecoTabIcon
import com.example.ui.components.DecoTabIconKind
import com.example.ui.components.FullScreenOverlayHost
import com.example.ui.components.FullScreenOverlayLayer
import com.example.ui.components.LocalFullScreenOverlayHost
import com.example.ui.components.OnboardingGestureGuideOverlay
import com.example.ui.components.monthChineseName
import com.example.ui.screens.ArcCleanerScreen
import com.example.ui.screens.BookshelfScreen
import com.example.ui.screens.FlipbookScreen
import com.example.ui.screens.FootprintMapScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.TrashVaultScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.UserPreferences
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoVermInk
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DecoGround
import com.example.ui.theme.ThemePreferences
import kotlin.math.roundToInt
import com.example.ui.theme.YearbookCoverStyle

val LocalFloatingDockInset = compositionLocalOf { 0.dp }

/** 停靠栏顶缘在窗口坐标系下的实测绝对 Y（px）；未测量时为 NaN。画框等元素以此为锚。 */
val LocalDockTopYPx = compositionLocalOf { Float.NaN }

object FlipYearRoutes {
    const val BOOKSHELF = "bookshelf"
    const val GALLERY = "gallery"
    const val MAP = "footprint_map"
    const val VAULT = "trash_vault"
    const val FLIPBOOK = "flipbook/{year}"
    const val CLEANER = "arc_cleaner?year={year}&month={month}&day={day}&location={location}&includeReviewed={includeReviewed}&source={source}&custom={custom}"

    fun flipbook(year: Int) = "flipbook/$year"

    fun cleanerForYear(
        year: Int,
        includeReviewed: Boolean,
        source: CleanerEntrySource = CleanerEntrySource.BOOKSHELF
    ) = "arc_cleaner?year=$year&month=-1&day=-1&location=&includeReviewed=$includeReviewed&source=${source.routeKey}&custom=false"

    fun cleanerForMonth(
        year: Int,
        month: Int,
        includeReviewed: Boolean,
        focusDay: Int? = null,
        source: CleanerEntrySource = CleanerEntrySource.FLIPBOOK
    ) = "arc_cleaner?year=$year&month=$month&day=${focusDay ?: -1}&location=&includeReviewed=$includeReviewed&source=${source.routeKey}&custom=false"

    fun cleanerForLocation(
        location: String,
        includeReviewed: Boolean,
        source: CleanerEntrySource = CleanerEntrySource.MAP
    ) = "arc_cleaner?year=-1&month=-1&day=-1&location=${Uri.encode(location)}&includeReviewed=$includeReviewed&source=${source.routeKey}&custom=false"

    fun cleanerForCustom(
        includeReviewed: Boolean = true,
        source: CleanerEntrySource = CleanerEntrySource.GALLERY
    ) = "arc_cleaner?year=-1&month=-1&day=-1&location=&includeReviewed=$includeReviewed&source=${source.routeKey}&custom=true"
}

private val TOP_LEVEL_ROUTE_ORDER = listOf(
    FlipYearRoutes.BOOKSHELF,
    FlipYearRoutes.GALLERY,
    FlipYearRoutes.MAP,
    FlipYearRoutes.VAULT
)

private val TOP_LEVEL_ROUTES = TOP_LEVEL_ROUTE_ORDER.toSet()

private fun topLevelRouteIndex(route: String?): Int = TOP_LEVEL_ROUTE_ORDER.indexOf(route)

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val iconKind: DecoTabIconKind,
    val testTag: String
)

@Composable
fun FlipYearApp(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    userPreferences: UserPreferences = UserPreferences(themeMode = themeMode),
    onSelectThemeMode: (AppThemeMode) -> Unit = {},
    onSelectTimelineSortOrder: (TimelineSortOrder) -> Unit = {},
    onToggleShowEmptyMonths: (Boolean) -> Unit = {},
    onSelectFlipbookDefaultView: (FlipbookDefaultView) -> Unit = {},
    onSelectDefaultStartTab: (DefaultStartTab) -> Unit = {},
    onSelectGalleryGridDensity: (GalleryGridDensity) -> Unit = {},
    onSelectYearbookCoverStyle: (YearbookCoverStyle) -> Unit = {},
    viewModel: FlipYearViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(userPreferences.timelineSortOrder) {
        viewModel.setTimelineSortOrder(userPreferences.timelineSortOrder)
    }
    LaunchedEffect(userPreferences.yearbookCoverStyle) {
        viewModel.setDefaultYearbookCoverStyle(userPreferences.yearbookCoverStyle)
    }

    val initialStartDestination = remember { userPreferences.defaultStartTab.route }

    val onboardingPrefs = remember(context) { ThemePreferences(context) }
    val hasSeenOnboarding by onboardingPrefs.hasSeenOnboardingGuide.collectAsStateWithLifecycle()

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            viewModel.refreshPermissionAndScan(silent = false)
        }
    )

    var pendingPurgeTargets by remember { mutableStateOf<List<PhotoItemEntity>>(emptyList()) }

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            val targets = pendingPurgeTargets
            pendingPurgeTargets = emptyList()
            if (result.resultCode == Activity.RESULT_OK && targets.isNotEmpty()) {
                viewModel.finalizePurgeTrashed(targets)
            }
        }
    )

    var pendingLegacyConsentSender by remember { mutableStateOf<android.content.IntentSender?>(null) }

    val legacyDeleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.continuePendingLegacyPurge(
                granted = result.resultCode == Activity.RESULT_OK,
                onNeedRecoverableConsent = { nextSender ->
                    pendingLegacyConsentSender = nextSender
                }
            )
        }
    )

    LaunchedEffect(pendingLegacyConsentSender) {
        val sender = pendingLegacyConsentSender ?: return@LaunchedEffect
        pendingLegacyConsentSender = null
        legacyDeleteConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    val launchLegacyConsent: (android.content.IntentSender) -> Unit = { sender ->
        pendingLegacyConsentSender = sender
    }

    val systemWriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.confirmPendingBatchWrite(result.resultCode == Activity.RESULT_OK)
        }
    )

    val systemFavoriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.confirmPendingFavoriteConsent(result.resultCode == Activity.RESULT_OK)
        }
    )
    val pendingFavoriteConsent by viewModel.pendingFavoriteConsent.collectAsStateWithLifecycle()
    LaunchedEffect(pendingFavoriteConsent) {
        val request = pendingFavoriteConsent ?: return@LaunchedEffect
        systemFavoriteLauncher.launch(IntentSenderRequest.Builder(request.pendingIntent.intentSender).build())
    }

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

    val confirmVaultPurgeTargets: (List<PhotoItemEntity>) -> Unit = { targets ->
        if (targets.isNotEmpty()) {
            val hasDeviceItems = targets.any { !it.isSample && !it.contentUri.isNullOrBlank() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = viewModel.buildSystemTrashIntentForVault(targets)
                if (pendingIntent != null) {
                    pendingPurgeTargets = targets
                    systemTrashLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    viewModel.finalizePurgeTrashed(targets)
                }
            } else if (hasDeviceItems) {
                // Stage 1.1: Real per-item ContentResolver.delete + API 29 RecoverableSecurityException consent
                viewModel.purgeLegacyPhotos(
                    targets = targets,
                    onNeedRecoverableConsent = launchLegacyConsent
                )
            } else {
                viewModel.finalizePurgeTrashed(targets)
            }
        }
    }

    val tabs = remember {
        listOf(
            TopLevelTab(
                route = FlipYearRoutes.BOOKSHELF,
                labelRes = R.string.nav_bookshelf,
                iconKind = DecoTabIconKind.ALBUM,
                testTag = "nav_tab_bookshelf"
            ),
            TopLevelTab(
                route = FlipYearRoutes.GALLERY,
                labelRes = R.string.nav_gallery,
                iconKind = DecoTabIconKind.GALLERY,
                testTag = "nav_tab_gallery"
            ),
            TopLevelTab(
                route = FlipYearRoutes.MAP,
                labelRes = R.string.nav_map,
                iconKind = DecoTabIconKind.FOOTPRINT,
                testTag = "nav_tab_map"
            ),
            TopLevelTab(
                route = FlipYearRoutes.VAULT,
                labelRes = R.string.nav_vault,
                iconKind = DecoTabIconKind.VAULT,
                testTag = "nav_tab_vault"
            )
        )
    }

    val showTopLevelNav = currentRoute in TOP_LEVEL_ROUTES

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
            // 画报纸地墨绿作为 App 根底色：转场缝隙/overscroll 不再漏出旧 StudioCanvas 色
            .background(DecoGround)
    ) {
        val useNavigationRail = maxWidth >= 640.dp
        val floatingDockInset = if (showTopLevelNav && !useNavigationRail) 82.dp else 0.dp
        val fullScreenOverlayHost = remember { FullScreenOverlayHost() }
        // 停靠栏顶缘实测 Y（px，窗口坐标系）；由 dock 自身 onGloballyPositioned 回写
        val dockTopYPx = remember { mutableStateOf(Float.NaN) }

        CompositionLocalProvider(
            LocalFloatingDockInset provides floatingDockInset,
            LocalDockTopYPx provides dockTopYPx.value,
            LocalFullScreenOverlayHost provides fullScreenOverlayHost
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-bleed edge-to-edge content layer (extends completely behind the floating dock)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail(
                            containerColor = DecoGround,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val railGold = DecoGold
                            val tabLabel = stringResource(tab.labelRes)
                            val railDesc = if (tab.route == FlipYearRoutes.VAULT && uiState.trashedPhotos.isNotEmpty()) {
                                "$tabLabel，${uiState.trashedPhotos.size} 项待清理"
                            } else {
                                tabLabel
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        trashedCount = uiState.trashedPhotos.size,
                                        tint = if (selected) DecoGold else DecoInkOnGround.copy(alpha = 0.72f),
                                        decorative = true
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = DecoDisplayFontFamily,
                                        letterSpacing = 1.5.sp
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DecoGold,
                                    selectedTextColor = DecoGold,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = DecoInkOnGround.copy(alpha = 0.72f),
                                    unselectedTextColor = DecoInkOnGround.copy(alpha = 0.72f)
                                ),
                                modifier = Modifier
                                    .drawBehind {
                                        // 画报选中态：底边 1dp 金下划线（弃 M3 填充胶囊指示器，同 FloatingGalleryDock 语法）
                                        if (selected) {
                                            val y = size.height - 1.dp.toPx()
                                            drawLine(
                                                color = railGold,
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                    .semantics {
                                        contentDescription = railDesc
                                        role = Role.Tab
                                        this.selected = selected
                                    }
                                    .testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = initialStartDestination,
                        enterTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val isTabSwitch = fromRoute in TOP_LEVEL_ROUTES && toRoute in TOP_LEVEL_ROUTES
                            when {
                                isTabSwitch -> {
                                    val dir = if (topLevelRouteIndex(toRoute) >= topLevelRouteIndex(fromRoute)) 1 else -1
                                    fadeIn(animationSpec = tween(155, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(175, easing = FastOutSlowInEasing)) { width ->
                                            dir * (width / 22)
                                        }
                                }
                                toRoute?.startsWith("flipbook") == true -> {
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.93f,
                                            animationSpec = tween(230, easing = FastOutSlowInEasing)
                                        )
                                }
                                toRoute?.startsWith("arc_cleaner") == true -> {
                                    fadeIn(animationSpec = tween(210, easing = FastOutSlowInEasing)) +
                                        slideInVertically(animationSpec = tween(230, easing = FastOutSlowInEasing)) { height ->
                                            height / 14
                                        }
                                }
                                else -> {
                                    fadeIn(animationSpec = tween(190, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.97f, animationSpec = tween(190))
                                }
                            }
                        },
                        exitTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val isTabSwitch = fromRoute in TOP_LEVEL_ROUTES && toRoute in TOP_LEVEL_ROUTES
                            when {
                                isTabSwitch -> {
                                    val dir = if (topLevelRouteIndex(toRoute) >= topLevelRouteIndex(fromRoute)) 1 else -1
                                    fadeOut(animationSpec = tween(105)) +
                                        slideOutHorizontally(animationSpec = tween(135, easing = FastOutSlowInEasing)) { width ->
                                            -dir * (width / 28)
                                        }
                                }
                                toRoute?.startsWith("flipbook") == true -> {
                                    fadeOut(animationSpec = tween(150)) +
                                        scaleOut(targetScale = 1.03f, animationSpec = tween(165))
                                }
                                toRoute?.startsWith("arc_cleaner") == true -> {
                                    fadeOut(animationSpec = tween(145)) +
                                        scaleOut(targetScale = 0.97f, animationSpec = tween(160))
                                }
                                else -> fadeOut(animationSpec = tween(140))
                            }
                        },
                        popEnterTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val isTabSwitch = fromRoute in TOP_LEVEL_ROUTES && toRoute in TOP_LEVEL_ROUTES
                            when {
                                isTabSwitch -> {
                                    val dir = if (topLevelRouteIndex(toRoute) >= topLevelRouteIndex(fromRoute)) 1 else -1
                                    fadeIn(animationSpec = tween(155, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(175, easing = FastOutSlowInEasing)) { width ->
                                            dir * (width / 22)
                                        }
                                }
                                fromRoute?.startsWith("flipbook") == true -> {
                                    fadeIn(animationSpec = tween(195, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 1.02f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                }
                                fromRoute?.startsWith("arc_cleaner") == true -> {
                                    fadeIn(animationSpec = tween(185, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.97f, animationSpec = tween(195, easing = FastOutSlowInEasing))
                                }
                                else -> fadeIn(animationSpec = tween(180))
                            }
                        },
                        popExitTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val isTabSwitch = fromRoute in TOP_LEVEL_ROUTES && toRoute in TOP_LEVEL_ROUTES
                            when {
                                isTabSwitch -> {
                                    val dir = if (topLevelRouteIndex(toRoute) >= topLevelRouteIndex(fromRoute)) 1 else -1
                                    fadeOut(animationSpec = tween(105)) +
                                        slideOutHorizontally(animationSpec = tween(135, easing = FastOutSlowInEasing)) { width ->
                                            -dir * (width / 28)
                                        }
                                }
                                fromRoute?.startsWith("flipbook") == true -> {
                                    fadeOut(animationSpec = tween(160)) +
                                        scaleOut(targetScale = 0.94f, animationSpec = tween(175, easing = FastOutSlowInEasing))
                                }
                                fromRoute?.startsWith("arc_cleaner") == true -> {
                                    fadeOut(animationSpec = tween(155)) +
                                        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { height ->
                                            height / 12
                                        }
                                }
                                else -> fadeOut(animationSpec = tween(140))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(FlipYearRoutes.BOOKSHELF) {
                            BookshelfScreen(
                                uiState = uiState,
                                themeMode = themeMode,
                                userPreferences = userPreferences,
                                onSelectThemeMode = onSelectThemeMode,
                                onSelectTimelineSortOrder = { order ->
                                    onSelectTimelineSortOrder(order)
                                    viewModel.setTimelineSortOrder(order)
                                },
                                onToggleShowEmptyMonths = onToggleShowEmptyMonths,
                                onSelectFlipbookDefaultView = onSelectFlipbookDefaultView,
                                onSelectDefaultStartTab = onSelectDefaultStartTab,
                                onSelectGalleryGridDensity = onSelectGalleryGridDensity,
                                onSelectYearbookCoverStyle = { style ->
                                    onSelectYearbookCoverStyle(style)
                                    viewModel.setDefaultYearbookCoverStyle(style)
                                },
                                onOpenYearbook = { year ->
                                    navController.navigate(FlipYearRoutes.flipbook(year))
                                },
                                onStartYearCleanup = { year, includeReviewed ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForYear(
                                            year = year,
                                            includeReviewed = includeReviewed,
                                            source = CleanerEntrySource.BOOKSHELF
                                        )
                                    )
                                },
                                onOpenVault = {
                                    viewModel.setVaultSessionFilter(null)
                                    navigateToTab(FlipYearRoutes.VAULT)
                                },
                                onRequestMediaPermission = requestMediaAccess,
                                onRescanDeviceMedia = { viewModel.scanDeviceMedia(silent = false) },
                                onSelectFilter = viewModel::setMediaFilter,
                                onLoadDemoArchive = viewModel::loadDemoArchive,
                                onRemoveDemoArchive = viewModel::removeDemoArchive,
                                onSaveYearbookCover = viewModel::saveYearbookCover,
                                onResetYearbookCover = viewModel::resetYearbookCoverToSmart,
                                onExportYearbookCollage = viewModel::exportYearbookCollage
                            )
                        }

                        composable(FlipYearRoutes.GALLERY) {
                            GalleryScreen(
                                uiState = uiState,
                                galleryColumns = userPreferences.galleryGridDensity.columns,
                                onSelectBestInBurst = viewModel::selectBestInStack,
                                onKeepBestInBurst = viewModel::keepBestInGalleryBurst,
                                onKeepPhoto = { photo, fav -> viewModel.keepPhoto(photo, fav, fromCleanerSession = false) },
                                onToggleFavorite = viewModel::toggleFavoritePhoto,
                                onTrashPhoto = { photo -> viewModel.swipeTrashPhoto(photo, fromCleanerSession = false) },
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
                                onComputeSemanticMatches = viewModel::computeSemanticMatches,
                                onSelectionModeChanged = { isGallerySelectionMode = it },
                                onStartCustomCleanup = { sessionTitle, photoIds, includeRev ->
                                    if (photoIds.isNotEmpty()) {
                                        viewModel.setCustomCleanerSession(
                                            title = sessionTitle,
                                            photoIds = photoIds,
                                            includeReviewed = includeRev
                                        )
                                        navController.navigate(
                                            FlipYearRoutes.cleanerForCustom(
                                                includeReviewed = includeRev,
                                                source = CleanerEntrySource.GALLERY
                                            )
                                        )
                                    }
                                },
                                onStartMonthCleanup = { y, m, includeRev ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForMonth(
                                            year = y,
                                            month = m,
                                            includeReviewed = includeRev,
                                            focusDay = null,
                                            source = CleanerEntrySource.GALLERY
                                        )
                                    )
                                },
                                onResetGlobalMediaFilter = { viewModel.setMediaFilter(MediaFilter.ALL) }
                            )
                        }

                        composable(FlipYearRoutes.MAP) {
                            FootprintMapScreen(
                                uiState = uiState,
                                onCleanLocation = { locName, includeReviewed ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForLocation(
                                            location = locName,
                                            includeReviewed = includeReviewed,
                                            source = CleanerEntrySource.MAP
                                        )
                                    )
                                },
                                onKeepPhoto = { photo, fav -> viewModel.keepPhoto(photo, fav, fromCleanerSession = false) },
                                onToggleFavorite = viewModel::toggleFavoritePhoto,
                                onTrashPhoto = { photo -> viewModel.swipeTrashPhoto(photo, fromCleanerSession = false) },
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
                                onGeocodeLocation = viewModel::geocodeLocationQuery,
                                onResetGlobalMediaFilter = { viewModel.setMediaFilter(MediaFilter.ALL) }
                            )
                        }

                        composable(FlipYearRoutes.VAULT) {
                            TrashVaultScreen(
                                uiState = uiState,
                                onRestorePhoto = viewModel::restoreFromVault,
                                onRestoreAll = { targets -> viewModel.restoreAllFromVault(targets) },
                                onConfirmPurgeTargets = confirmVaultPurgeTargets,
                                onClearSessionFilter = { viewModel.setVaultSessionFilter(null) },
                                onBrowseToOrganize = { navigateToTab(FlipYearRoutes.BOOKSHELF) }
                            )
                        }

                        composable(
                            route = FlipYearRoutes.FLIPBOOK,
                            arguments = listOf(navArgument("year") { type = NavType.IntType })
                        ) { entry ->
                            val year = entry.arguments?.getInt("year") ?: 2026
                            val pages = remember(
                                year,
                                uiState.allPhotos,
                                uiState.monthStamps,
                                uiState.timelineSortOrder,
                                userPreferences.showEmptyMonths
                            ) {
                                viewModel.getMonthPagesForYear(
                                    year = year,
                                    state = uiState,
                                    showEmptyMonths = userPreferences.showEmptyMonths
                                )
                            }
                            val flipbookAvailableYears = remember(uiState.yearbooks, year) {
                                uiState.yearbooks.map { it.year }
                                    .distinct()
                                    .sortedDescending()
                                    .ifEmpty { listOf(year) }
                            }
                            FlipbookScreen(
                                year = year,
                                monthPages = pages,
                                recentlyStampedMonthKey = uiState.recentlyStampedMonthKey,
                                timelineSortOrder = uiState.timelineSortOrder,
                                showEmptyMonths = userPreferences.showEmptyMonths,
                                defaultShowGalleryMode = userPreferences.flipbookDefaultView == FlipbookDefaultView.GALLERY,
                                galleryColumns = userPreferences.galleryGridDensity.columns,
                                existingAlbums = uiState.galleryIndex.existingAlbums,
                                availableYears = flipbookAvailableYears,
                                locationClusters = uiState.locationClusters,
                                allPhotos = uiState.allPhotos,
                                onBack = { navController.popBackStack() },
                                onStartMonthCleanup = { y, m, includeReviewed, focusDay ->
                                    navController.navigate(
                                        FlipYearRoutes.cleanerForMonth(
                                            year = y,
                                            month = m,
                                            includeReviewed = includeReviewed,
                                            focusDay = focusDay,
                                            source = CleanerEntrySource.FLIPBOOK
                                        )
                                    )
                                },
                                onSelectBestInBurst = viewModel::selectBestInStack,
                                onKeepBestInBurst = viewModel::keepBestInGalleryBurst,
                                onKeepPhoto = { photo, fav -> viewModel.keepPhoto(photo, fav, fromCleanerSession = false) },
                                onToggleFavorite = viewModel::toggleFavoritePhoto,
                                onTrashPhoto = { photo -> viewModel.swipeTrashPhoto(photo, fromCleanerSession = false) },
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
                                onGeocodeLocation = viewModel::geocodeLocationQuery
                            )
                        }

                        composable(
                            route = FlipYearRoutes.CLEANER,
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("month") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("day") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("location") { type = NavType.StringType; defaultValue = "" },
                                navArgument("includeReviewed") { type = NavType.BoolType; defaultValue = false },
                                navArgument("source") { type = NavType.StringType; defaultValue = CleanerEntrySource.BOOKSHELF.routeKey },
                                navArgument("custom") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { entry ->
                            val yearArg = entry.arguments?.getInt("year")?.takeIf { it > 0 }
                            val monthArg = entry.arguments?.getInt("month")?.takeIf { it in 1..12 }
                            val dayArg = entry.arguments?.getInt("day")?.takeIf { it in 1..31 }
                            val locArg = entry.arguments?.getString("location")?.takeIf { it.isNotBlank() }
                            val includeReviewed = entry.arguments?.getBoolean("includeReviewed") ?: false
                            val entrySource = CleanerEntrySource.fromRouteKey(entry.arguments?.getString("source"))
                            val isCustomSession = entry.arguments?.getBoolean("custom") ?: false
                            val customSession = if (isCustomSession) uiState.customCleanerSession else null

                            LaunchedEffect(entry.id) {
                                viewModel.resetCleanerSessionUndo()
                            }

                            val queue = remember(
                                yearArg,
                                monthArg,
                                locArg,
                                includeReviewed,
                                customSession,
                                uiState.allPhotos,
                                uiState.timelineSortOrder
                            ) {
                                viewModel.buildArcQueue(
                                    year = yearArg,
                                    month = monthArg,
                                    locationName = locArg,
                                    includeReviewed = customSession?.includeReviewed ?: includeReviewed,
                                    state = uiState,
                                    customPhotoIds = customSession?.photoIds
                                )
                            }

                            val headerTitle = when {
                                customSession != null -> customSession.title
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
                                includeReviewed = includeReviewed,
                                entrySource = entrySource,
                                timelineSortOrder = uiState.timelineSortOrder,
                                queue = queue,
                                undoStackDepth = uiState.cleanerSessionUndoDepth,
                                onBack = { navController.popBackStack() },
                                onOpenVaultForSession = { sessionTrashedIds ->
                                    viewModel.setVaultSessionFilter(sessionTrashedIds)
                                    navController.navigate(FlipYearRoutes.VAULT) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                onKeepPhoto = { photo, fav ->
                                    viewModel.keepPhoto(photo, fav, fromCleanerSession = true)
                                },
                                onToggleFavorite = viewModel::toggleFavoritePhoto,
                                onTrashPhoto = { photo ->
                                    viewModel.swipeTrashPhoto(photo, fromCleanerSession = true)
                                },
                                onKeepEntireStack = { stack, fav ->
                                    viewModel.keepEntireBurstStack(stack, fav, fromCleanerSession = true)
                                },
                                onKeepBestInStack = { stack, chosen ->
                                    viewModel.keepBestAndTrashStackOthers(stack, chosen, fromCleanerSession = true)
                                },
                                onTrashEntireStack = { stack ->
                                    viewModel.trashEntireStack(stack, fromCleanerSession = true)
                                },
                                onSelectBestInStack = viewModel::selectBestInStack,
                                onUndoSession = { triggeredByShake ->
                                    viewModel.undoCleanerSessionAction(triggeredByShake = triggeredByShake)
                                }
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
                            DecoPosterPanel(
                                radius = 3.dp,
                                shadowElevation = 12.dp,
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
                                            color = DecoGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = taskMsg,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = DecoInkOnGround,
                                            maxLines = 1
                                        )
                                    }
                                    if (uiState.batchTaskTotal > 0) {
                                        val fraction = (uiState.batchTaskProcessed.toFloat() / uiState.batchTaskTotal.toFloat())
                                            .coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            color = DecoGold,
                                            trackColor = DecoGold.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                }

                // Stage 1.4: Bottom Material 3 Snackbar positioned above the Floating Gallery Dock,
                // carrying each message's own bound UndoSnapshot and explicit undo copy (>=4s).
                val snackbarBottomPadding = when {
                    showTopLevelNav && !useNavigationRail && !isGallerySelectionMode -> 84.dp
                    isGallerySelectionMode -> 92.dp
                    else -> 16.dp
                }
                AnimatedVisibility(
                    visible = uiState.snackbarMessage != null && uiState.batchTaskMessage == null,
                    enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = snackbarBottomPadding)
                        .widthIn(max = 460.dp)
                        .testTag("bottom_feedback_snackbar")
                ) {
                    uiState.snackbarMessage?.let { snack ->
                        Snackbar(
                            shape = RoundedCornerShape(3.dp),
                            containerColor = DecoMat,
                            contentColor = DecoInkOnGround,
                            actionContentColor = DecoVermInk,
                            action = snack.undoSnapshot?.let { boundSnapshot ->
                                {
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.undoBySnapshot(boundSnapshot)
                                        },
                                        modifier = Modifier.testTag("banner_undo_button")
                                    ) {
                                        Text(
                                            text = snack.undoLabel ?: stringResource(R.string.action_undo),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = DecoDisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = DecoVermInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { viewModel.dismissBanner() }
                        ) {
                            Text(
                                text = snack.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DecoInkOnGround,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
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
                        // 画报底座：导航条区域以 DecoGroundDeep 纯色填充（不再透出滚动内容），
                        // 与 dock 纵向渐变终点无缝衔接为一整块 footer
                        .background(DecoGroundDeep)
                        .navigationBarsPadding()
                ) {
                    FloatingGalleryDock(
                        tabs = tabs,
                        currentRoute = currentRoute,
                        trashedCount = uiState.trashedPhotos.size,
                        onSelectTab = navigateToTab,
                        onTopEdgeMeasured = { dockTopYPx.value = it }
                    )
                }

                // True full-screen edge-to-edge overlay layer (e.g. PhotoDetailLightbox) inside
                // MainActivity's window so system bars, cutouts, and bottom docks never clip.
                FullScreenOverlayLayer(host = fullScreenOverlayHost)

                // Stage 4: One-time skippable gesture guide shown once on first launch
                if (!hasSeenOnboarding && !uiState.isLoading) {
                    OnboardingGestureGuideOverlay(
                        onDismiss = { onboardingPrefs.markOnboardingGuideSeen() }
                    )
                }
            }
        }
    }
}

/**
 * 摩登画报底部导航（deco-app.html:126 DecoNav）：纸地渐变底 + 1dp 金细线，无玻璃胶囊、无缎面药丸。
 * 原型同款纯文字：无图标、等宽槽位，站酷小薇 13sp 字距 1.7sp，选中金下划线 + DecoGold 字标，
 * 未选中 DecoInkOnGround(0.72)。回收站待清理数以朱红计费小签随行。
 * 交互基线不变：乐观路由 visualRoute、临界阻尼 spring、tab.testTag / Role.Tab / selected 语义 / onSelectTab。
 */
@Composable
private fun FloatingGalleryDock(
    tabs: List<TopLevelTab>,
    currentRoute: String?,
    trashedCount: Int,
    onSelectTab: (String) -> Unit,
    onTopEdgeMeasured: (Float) -> Unit = {}
) {
    if (tabs.isEmpty()) return
    val gold = DecoGold
    val ink = DecoInkOnGround
    val density = LocalDensity.current

    // Optimistic route responds on frame 0 when tapped, staying synced with NavController backstack
    var visualRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        visualRoute = currentRoute
    }

    // Crisp, critically-damped spring shared by gliding gold underline, slot expansion, icon morph, and label reveal
    val dockSpringSpec = remember {
        spring<Float>(
            dampingRatio = 0.88f,
            stiffness = 500f
        )
    }

    val selectedIndex = remember(tabs, visualRoute) {
        tabs.indexOfFirst { it.route == visualRoute }.coerceAtLeast(0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(DecoGround, DecoGroundDeep)),
            )
            // 仅顶部金细线：圆角实机上左右边框会被屏幕圆角截断产生割裂
            .drawBehind {
                // 上下金细线：左右开放（圆角屏割裂规避），底边线闭合底座
                drawLine(
                    color = gold.copy(alpha = 0.5f),
                    start = Offset(0f, 0.5.dp.toPx()),
                    end = Offset(size.width, 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gold.copy(alpha = 0.5f),
                    start = Offset(0f, size.height - 0.5.dp.toPx()),
                    end = Offset(size.width, size.height - 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .onGloballyPositioned { onTopEdgeMeasured(it.positionInRoot().y) }
            .testTag("floating_gallery_dock"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            val gapDp = 4.dp
            val gapPxFloat = with(density) { gapDp.toPx() }
            val gapPxInt = gapPxFloat.roundToInt()
            val heightPxInt = with(density) { 48.dp.roundToPx() }
            val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            // 原型 DecoNav：纯文字等宽槽位，金下划线滑到选中槽
            val totalWeight = tabs.size.toFloat()
            val availableWidthPx = (containerWidthPx - gapPxFloat * (tabs.size - 1)).coerceAtLeast(1f)
            val unitWidthPx = availableWidthPx / totalWeight
            val targetPillOffsetX = selectedIndex * (unitWidthPx + gapPxFloat)
            val targetPillWidth = unitWidthPx

            val pillOffsetXState = animateFloatAsState(
                targetValue = targetPillOffsetX,
                animationSpec = dockSpringSpec,
                label = "dock_gliding_pill_x"
            )
            val pillWidthState = animateFloatAsState(
                targetValue = targetPillWidth,
                animationSpec = dockSpringSpec,
                label = "dock_gliding_pill_w"
            )

            Layout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .drawBehind {
                        // 画报语法：选中槽位仅在底边画 1dp 金下划线（无缎面药丸、无玻璃底）
                        val y = size.height - 0.5.dp.toPx()
                        val currentX = pillOffsetXState.value
                        val currentW = pillWidthState.value
                        drawLine(gold, Offset(currentX, y), Offset(currentX + currentW, y), strokeWidth = 1.dp.toPx())
                    },
                content = {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        val tabLabel = stringResource(tab.labelRes)
                        val tabAccessibilityDesc = if (tab.route == FlipYearRoutes.VAULT && trashedCount > 0) {
                            "$tabLabel，${trashedCount} 项待清理"
                        } else {
                            tabLabel
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        val pressScaleState = animateFloatAsState(
                            targetValue = if (pressed) 0.92f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dock_press_scale_${tab.route}"
                        )

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.dp))
                                .graphicsLayer {
                                    val s = pressScaleState.value
                                    scaleX = s
                                    scaleY = s
                                }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = tabAccessibilityDesc
                                    role = Role.Tab
                                    this.selected = isSelected
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab
                                ) {
                                    if (visualRoute != tab.route) {
                                        visualRoute = tab.route
                                    }
                                    onSelectTab(tab.route)
                                }
                                .padding(horizontal = 6.dp)
                                .testTag(tab.testTag)
                        ) {
                            // 画报细线图标（Canvas 手绘语汇）+ 小薇文字
                            DecoTabIcon(
                                kind = when (tab.route) {
                                    FlipYearRoutes.BOOKSHELF -> DecoTabIconKind.ALBUM
                                    FlipYearRoutes.GALLERY -> DecoTabIconKind.GALLERY
                                    FlipYearRoutes.MAP -> DecoTabIconKind.FOOTPRINT
                                    else -> DecoTabIconKind.VAULT
                                },
                                color = if (isSelected) gold else ink.copy(alpha = 0.72f),
                                selected = isSelected,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = tabLabel,
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                // 原型 0.14em @12px ≈ 1.7sp
                                letterSpacing = 1.7.sp,
                                color = if (isSelected) gold else ink.copy(alpha = 0.72f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                            if (tab.route == FlipYearRoutes.VAULT && trashedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = trashedCount.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = DecoPaperCream,
                                    modifier = Modifier
                                        .background(DecoVerm, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            ) { measurables, constraints ->
                val count = measurables.size
                if (count == 0) {
                    return@Layout layout(constraints.minWidth, heightPxInt) {}
                }
                val totalWidth = constraints.maxWidth
                val totalGaps = gapPxInt * (count - 1)
                val availWidth = (totalWidth - totalGaps).coerceAtLeast(count)

                // 原型等宽槽位：文字导航不再做选中槽扩张
                val slotWidth = (availWidth / count).coerceAtLeast(1)

                var usedWidth = 0
                val placeables = Array(count) { i ->
                    val slotW = if (i == count - 1) {
                        (availWidth - usedWidth).coerceAtLeast(1)
                    } else {
                        slotWidth
                    }
                    usedWidth += slotW
                    measurables[i].measure(Constraints.fixed(slotW, heightPxInt))
                }

                layout(totalWidth, heightPxInt) {
                    var x = 0
                    for (i in 0 until count) {
                        placeables[i].placeRelative(x, 0)
                        x += placeables[i].width + gapPxInt
                    }
                }
            }
        }
    }
}

/**
 * 宽屏 NavigationRail 图标（不属原型手机版范围，统一画报手绘细线图标）。
 * 回收站待清理数以朱红计数徽随行。
 */
@Composable
private fun TabIconWithBadge(
    tab: TopLevelTab,
    selected: Boolean,
    trashedCount: Int,
    tint: Color,
    decorative: Boolean = false
) {
    val iconDesc = if (decorative) null else stringResource(tab.labelRes)
    val iconModifier = if (iconDesc != null) {
        Modifier.semantics { contentDescription = iconDesc }
    } else {
        Modifier
    }
    val decoIcon: @Composable () -> Unit = {
        DecoTabIcon(
            kind = tab.iconKind,
            color = tint,
            selected = selected,
            iconSize = 22.dp,
            modifier = iconModifier
        )
    }
    if (tab.route == FlipYearRoutes.VAULT && trashedCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = DecoVerm,
                    contentColor = DecoPaperCream
                ) {
                    Text(trashedCount.toString())
                }
            }
        ) {
            decoIcon()
        }
    } else {
        decoIcon()
    }
}
