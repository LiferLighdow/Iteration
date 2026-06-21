package com.liferlighdow.iteration.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.viewmodel.handleAppDrop
import com.liferlighdow.iteration.viewmodel.prepareForDrag
import com.liferlighdow.iteration.viewmodel.removeAppFromHome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: MainViewModel = viewModel(),
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val blurredWallpaper by viewModel.blurredWallpaper.collectAsState()
    val rawWallpaper by viewModel.rawWallpaper.collectAsState()
    val wallpaperSignal by viewModel.wallpaperUpdateSignal.collectAsState()

    // 唯一的採樣器，確保座標對齊
    val backdrop = rememberLayerBackdrop()
    
    // 當訊號改變時，強制重新加載
    LaunchedEffect(wallpaperSignal) {
        if (wallpaperSignal > 0) {
            viewModel.updateBlurredWallpaper()
        }
    }
    val pages by viewModel.pages.collectAsState()
    val allAppsFlat by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val minusOneWidgets by viewModel.minusOneWidgets.collectAsState()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidGlassDockEnabled by viewModel.isLiquidGlassDockEnabled.collectAsState()
    val isLiquidGlassHomeFolderEnabled by viewModel.isLiquidGlassHomeFolderEnabled.collectAsState()
    val isLiquidGlassAppLibraryFolderEnabled by viewModel.isLiquidGlassAppLibraryFolderEnabled.collectAsState()
    val isLiquidGlassGlobalSearchEnabled by viewModel.isLiquidGlassGlobalSearchEnabled.collectAsState()
    val isLiquidGlassAppLibrarySearchEnabled by viewModel.isLiquidGlassAppLibrarySearchEnabled.collectAsState()

    val iconShape by viewModel.iconShape.collectAsState()
    val libraryShape by viewModel.libraryShape.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    val doubleTapAction by viewModel.doubleTapAction.collectAsState()
    val swipeUpAction by viewModel.swipeUpAction.collectAsState()
    val doubleTapApp by viewModel.doubleTapApp.collectAsState()
    val swipeUpApp by viewModel.swipeUpApp.collectAsState()
    val swipeDownAction by viewModel.swipeDownAction.collectAsState()
    val longPressAction by viewModel.longPressAction.collectAsState()
    val swipeDownApp by viewModel.swipeDownApp.collectAsState()
    val longPressApp by viewModel.longPressApp.collectAsState()
    val twoFingerSwipeUpAction by viewModel.twoFingerSwipeUpAction.collectAsState()
    val twoFingerSwipeDownAction by viewModel.twoFingerSwipeDownAction.collectAsState()
    val twoFingerSwipeUpApp by viewModel.twoFingerSwipeUpApp.collectAsState()
    val twoFingerSwipeDownApp by viewModel.twoFingerSwipeDownApp.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val showMinusOnePage by viewModel.showMinusOnePage.collectAsState()
    val showAppLibrary by viewModel.showAppLibrary.collectAsState()

    var showDesktopMenu by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }

    val mContext = LocalContext.current
    
    fun performGestureAction(action: GestureAction, pkg: String) {
        com.liferlighdow.iteration.utils.performGestureAction(
            action = action,
            pkg = pkg,
            context = mContext,
            onSettingsClick = onSettingsClick,
            onOpenGlobalSearch = { showGlobalSearch = true },
            onOpenDesktopMenu = { showDesktopMenu = true }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val myPackageName = mContext.packageName

    val isDefaultLauncher = remember(allAppsFlat) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = mContext.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == myPackageName
    }

    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var folderToOpenId by remember { mutableStateOf<String?>(null) }
    val openFolder = remember(folderToOpenId, pages) {
        pages.flatten().find { it.uniqueId == folderToOpenId }
    }
    var folderIconPosition by remember { mutableStateOf(Offset.Zero) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var widgetTargetPage by remember { mutableStateOf<Int?>(null) }

    // 新增：快速編輯 App 的狀態
    var appToEdit by remember { mutableStateOf<AppModel?>(null) }

    val slotBounds = remember { mutableStateMapOf<String, Rect>() }
    var rawHoveredKey by remember { mutableStateOf<String?>(null) }
    var confirmedHoveredKey by remember { mutableStateOf<String?>(null) }
    var confirmedIntent by remember { mutableStateOf(MainViewModel.DropType.REORDER) }

    LaunchedEffect(rawHoveredKey) {
        if (rawHoveredKey == null) { confirmedHoveredKey = null; return@LaunchedEffect }
        delay(500)
        confirmedHoveredKey = rawHoveredKey
    }

    var showDockPicker by remember { mutableStateOf<Int?>(null) }
    val dockApps = remember(dockPkgNames, allAppsFlat) {
        List(4) { index ->
            val pkg = dockPkgNames.getOrNull(index) ?: ""
            allAppsFlat.find { it.packageName == pkg && !it.isHidden } 
                ?: AppModel(label = "", packageName = "", uniqueId = "empty_dock_$index")
        }
    }

    // iOS 感的主畫面聯動動畫
    val launcherScale by animateFloatAsState(
        targetValue = if (showGlobalSearch) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "launcherScale"
    )

    // 新增：高強度模糊動畫
    val launcherBlur by animateDpAsState(
        targetValue = if (showGlobalSearch || folderToOpenId != null) 20.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "launcherBlur"
    )
    
    if (showGlobalSearch) BackHandler { showGlobalSearch = false }

    if (isEditMode) BackHandler { viewModel.setEditMode(false) }

    val desktopPageCount = pages.size.coerceAtLeast(1)
    val minusOneCount = if (showMinusOnePage) 1 else 0
    val libraryCount = if (showAppLibrary) 1 else 0
    val extraDragPage = if (draggingApp != null) 1 else 0
    val pageCount = minusOneCount + desktopPageCount + libraryCount + extraDragPage
    
    val pagerState = rememberPagerState(
        initialPage = if (showMinusOnePage) 1 else 0,
        pageCount = { pageCount }
    )
    val scope = rememberCoroutineScope()
    
    // 控制 Search Pill 與分頁點切換的邏輯
    var isUserInteracting by remember { mutableStateOf(false) }
    var showPillTemporarily by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.isScrollInProgress, draggingApp) {
        if (pagerState.isScrollInProgress || draggingApp != null) {
            isUserInteracting = true
            showPillTemporarily = false
        } else {
            // 停止滑動後等待 1 秒變回 Search Pill
            delay(1000)
            isUserInteracting = false
            showPillTemporarily = true
        }
    }
    
    val desktopStartIndex = if (showMinusOnePage) 1 else 0
    // 計算 Dock 的顯示進度 (1.0 = 完全顯示, 0.0 = 完全隱藏)
    val dockVisibilityProgress by remember(showMinusOnePage, showAppLibrary, desktopPageCount) {
        derivedStateOf {
            val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val desktopStart = if (showMinusOnePage) 1f else 0f
            val desktopEnd = desktopStart + desktopPageCount - 1
            
            if (continuousPage < desktopStart) {
                // 滑向負一頁
                (continuousPage - (desktopStart - 1f)).coerceIn(0f, 1f)
            } else if (continuousPage > desktopEnd) {
                // 滑向 App Library
                (1f - (continuousPage - desktopEnd)).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }

    val isMinusOnePage = showMinusOnePage && pagerState.currentPage == 0
    val isAppLibraryPage = showAppLibrary && pagerState.currentPage == pageCount - 1
    
    // 檢查 App Library 是否處於搜尋模式
    val librarySearchQuery by viewModel.searchQuery.collectAsState()
    val libraryCategory by viewModel.selectedCategory.collectAsState()
    val isLibrarySearchFocused by viewModel.isLibrarySearchFocused.collectAsState()
    val isLibraryInSearchMode = isAppLibraryPage && (isLibrarySearchFocused || librarySearchQuery.isNotEmpty() || (libraryCategory != null && libraryCategory != "All"))

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        
        // 徹底移除桌布 Image 層，回歸純粹的 UI 採樣

        val iconSize = 62.dp
        val iconSizePx = with(density) { iconSize.toPx() }
        val columns = 4
        
        val userRows by viewModel.desktopRows.collectAsState()
        val rows = if (userRows > 0) userRows else (if (maxHeight / maxWidth < 2.0f) 5 else 6)

        LaunchedEffect(columns * rows) { viewModel.setPageSize(columns * rows) }

        LaunchedEffect(draggingApp) {
            if (draggingApp == null) return@LaunchedEffect
            while (true) {
                val finalX = touchPosition.x + dragOffset.x
                val edgeWidth = with(density) { 45.dp.toPx() }
                if (finalX < edgeWidth && pagerState.currentPage > 0) {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    delay(800)
                } else if (finalX > with(density) { maxWidth.toPx() } - edgeWidth && pagerState.currentPage < pageCount - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    delay(800)
                }
                delay(100)
            }
        }

        // 1. 桌面底層 (包含桌布與內容)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = launcherScale
                    scaleY = launcherScale
                    // 只有 Android 12+ 才對整個內容層進行系統級模糊
                    // 透過 graphicsLayer 設定 renderEffect 可以避免頻繁重組
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurPx = with(density) { launcherBlur.toPx() }
                        renderEffect = if (blurPx > 0f) BlurEffect(blurPx, blurPx) else null
                    }
                }
        ) {
            // 1.1 桌布層
            Box(modifier = Modifier.fillMaxSize()) {
                rawWallpaper?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Android 12 以下的「高清模糊」替代方案
                // 透過淡入預先處理好的模糊桌布，避免即時採樣產生的重影
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    blurredWallpaper?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // 在繪製層級控制透明度，避免觸發重組
                                    alpha = (launcherBlur.value / 20f).coerceIn(0f, 1f)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 1.2 桌面內容 (Pager, Icons, Widgets)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Android 12 以下：透過降低透明度讓內容「融入」模糊背景，模擬模糊感
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            alpha = 1f - (launcherBlur.value / 30f).coerceIn(0f, 0.6f)
                        }
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = draggingApp == null && !isLibraryInSearchMode,
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                // 計算頁面偏移量 (-1.0 到 1.0)
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)

                val isMinusOne = showMinusOnePage && pageIndex == 0
                val isLibrary = showAppLibrary && pageIndex == pageCount - 1
                
                val desktopStartIndex = if (showMinusOnePage) 1 else 0
                val isDesktop = pageIndex >= desktopStartIndex && pageIndex < desktopStartIndex + desktopPageCount
                val isNewPage = draggingApp != null && pageIndex == desktopStartIndex + desktopPageCount

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = if (isLibrary || isMinusOne) 0.dp else 158.dp)
                ) {
                    when {
                        isMinusOne -> {
                            MinusOnePage(
                                widgets = minusOneWidgets,
                                viewModel = viewModel,
                                backdrop = backdrop,
                                isEditMode = isEditMode,
                                onAddClick = { showWidgetPicker = true },
                                onRemoveWidget = { viewModel.removeWidget(it) },
                                onUpdateWidgetMode = { id, mode ->
                                    viewModel.updateWidgetDisplayMode(
                                        id,
                                        mode
                                    )
                                }
                            )
                        }
                        isDesktop || isNewPage -> {
                            val desktopIdx = pageIndex - desktopStartIndex
                            AppGrid(
                                apps = pages.getOrNull(desktopIdx) ?: emptyList(),
                                columns = columns, rows = rows, iconSize = iconSize,
                                isEditMode = isEditMode,
                                viewModel = viewModel,
                                pageOffset = pageOffset,
                                isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                backdrop = backdrop,
                                iconShape = iconShape,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration,
                                draggingApp = draggingApp,
                                confirmedHoveredSlotIdx = if (confirmedHoveredKey?.startsWith("$pageIndex-") == true)
                                    confirmedHoveredKey?.substringAfter("-")?.toInt() else null,
                                confirmedIntent = if (isEditMode) MainViewModel.DropType.REORDER else confirmedIntent,
                                onAppClick = { app, pos ->
                                    if (isEditMode) {
                                        if (!app.isFolder) appToEdit = app
                                        return@AppGrid
                                    }
                                    if (app.isFolder) {
                                        folderIconPosition = pos
                                        folderToOpenId = app.uniqueId
                                    } else onAppClick(app.packageName)
                                },
                                onSlotPositioned = { idx, rect ->
                                    slotBounds["$pageIndex-$idx"] = rect
                                },
                                onDragStart = { app, offset ->
                                    viewModel.prepareForDrag()
                                    draggingApp = app; touchPosition = offset; dragOffset =
                                    Offset.Zero
                                },
                                onDrag = { delta ->
                                    dragOffset += delta
                                    val currentPos = touchPosition + dragOffset
                                    val dragRect = Rect(
                                        currentPos.x - iconSizePx / 2,
                                        currentPos.y - iconSizePx / 2,
                                        currentPos.x + iconSizePx / 2,
                                        currentPos.y + iconSizePx / 2
                                    )
                                    var bestKey: String? = null
                                    var maxOverlap = 0f
                                    slotBounds.forEach { (key, rect) ->
                                        val overlap = calculateOverlap(rect, dragRect)
                                        if (overlap > maxOverlap) {
                                            maxOverlap = overlap; bestKey = key
                                        }
                                    }
                                    rawHoveredKey = bestKey
                                    confirmedIntent =
                                        if (!isEditMode && maxOverlap > 0.50f) MainViewModel.DropType.FOLDER else MainViewModel.DropType.REORDER
                                },
                                onDragEnd = {
                                    if (draggingApp != null) {
                                        val finalPos = touchPosition + dragOffset
                                        val dragRect = Rect(
                                            finalPos.x - iconSizePx / 2,
                                            finalPos.y - iconSizePx / 2,
                                            finalPos.x + iconSizePx / 2,
                                            finalPos.y + iconSizePx / 2
                                        )
                                        var bestKey: String? = null
                                        var maxOverlap = 0f
                                        slotBounds.forEach { (key, rect) ->
                                            val overlap = calculateOverlap(rect, dragRect)
                                            if (overlap > maxOverlap) {
                                                maxOverlap = overlap; bestKey = key
                                            }
                                        }
                                        if (bestKey != null) {
                                            val parts = bestKey!!.split("-")
                                            val tPageIdx = parts[0].toInt()
                                            val tSlotIdx = parts[1].toInt()
                                            val targetApp =
                                                pages.getOrNull(tPageIdx - desktopStartIndex)
                                                    ?.getOrNull(tSlotIdx)
                                            val dropType =
                                                if (!isEditMode && maxOverlap > 0.50f && targetApp != null) MainViewModel.DropType.FOLDER else MainViewModel.DropType.REORDER
                                            viewModel.handleAppDrop(
                                                draggingApp!!.uniqueId,
                                                targetApp?.uniqueId,
                                                tPageIdx - desktopStartIndex,
                                                false,
                                                dropType
                                            )
                                        } else {
                                            val currentPage = pagerState.currentPage
                                            if (showAppLibrary && currentPage == pageCount - 1) {
                                                viewModel.removeAppFromHome(draggingApp!!.uniqueId)
                                            } else {
                                                val targetIdx =
                                                    (currentPage - desktopStartIndex).coerceIn(
                                                        0,
                                                        desktopPageCount
                                                    )
                                                viewModel.handleAppDrop(
                                                    draggingApp!!.uniqueId,
                                                    null,
                                                    targetIdx,
                                                    false,
                                                    MainViewModel.DropType.REORDER
                                                )
                                            }
                                        }
                                    }
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey =
                                    null
                                },
                                onBackgroundLongPress = {
                                    if (!isEditMode) {
                                        performGestureAction(longPressAction, longPressApp)
                                    }
                                },
                                onBackgroundClick = {
                                    if (isEditMode) viewModel.setEditMode(false)
                                    else showGlobalSearch = true
                                },
                                onBackgroundDoubleTap = {
                                    performGestureAction(doubleTapAction, doubleTapApp)
                                },
                                onBackgroundSwipeUp = {
                                    performGestureAction(swipeUpAction, swipeUpApp)
                                },
                                onBackgroundSwipeDown = {
                                    performGestureAction(swipeDownAction, swipeDownApp)
                                },
                                onBackgroundTwoFingerSwipeUp = {
                                    performGestureAction(
                                        twoFingerSwipeUpAction,
                                        twoFingerSwipeUpApp
                                    )
                                },
                                onBackgroundTwoFingerSwipeDown = {
                                    performGestureAction(
                                        twoFingerSwipeDownAction,
                                        twoFingerSwipeDownApp
                                    )
                                },
                                onEditApp = { appToEdit = it }
                            )
                        }
                        else -> {
                            AppLibraryPage(
                                allAppsFlat,
                                isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassAppLibraryFolderEnabled,
                                isSearchLiquidGlass = isLiquidGlassEnabled && isLiquidGlassAppLibrarySearchEnabled,
                                backdrop = backdrop,
                                iconShape = iconShape,
                                libraryShape = libraryShape,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration,
                                onAppClick = { pkg ->
                                    val app = allAppsFlat.find { it.packageName == pkg }
                                    if (app?.isFolder == true) folderToOpenId =
                                        app.uniqueId else onAppClick(pkg)
                                },
                                onDragStart = { app, offset ->
                                    draggingApp = app; touchPosition = offset; dragOffset =
                                    Offset.Zero
                                },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    if (draggingApp != null) viewModel.handleAppDrop(
                                        draggingApp!!.packageName,
                                        null,
                                        pagerState.currentPage - desktopStartIndex,
                                        true,
                                        MainViewModel.DropType.REORDER
                                    )
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey =
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                        Button(onClick = { viewModel.setEditMode(false) }) { Text(stringResource(R.string.done)) }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BackHandler(enabled = (isAppLibraryPage || isMinusOnePage) && !showGlobalSearch) {
                        scope.launch { pagerState.animateScrollToPage(desktopStartIndex) }
                    }

                    LauncherBottomBar(
                        visibilityProgress = dockVisibilityProgress,
                        showPill = showPillTemporarily,
                        isLiquidGlassEnabled = isLiquidGlassEnabled,
                        isLiquidGlassDockEnabled = isLiquidGlassDockEnabled,
                        backdrop = backdrop,
                        iconSize = iconSize,
                        iconShape = iconShape,
                        dockStyle = dockStyle,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        desktopPageCount = desktopPageCount,
                        currentPage = pagerState.currentPage - desktopStartIndex,
                        dockApps = dockApps,
                        isEditMode = isEditMode,
                        myPackageName = myPackageName,
                        onSearchClick = { showGlobalSearch = true },
                        onAppClick = onAppClick,
                        onSettingsClick = onSettingsClick,
                        onLongClick = { showDockPicker = it },
                        onDeleteClick = { app ->
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                mContext.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("Iteration", "Uninstall failed", e)
                            }
                        }
                    )
                }
            }

        draggingApp?.let { app ->
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        // 在 Draw 階段計算位移，避免觸發重組
                        translationX = touchPosition.x + dragOffset.x - iconSizePx / 2
                        translationY = touchPosition.y + dragOffset.y - iconSizePx / 2
                        alpha = 0.8f
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
            ) {
                AppItem(
                    app = app,
                    iconSize = iconSize,
                    getIcon = { pkg -> viewModel.getIcon(pkg) })
            }
        }

        GlobalSearchOverlay(
            isVisible = showGlobalSearch,
            onDismiss = { showGlobalSearch = false },
            allApps = allAppsFlat,
            suggestedApps = viewModel.suggestedApps.collectAsState().value,
            onAppClick = onAppClick,
            iconShape = iconShape,
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isLiquidGlassGlobalSearchEnabled = isLiquidGlassGlobalSearchEnabled,
            backdrop = backdrop,
            blurRadius = blurRadius,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
            chromaticAberration = chromaticAberration
        )
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    mContext.contentResolver.openInputStream(it)?.use { input ->
                        val bitmap = BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            viewModel.setCustomWallpaper(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LauncherOverlays(
        viewModel = viewModel,
        showDesktopMenu = showDesktopMenu,
        onDismissDesktopMenu = { showDesktopMenu = false },
        showCreateFolderDialog = showCreateFolderDialog,
        onDismissCreateFolder = { showCreateFolderDialog = false },
        showDeleteFolderConfirm = showDeleteFolderConfirm,
        onDismissDeleteFolder = { showDeleteFolderConfirm = false },
        showWidgetPicker = showWidgetPicker,
        onDismissWidgetPicker = {
            showWidgetPicker = false
            widgetTargetPage = null
        },
        showDockPicker = showDockPicker,
        onDismissDockPicker = { showDockPicker = null },
        appToEdit = appToEdit,
        onDismissAppEdit = { appToEdit = null },
        folderToOpenId = folderToOpenId,
        onDismissFolder = { folderToOpenId = null },
        currentPage = pagerState.currentPage - desktopStartIndex,
        pages = pages,
        allAppsFlat = allAppsFlat,
        isDefaultLauncher = isDefaultLauncher,
        isEditMode = isEditMode,
        iconShape = iconShape,
        backdrop = backdrop,
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        isLiquidGlassEnabled = isLiquidGlassEnabled,
        isLiquidGlassHomeFolderEnabled = isLiquidGlassHomeFolderEnabled,
        onAddWidgetClick = { page ->
            widgetTargetPage = page
            showWidgetPicker = true
        },
        onWallpaperClick = { wallpaperLauncher.launch("image/*") },
        onSettingsClick = onSettingsClick,
        onAppClick = onAppClick
    )
}
