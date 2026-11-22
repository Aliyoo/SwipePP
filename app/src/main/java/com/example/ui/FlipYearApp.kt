package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.State
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.roundToInt
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
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FullScreenOverlayHost
import com.example.ui.components.FullScreenOverlayLayer
import com.example.ui.components.LocalFullScreenOverlayHost
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.OnboardingGestureGuideOverlay
import com.example.ui.components.activeChromeSatinBrush
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
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.ThemePreferences

val LocalFloatingDockInset = compositionLocalOf { 0.dp }

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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
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
            .background(StudioCanvas)
    ) {
        val useNavigationRail = maxWidth >= 640.dp
        val floatingDockInset = if (showTopLevelNav && !useNavigationRail) 82.dp else 0.dp
        val fullScreenOverlayHost = remember { FullScreenOverlayHost() }

        CompositionLocalProvider(
            LocalFloatingDockInset provides floatingDockInset,
            LocalFullScreenOverlayHost provides fullScreenOverlayHost
        ) {
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
                                        tint = if (selected) CobaltPrimary else InkSecondary,
                                        decorative = true
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel,
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
                                modifier = Modifier
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
                                onResetYearbookCover = viewModel::resetYearbookCoverToSmart
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
                                onClearSessionFilter = { viewModel.setVaultSessionFilter(null) }
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
                        val isDark = LocalIsDarkTheme.current
                        Snackbar(
                            shape = RoundedCornerShape(18.dp),
                            containerColor = if (isDark) Color(0xFF1F2636) else Color(0xFF1A1F2C),
                            contentColor = Color.White,
                            actionContentColor = Color(0xFF7CA7FF),
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
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8AB4FF),
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
                                color = Color.White,
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

private const val ACTIVE_DOCK_TAB_WEIGHT = 1.95f

/**
 * Truly floating translucent frosted-glass capsule navigation dock overlaid directly on top of
 * full-bleed maps, photo grids, and backdrops:
 * - Inactive tabs stay calm, centered pure icons.
 * - Active tab expands horizontally while a sculpted satin pill glides across the dock to reveal
 *   the label.
 * - Performance Architecture: ZERO recompositions across every animation frame! Slot widths are
 *   computed inside a custom `Layout` measure pass, the gliding pill is drawn in `drawBehind`,
 *   icon morph/tint transitions run in GPU `graphicsLayer`, and label reveal runs in `Modifier.layout`
 *   + `placeWithLayer`.
 */
@Composable
private fun FloatingGalleryDock(
    tabs: List<TopLevelTab>,
    currentRoute: String?,
    trashedCount: Int,
    onSelectTab: (String) -> Unit
) {
    if (tabs.isEmpty()) return
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val density = LocalDensity.current
    val unselectedTint = InkSecondary
    val activeObsidianBrush = activeChromeSatinBrush()
    val activeRimBrush = remember(palette) {
        Brush.linearGradient(
            colors = listOf(
                palette.activeChromeBorderStart,
                Color.White.copy(alpha = 0.12f),
                palette.activeChromeBorderEnd
            )
        )
    }

    // Optimistic route responds on frame 0 when tapped, staying synced with NavController backstack
    var visualRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        visualRoute = currentRoute
    }

    // Crisp, critically-damped spring shared by gliding pill, slot expansion, icon morph, and label reveal
    val dockSpringSpec = remember {
        spring<Float>(
            dampingRatio = 0.88f,
            stiffness = 500f
        )
    }

    val selectedIndex = remember(tabs, visualRoute) {
        tabs.indexOfFirst { it.route == visualRoute }.coerceAtLeast(0)
    }

    // Hold State<Float> handles without reading `.value` in composition scope -> 0 recompositions per frame!
    val expandStates: List<State<Float>> = tabs.mapIndexed { index, tab ->
        animateFloatAsState(
            targetValue = if (index == selectedIndex) 1f else 0f,
            animationSpec = dockSpringSpec,
            label = "dock_tab_expand_${tab.route}"
        )
    }

    FrostedGlassCard(
        shape = RoundedCornerShape(28.dp),
        topColor = if (isDark) Color(0xEB1C2230) else Color(0xE0FFFFFF),
        bottomColor = if (isDark) Color(0xDE131722) else Color(0xCEF7F5F0),
        shadowElevation = 10.dp,
        enableAmbientBlur = false,
        modifier = Modifier
            .widthIn(max = 376.dp)
            .fillMaxWidth()
            .testTag("floating_gallery_dock")
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
            val totalWeight = (tabs.size - 1) + ACTIVE_DOCK_TAB_WEIGHT
            val availableWidthPx = (containerWidthPx - gapPxFloat * (tabs.size - 1)).coerceAtLeast(1f)
            val unitWidthPx = availableWidthPx / totalWeight
            val targetPillOffsetX = selectedIndex * (unitWidthPx + gapPxFloat)
            val targetPillWidth = unitWidthPx * ACTIVE_DOCK_TAB_WEIGHT

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
                        val cornerPx = size.height / 2f
                        val currentX = pillOffsetXState.value
                        val currentW = pillWidthState.value
                        val topLeft = Offset(currentX, 0f)
                        val pillSize = Size(currentW.coerceAtLeast(size.height), size.height)
                        // Gliding sculpted obsidian/sapphire satin capsule (draw-phase only, sub-pixel smooth)
                        drawRoundRect(
                            brush = activeObsidianBrush,
                            topLeft = topLeft,
                            size = pillSize,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                        // Subtle top specular highlight arc
                        val sheenInset = cornerPx * 0.55f
                        if (pillSize.width > sheenInset * 2f) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.28f),
                                start = Offset(topLeft.x + sheenInset, 0.75.dp.toPx()),
                                end = Offset(topLeft.x + pillSize.width - sheenInset, 0.75.dp.toPx()),
                                strokeWidth = 0.9.dp.toPx()
                            )
                        }
                        // Crisp gradient rim contour
                        val strokeWidthPx = 1.dp.toPx()
                        drawRoundRect(
                            brush = activeRimBrush,
                            topLeft = Offset(topLeft.x + strokeWidthPx / 2f, strokeWidthPx / 2f),
                            size = Size(
                                (pillSize.width - strokeWidthPx).coerceAtLeast(1f),
                                (pillSize.height - strokeWidthPx).coerceAtLeast(1f)
                            ),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            style = Stroke(width = strokeWidthPx)
                        )
                    },
                content = {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        val expandState = expandStates[index]
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
                                .clip(RoundedCornerShape(24.dp))
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
                            AnimatedDockTabIcon(
                                tab = tab,
                                expandState = expandState,
                                trashedCount = trashedCount,
                                unselectedTint = unselectedTint
                            )

                            // Natural-width label reveal: read expandState.value ONLY in layout & draw phase
                            // -> 0 recompositions, centered pure icon when inactive, zero "..." clipping!
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.layout { measurable, constraints ->
                                    val progress = expandState.value.coerceIn(0f, 1f)
                                    val placeable = measurable.measure(
                                        constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                                    )
                                    val revealedWidth = (placeable.width * progress).roundToInt()
                                        .coerceIn(0, constraints.maxWidth)
                                    layout(revealedWidth, placeable.height) {
                                        if (progress > 0.02f) {
                                            val labelAlpha = ((progress - 0.18f) / 0.82f).coerceIn(0f, 1f)
                                            val shiftX = -(1f - progress) * 5.dp.toPx()
                                            placeable.placeWithLayer(0, 0) {
                                                alpha = labelAlpha
                                                translationX = shiftX
                                            }
                                        }
                                    }
                                }
                            ) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tabLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
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

                val weights = FloatArray(count) { i ->
                    1f + (ACTIVE_DOCK_TAB_WEIGHT - 1f) * expandStates[i].value.coerceIn(0f, 1f)
                }
                var sumWeights = 0f
                for (w in weights) sumWeights += w
                if (sumWeights <= 0f) sumWeights = count.toFloat()

                var usedWidth = 0
                val placeables = Array(count) { i ->
                    val slotW = if (i == count - 1) {
                        (availWidth - usedWidth).coerceAtLeast(1)
                    } else {
                        ((availWidth * (weights[i] / sumWeights)).roundToInt()).coerceAtLeast(1)
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
 * Zero-recomposition dual-layer icon crossfade (outlined InkSecondary <-> filled White)
 * driven strictly in GPU `graphicsLayer` from `expandState`.
 */
@Composable
private fun AnimatedDockTabIcon(
    tab: TopLevelTab,
    expandState: State<Float>,
    trashedCount: Int,
    unselectedTint: Color
) {
    val iconStack: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.unselectedIcon,
                contentDescription = null,
                tint = unselectedTint,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - expandState.value * 1.45f).coerceIn(0f, 1f)
                    }
            )
            Icon(
                imageVector = tab.selectedIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = ((expandState.value - 0.14f) / 0.86f).coerceIn(0f, 1f)
                    }
            )
        }
    }

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
            iconStack()
        }
    } else {
        iconStack()
    }
}

@Composable
private fun TabIconWithBadge(
    tab: TopLevelTab,
    selected: Boolean,
    trashedCount: Int,
    tint: Color,
    decorative: Boolean = false
) {
    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
    val iconDesc = if (decorative) null else stringResource(tab.labelRes)
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
                contentDescription = iconDesc,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = iconDesc,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
