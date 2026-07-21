package com.liferlighdow.iteration.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.ui.dialogs.WallpaperCropDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: MainViewModel = viewModel(),
    onAppClick: (AppModel) -> Unit,
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
    val dockCornerRadius by viewModel.dockCornerRadius.collectAsState()
    val showMinusOnePage by viewModel.showMinusOnePage.collectAsState()
    val showAppLibrary by viewModel.showAppLibrary.collectAsState()
    val isApplyingWallpaper by viewModel.isApplyingWallpaper.collectAsState()
    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
    val iconScaleFactor by viewModel.iconScale.collectAsState()

    var showDesktopMenu by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var searchDragOffset by remember { mutableStateOf(0f) }
    var pickedWallpaperUri by remember { mutableStateOf<Uri?>(null) }
    var showWallpaperTypeDialog by remember { mutableStateOf(false) }
    var showColorPickerByWallpaper by remember { mutableStateOf(false) }
    data class EmojiSelectionData(val color: Int, val emojiText: String)
    var showEmojiModeSelection by remember { mutableStateOf<EmojiSelectionData?>(null) }

    val mContext = LocalContext.current
    val actionMode by viewModel.actionMode.collectAsState()
    val scope = rememberCoroutineScope()

    // --- iOS 風格進入動畫狀態 ---
    var isEntering by remember { mutableStateOf(false) }
    val enterScale by animateFloatAsState(
        targetValue = if (isEntering) 1f else 1.1f, // 從略大縮小回原狀，模擬從 App 抽離感
        animationSpec = spring(
            dampingRatio = 0.8f, // 適度回彈
            stiffness = Spring.StiffnessLow
        ),
        label = "enterScale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (isEntering) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "enterAlpha"
    )
    val enterBlur by animateDpAsState(
        targetValue = if (isEntering) 0.dp else 15.dp,
        animationSpec = tween(durationMillis = 300),
        label = "enterBlur"
    )

    fun performGestureAction(action: GestureAction, pkg: String) {
        com.liferlighdow.iteration.utils.performGestureAction(
            action = action,
            pkg = pkg,
            context = mContext,
            actionMode = actionMode,
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
                // 觸發進入動畫
                isEntering = false
                scope.launch {
                    delay(16) // 確保在下一幀觸發，產生明顯的動畫起始點
                    isEntering = true
                }
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
    val dockApps by viewModel.dockItems.collectAsState()
    var folderToOpenId by remember { mutableStateOf<String?>(null) }
    val openFolder = remember(folderToOpenId, pages, dockApps) {
        pages.flatten().find { it.uniqueId == folderToOpenId } 
            ?: dockApps.find { it.uniqueId == folderToOpenId }
    }
    var folderIconPosition by remember { mutableStateOf(Offset.Zero) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var showDeletePageConfirm by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showShortcutPicker by remember { mutableStateOf(false) }
    var widgetTargetPage by remember { mutableStateOf<Int?>(null) }

    // 新增：快速編輯 App 的狀態
    var appToEdit by remember { mutableStateOf<AppModel?>(null) }
    var appToUnfreeze by remember { mutableStateOf<AppModel?>(null) }

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
    var showDockAddTypePicker by remember { mutableStateOf<Int?>(null) }

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
    if (folderToOpenId != null) BackHandler { folderToOpenId = null }

    val desktopPageCount = pages.size.coerceAtLeast(1)
    val minusOneCount = if (showMinusOnePage) 1 else 0
    val libraryCount = if (showAppLibrary) 1 else 0
    val pageCount = minusOneCount + desktopPageCount + libraryCount
    
    val pagerState = rememberPagerState(
        initialPage = if (showMinusOnePage) 1 else 0,
        pageCount = { pageCount }
    )
    
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

    // 處理返回按鍵以返回主頁面，並防止在主頁面按下返回鍵導致 Activity 重啟（刷新）
    BackHandler(enabled = !showGlobalSearch && !isEditMode && folderToOpenId == null) {
        if (pagerState.currentPage != desktopStartIndex) {
            scope.launch { pagerState.animateScrollToPage(desktopStartIndex) }
        }
    }

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
    
    // 效能優化：統一收集通知狀態，避免 AppItem 集體重組
    val notificationCounts by NotificationService.notifications.collectAsState()
    val emojiWallpaperText by viewModel.emojiWallpaperText.collectAsState()

    // 檢查 App Library 是否處於搜尋模式
    val librarySearchQuery by viewModel.searchQuery.collectAsState()
    val libraryCategory by viewModel.selectedCategory.collectAsState()
    val isLibrarySearchFocused by viewModel.isLibrarySearchFocused.collectAsState()
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isLibraryInSearchMode = isAppLibraryPage && (isLibrarySearchFocused || isImeVisible || librarySearchQuery.isNotEmpty() || (libraryCategory != null && libraryCategory != "All"))

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenRatio = maxHeight / maxWidth
        
        val userRows by viewModel.desktopRows.collectAsState()
        val isBalanced = userRows == -1
        val rows = if (isBalanced) 6 else if (userRows > 0) userRows else (if (screenRatio < 2.0f) 5 else 6)
        
        val showWidgetLabel = if (rows >= 7) screenRatio >= 2.22f else true

        // 畫質調整不會影響這個顯示尺寸
        val baseIconSize = if (isBalanced) 61.5.dp else 62.dp
        val iconSize = baseIconSize * iconScaleFactor
        val labelFontSize = if (isBalanced) 11.8.sp else 12.sp
        val iconSizePx = with(density) { iconSize.toPx() }
        val columns = 4
        
        val horizontalPadding = if (isBalanced) 18.dp else 16.dp

        LaunchedEffect(columns, rows) { viewModel.setPageSize(columns * rows) }

        LaunchedEffect(draggingApp) {
            if (draggingApp == null) return@LaunchedEffect
            while (true) {
                val finalX = touchPosition.x + dragOffset.x
                val edgeWidth = with(density) { 45.dp.toPx() }
                
                if (finalX < edgeWidth && pagerState.currentPage > desktopStartIndex) {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    delay(800)
                } else if (finalX > with(density) { maxWidth.toPx() } - edgeWidth && pagerState.currentPage < desktopStartIndex + desktopPageCount - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    delay(800)
                }
                delay(100)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val combinedScale = launcherScale * enterScale
                    scaleX = combinedScale
                    scaleY = combinedScale
                    alpha = enterAlpha
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurPx = with(density) { (launcherBlur + enterBlur).toPx() }
                        renderEffect = if (blurPx > 0f) BlurEffect(blurPx, blurPx) else null
                    }
                }
        ) {
            // 1.1 桌布與圖案層 (統一採樣範圍)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                // 底層：系統桌布圖片 (在 Lite/Balance 下是 1x1 純色)
                rawWallpaper?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 中層：Lite 模式即時渲染的 Emoji 陣列
                if (emojiWallpaperText.isNotEmpty()) {
                    val emojis = remember(emojiWallpaperText) { parseEmojis(emojiWallpaperText) }
                    
                    if (emojis.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawEmojiPattern(
                                drawContext.canvas.nativeCanvas,
                                emojis,
                                size.width.toInt(),
                                size.height.toInt()
                            )
                        }
                    }
                }
            }
            
            // 獨立的模糊處理層 (Android 12 以下)
            Box(modifier = Modifier.fillMaxSize()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    blurredWallpaper?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = (launcherBlur.value / 20f).coerceIn(0f, 1f)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - (launcherBlur.value / 20f).coerceIn(0f, 1f)
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = draggingApp == null && !isLibraryInSearchMode,
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.4f,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 180f
                        )
                    )
                ) { pageIndex ->
                val pageOffsetProvider = remember(pagerState, pageIndex) {
                    { (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction }
                }

                val isMinusOne = showMinusOnePage && pageIndex == 0
                val isLibrary = showAppLibrary && pageIndex == pageCount - 1
                
                val isDesktop = pageIndex >= desktopStartIndex && pageIndex < desktopStartIndex + desktopPageCount

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(
                            top = if (isDesktop && isBalanced) 22.dp else 0.dp,
                            bottom = if (isLibrary || isMinusOne) 0.dp else (if (isBalanced) 168.dp else 158.dp)
                        )
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
                                },
                                onAppClick = onAppClick
                            )
                        }
                        isDesktop -> {
                            val desktopIdx = pageIndex - desktopStartIndex
                            AppGrid(
                                apps = pages.getOrNull(desktopIdx) ?: emptyList(),
                                columns = columns, rows = rows, iconSize = iconSize,
                                horizontalPadding = horizontalPadding,
                                labelFontSize = labelFontSize,
                                isEditMode = isEditMode,
                                viewModel = viewModel,
                                pageOffsetProvider = pageOffsetProvider,
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
                                    } else if (app.isFrozen) {
                                        appToUnfreeze = app
                                    } else viewModel.launchApp(app)
                                },
                                onSlotPositioned = { idx, rect ->
                                    slotBounds["$pageIndex-$idx"] = rect
                                },
                                onDragStart = { app, offset ->
                                    if (!isDesktopLocked) {
                                        viewModel.prepareForDrag()
                                        draggingApp = app
                                        touchPosition = offset
                                        dragOffset = Offset.Zero
                                    }
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
                                                        desktopPageCount - 1
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
                                        if (isDesktopLocked) {
                                            showDesktopMenu = true
                                        } else {
                                            performGestureAction(longPressAction, longPressApp)
                                        }
                                    }
                                },
                                onBackgroundClick = {
                                    if (isEditMode) viewModel.setEditMode(false)
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
                                onBackgroundDragY = { offset ->
                                    if (!isEditMode && swipeDownAction == GestureAction.OPEN_GLOBAL_SEARCH) {
                                        if (offset > 0) searchDragOffset = offset
                                    }
                                },
                                onBackgroundDragEnd = { finalOffset ->
                                    if (!isEditMode && swipeDownAction == GestureAction.OPEN_GLOBAL_SEARCH) {
                                        if (finalOffset > 80f) {
                                            showGlobalSearch = true
                                        }
                                        searchDragOffset = 0f
                                    }
                                },
                                onEditApp = { appToEdit = it },
                                showWidgetLabel = showWidgetLabel
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
                                horizontalPadding = if (isBalanced) 28.dp else horizontalPadding,
                                iconSize = (if (isBalanced) 70.dp else 72.dp) * iconScaleFactor,
                                labelFontSize = if (isBalanced) 11.sp else labelFontSize,
                                onAppClick = { app ->
                                    if (app.isFolder) {
                                        folderToOpenId = app.uniqueId
                                    } else if (app.isFrozen) {
                                        appToUnfreeze = app
                                    } else {
                                        onAppClick(app)
                                    }
                                },
                                onDragStart = { app, offset ->
                                    if (!isDesktopLocked) {
                                        draggingApp = app
                                        touchPosition = offset
                                        dragOffset = Offset.Zero
                                    }
                                },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    if (draggingApp != null) viewModel.handleAppDrop(
                                        draggingApp!!.uniqueId,
                                        null,
                                        (pagerState.currentPage - desktopStartIndex).coerceIn(0, desktopPageCount - 1),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (1f - (launcherBlur.value / 20f).coerceIn(0f, 1f)) * dockVisibilityProgress
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {

                    LauncherBottomBar(
                        visibilityProgress = dockVisibilityProgress,
                        showPill = showPillTemporarily,
                        isLiquidGlassEnabled = isLiquidGlassEnabled,
                        isLiquidGlassDockEnabled = isLiquidGlassDockEnabled,
                        backdrop = backdrop,
                        iconSize = iconSize,
                        horizontalPadding = horizontalPadding,
                        iconShape = iconShape,
                        dockStyle = dockStyle,
                        dockCornerRadius = dockCornerRadius,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        desktopPageCount = desktopPageCount,
                        currentPage = pagerState.currentPage - desktopStartIndex,
                        dockApps = dockApps,
                        isEditMode = isEditMode,
                        myPackageName = myPackageName,
                        notificationCounts = notificationCounts,
                        onSearchClick = { showGlobalSearch = true },
                        onAppClick = { app ->
                            if (app.isFolder) {
                                folderToOpenId = app.uniqueId
                            } else if (app.isFrozen) {
                                appToUnfreeze = app
                            } else onAppClick(app)
                        },
                        onSettingsClick = onSettingsClick,
                        onLongClick = { showDockAddTypePicker = it },
                        onReplaceClick = { showDockPicker = it },
                        onDeleteClick = { app ->
                            if (app.isPWA) {
                                showNativeUninstallDialog(mContext, app.label) {
                                    viewModel.deletePWA(app)
                                }
                                return@LauncherBottomBar
                            }
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
            dragOffset = searchDragOffset,
            onDismiss = { showGlobalSearch = false },
            allApps = allAppsFlat,
            suggestedApps = viewModel.suggestedApps.collectAsState().value,
            onAppClick = { app ->
                if (app.isFrozen) appToUnfreeze = app
                else onAppClick(app)
            },
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
        pickedWallpaperUri = uri
    }

    if (pickedWallpaperUri != null) {
        WallpaperCropDialog(
            uri = pickedWallpaperUri!!,
            onDismiss = { pickedWallpaperUri = null },
            onConfirm = { croppedBitmap ->
                viewModel.setCustomWallpaperColor(0) // 清除純色記錄，標記為圖片桌布
                viewModel.setEmojiWallpaperText("") // 清除 Emoji
                viewModel.setCustomWallpaper(croppedBitmap)
                pickedWallpaperUri = null
            }
        )
    }

    LauncherOverlays(
        viewModel = viewModel,
        showDesktopMenu = showDesktopMenu,
        onDismissDesktopMenu = { showDesktopMenu = false },
        showCreateFolderDialog = showCreateFolderDialog,
        onShowCreateFolder = { showCreateFolderDialog = true },
        onDismissCreateFolder = { showCreateFolderDialog = false },
        showDeleteFolderConfirm = showDeleteFolderConfirm,
        onShowDeleteFolderConfirm = { showDeleteFolderConfirm = true },
        onDismissDeleteFolder = { showDeleteFolderConfirm = false },
        showDeletePageConfirm = showDeletePageConfirm,
        onDismissDeletePage = { showDeletePageConfirm = false },
        onShowDeletePageConfirm = { showDeletePageConfirm = true },
        onDeletePage = { pageIdx ->
            val targetDesktopPage = if (pageIdx > 0) pageIdx - 1 else 0
            val absoluteTargetPage = targetDesktopPage + desktopStartIndex
            
            viewModel.deletePage(pageIdx)
            
            scope.launch {
                delay(50)
                if (absoluteTargetPage < pagerState.pageCount) {
                    pagerState.animateScrollToPage(absoluteTargetPage)
                }
            }
        },
        showWidgetPicker = showWidgetPicker,
        onDismissWidgetPicker = {
            showWidgetPicker = false
            widgetTargetPage = null
        },
        showShortcutPicker = showShortcutPicker,
        onDismissShortcutPicker = { showShortcutPicker = false },
        showDockPicker = showDockPicker,
        onDismissDockPicker = { showDockPicker = null },
        showDockAddTypePicker = showDockAddTypePicker,
        onDismissDockAddTypePicker = { showDockAddTypePicker = null },
        onSelectDockApp = { showDockPicker = it },
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
        onAddShortcutClick = { showShortcutPicker = true },
        onWallpaperClick = { showWallpaperTypeDialog = true },
        onSettingsClick = onSettingsClick,
        onAppClick = { app ->
            if (app.isFrozen) appToUnfreeze = app
            else onAppClick(app)
        }
    )

    if (showWallpaperTypeDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperTypeDialog = false },
            title = { Text(stringResource(R.string.menu_wallpaper)) },
            text = { Text(stringResource(R.string.select_wallpaper_type_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showWallpaperTypeDialog = false
                    wallpaperLauncher.launch("image/*")
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.widget_photo))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWallpaperTypeDialog = false
                    showColorPickerByWallpaper = true
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.style_custom))
                    }
                }
            }
        )
    }

    if (showColorPickerByWallpaper) {
        var selectedColor by remember { mutableIntStateOf(0xFF2196F3.toInt()) }
        var emojiText by remember { mutableStateOf("") }
        val favorites by viewModel.favoriteWallpaperColors.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showColorPickerByWallpaper = false },
            title = { Text(stringResource(R.string.custom_style_title)) },
            text = {
                ColorPickerInternal(
                    initialColor = selectedColor,
                    onColorChanged = { selectedColor = it },
                    emojiText = emojiText,
                    onEmojiChanged = { emojiText = it },
                    favorites = favorites,
                    onAddFavorite = { viewModel.addFavoriteWallpaperColor(it) },
                    onRemoveFavorite = { viewModel.removeFavoriteWallpaperColor(it) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (emojiText.isBlank()) {
                        // 優化：如果沒有 Emoji，生成 1x1 的純色 Bitmap
                        val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
                            eraseColor(selectedColor)
                        }
                        viewModel.setCustomWallpaperColor(selectedColor)
                        viewModel.setEmojiWallpaperText("") // 清除 Lite 模式 Emoji
                        viewModel.setCustomWallpaper(bitmap)
                        showColorPickerByWallpaper = false
                    } else {
                        // 如果有 Emoji，先彈出模式選擇對話框
                        showEmojiModeSelection = EmojiSelectionData(selectedColor, emojiText)
                        showColorPickerByWallpaper = false
                    }
                }) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerByWallpaper = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEmojiModeSelection != null) {
        val data = showEmojiModeSelection!!
        AlertDialog(
            onDismissRequest = { showEmojiModeSelection = null },
            title = { Text(stringResource(R.string.wallpaper_mode_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_lite)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_lite_desc)) },
                        leadingContent = { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
                                eraseColor(data.color)
                            }
                            viewModel.setCustomWallpaperColor(data.color)
                            viewModel.setEmojiWallpaperText(data.emojiText)
                            viewModel.setCustomWallpaper(bitmap)
                            showEmojiModeSelection = null
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_balance)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_balance_desc)) },
                        leadingContent = { Icon(Icons.Default.Balance, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            // 1. 生成 Lite Bitmap (1x1)
                            val liteBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
                                eraseColor(data.color)
                            }
                            
                            // 2. 生成 Full Bitmap (全螢幕)
                            val dm = mContext.resources.displayMetrics
                            val fullBitmap = android.graphics.Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(fullBitmap)
                            canvas.drawColor(data.color)
                            
                            val emojis = parseEmojis(data.emojiText)
                            if (emojis.isNotEmpty()) {
                                drawEmojiPattern(canvas, emojis, dm.widthPixels, dm.heightPixels)
                            }
                            
                            viewModel.setCustomWallpaperColor(data.color)
                            viewModel.setEmojiWallpaperText(data.emojiText) // 啟動器層需要繪製
                            viewModel.setBalanceWallpaper(fullBitmap, liteBitmap)
                            showEmojiModeSelection = null
                        }
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_full)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_full_desc)) },
                        leadingContent = { Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            val dm = mContext.resources.displayMetrics
                            val b = android.graphics.Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(b)
                            canvas.drawColor(data.color)
                            
                            val emojis = parseEmojis(data.emojiText)
                            if (emojis.isNotEmpty()) {
                                drawEmojiPattern(canvas, emojis, dm.widthPixels, dm.heightPixels)
                            }
                            viewModel.setCustomWallpaperColor(data.color)
                            viewModel.setEmojiWallpaperText("") // Full 模式不需要啟動器層 Emoji
                            viewModel.setCustomWallpaper(b)
                            showEmojiModeSelection = null
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (isApplyingWallpaper) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.applying_wallpaper),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.applying_wallpaper_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    val showVNaviInstallDialog by viewModel.showVNaviInstallDialog.collectAsState()
    val vNaviInstallUrl = stringResource(R.string.vnavi_install_url)

    if (showVNaviInstallDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVNaviInstallDialog() },
            title = { Text(stringResource(R.string.vnavi_install_title)) },
            text = { Text(stringResource(R.string.vnavi_install_msg)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissVNaviInstallDialog()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vNaviInstallUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        mContext.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("Iteration", "Failed to open vNavi download URL", e)
                    }
                }) {
                    Text(stringResource(R.string.vnavi_install_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissVNaviInstallDialog() }) {
                    Text(stringResource(R.string.vnavi_install_no))
                }
            }
        )
    }

    if (appToUnfreeze != null) {
        AlertDialog(
            onDismissRequest = { appToUnfreeze = null },
            title = { Text(stringResource(R.string.unfreeze_dialog_title)) },
            text = { Text(stringResource(R.string.unfreeze_dialog_msg)) },
            confirmButton = {
                Button(onClick = {
                    appToUnfreeze?.let { viewModel.toggleFreezeApp(it, mContext) }
                    appToUnfreeze = null
                }) { Text(stringResource(R.string.unfreeze)) }
            },
            dismissButton = {
                TextButton(onClick = { appToUnfreeze = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun ColorPickerInternal(
    initialColor: Int,
    onColorChanged: (Int) -> Unit,
    emojiText: String,
    onEmojiChanged: (String) -> Unit,
    favorites: List<Int> = emptyList(),
    onAddFavorite: (Int) -> Unit = {},
    onRemoveFavorite: (Int) -> Unit = {}
) {
    var hexText by remember(initialColor) { mutableStateOf(String.format("%06X", initialColor and 0xFFFFFF)) }
    val hsv = remember(initialColor) {
        val res = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, res)
        res
    }
    var h by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var s by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var v by remember(initialColor) { mutableFloatStateOf(hsv[2]) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(androidx.compose.ui.graphics.Color(initialColor), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (emojiText.isNotEmpty()) {
                Text(text = emojiText, fontSize = 32.sp)
            }
            
            // Add Favorite Button
            IconButton(
                onClick = { onAddFavorite(initialColor) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    imageVector = if (favorites.contains(initialColor)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (favorites.contains(initialColor)) Color.Red else Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        if (favorites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(favorites.size) { index ->
                    val color = favorites[index]
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(color))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .pointerInput(color) {
                                detectTapGestures(
                                    onTap = { onColorChanged(color) },
                                    onLongPress = { onRemoveFavorite(color) }
                                )
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = emojiText,
            onValueChange = onEmojiChanged,
            label = { Text(stringResource(R.string.emoji_label)) },
            placeholder = { Text(stringResource(R.string.emoji_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = hexText,
            onValueChange = {
                val filtered = it.uppercase().filter { char -> char in "0123456789ABCDEF" }.take(6)
                hexText = filtered
                if (filtered.length == 6) {
                    try {
                        val color = android.graphics.Color.parseColor("#$filtered")
                        onColorChanged(0xFF000000.toInt() or color)
                        val newHsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(0xFF000000.toInt() or color, newHsv)
                        h = newHsv[0]
                        s = newHsv[1]
                        v = newHsv[2]
                    } catch (e: Exception) {}
                }
            },
            label = { Text(stringResource(R.string.hex_color_label)) },
            prefix = { Text("#") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.hue, h.toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = h, onValueChange = { h = it; onColorChanged(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))) }, valueRange = 0f..360f)
        
        Text(stringResource(R.string.saturation, (s * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = s, onValueChange = { s = it; onColorChanged(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
        
        Text(stringResource(R.string.brightness, (v * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = v, onValueChange = { v = it; onColorChanged(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
    }
}

private fun parseEmojis(text: String): List<String> {
    val list = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(text)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        list.add(text.substring(start, end))
        start = end
        end = it.next()
    }
    return list
}

private fun drawEmojiPattern(canvas: android.graphics.Canvas, emojis: List<String>, width: Int, height: Int) {
    val columns = 5
    val itemWidth = width / columns.toFloat()
    val itemHeight = itemWidth * 1.2f
    val rows = (height / itemHeight).toInt() + 2
    
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = itemWidth * 0.6f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    for (row in 0 until rows) {
        for (col in 0 until columns) {
            val emojiIndex = (row + col) % emojis.size
            val x = col * itemWidth + itemWidth / 2f
            val y = row * itemHeight + itemHeight / 2f - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText(emojis[emojiIndex], x, y, paint)
        }
    }
}
