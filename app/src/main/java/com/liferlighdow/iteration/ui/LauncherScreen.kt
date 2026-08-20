package com.liferlighdow.iteration.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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

    val iconCornerRadius by viewModel.iconCornerRadius.collectAsState()
    val libraryCornerRadius by viewModel.libraryCornerRadius.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    val doubleTapAction by viewModel.doubleTapAction.collectAsState()
    val swipeUpAction by viewModel.swipeUpAction.collectAsState()
    val doubleTapApp by viewModel.doubleTapApp.collectAsState()
    val swipeUpApp by viewModel.swipeUpApp.collectAsState()
    val swipeDownAction by viewModel.swipeDownAction.collectAsState()
    val isSwipeDownSplit by viewModel.isSwipeDownSplit.collectAsState()
    val swipeDownLeftAction by viewModel.swipeDownLeftAction.collectAsState()
    val swipeDownRightAction by viewModel.swipeDownRightAction.collectAsState()
    val swipeDownLeftApp by viewModel.swipeDownLeftApp.collectAsState()
    val swipeDownRightApp by viewModel.swipeDownRightApp.collectAsState()
    val longPressAction by viewModel.longPressAction.collectAsState()
    val swipeDownApp by viewModel.swipeDownApp.collectAsState()
    val longPressApp by viewModel.longPressApp.collectAsState()
    val twoFingerSwipeUpAction by viewModel.twoFingerSwipeUpAction.collectAsState()
    val twoFingerSwipeDownAction by viewModel.twoFingerSwipeDownAction.collectAsState()
    val twoFingerSwipeUpApp by viewModel.twoFingerSwipeUpApp.collectAsState()
    val twoFingerSwipeDownApp by viewModel.twoFingerSwipeDownApp.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val dockCornerRadius by viewModel.dockCornerRadius.collectAsState()
    val dockOffset by viewModel.dockOffset.collectAsState()
    val showNavigationBar by viewModel.showNavigationBar.collectAsState()
    val showMinusOnePage by viewModel.showMinusOnePage.collectAsState()
    val showAppLibrary by viewModel.showAppLibrary.collectAsState()
    val isApplyingWallpaper by viewModel.isApplyingWallpaper.collectAsState()
    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
    val iconScaleFactor by viewModel.iconScale.collectAsState()
    val userRows by viewModel.desktopRows.collectAsState()
    val isBalanced = userRows == -1
    val baseIconSize = if (isBalanced) 61.5.dp else 62.dp
    val iconSize = baseIconSize * iconScaleFactor

    val pagerSnapThreshold by viewModel.pagerSnapThreshold.collectAsState()
    val pagerDampingRatio by viewModel.pagerDampingRatio.collectAsState()

    var showDesktopMenu by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var searchDragOffset by remember { mutableStateOf(0f) }

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
    var doneButtonPosition by remember { mutableStateOf(Offset.Zero) }
    var emojiAnimStartPos by remember { mutableStateOf<Offset?>(null) }
    var showFloatingEmoji by remember { mutableStateOf(false) }
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
    var lastDraggingApp by remember { mutableStateOf<AppModel?>(null) }
    val draggingAlpha by animateFloatAsState(
        targetValue = if (draggingApp != null) 1f else 0f,
        animationSpec = if (draggingApp != null) snap() else tween(250),
        label = "draggingAlpha"
    )

    LaunchedEffect(draggingApp) {
        if (draggingApp != null) lastDraggingApp = draggingApp
    }

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

    LaunchedEffect(rawHoveredKey, confirmedIntent) {
        if (rawHoveredKey == null) {
            delay(80) // 離開後稍微延遲再清空，增加佈局穩定性
            confirmedHoveredKey = null
            return@LaunchedEffect
        }
        
        // 排序意圖：快速反應，但仍給予微小緩衝避免閃爍
        delay(80)
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
    val emojiPatternStyle by viewModel.emojiPatternStyle.collectAsState()

    // 檢查 App Library 是否處於搜尋模式
    val librarySearchQuery by viewModel.searchQuery.collectAsState()
    val libraryCategory by viewModel.selectedCategory.collectAsState()
    val isLibrarySearchFocused by viewModel.isLibrarySearchFocused.collectAsState()
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isLibraryInSearchMode = isAppLibraryPage && (isLibrarySearchFocused || isImeVisible || librarySearchQuery.isNotEmpty() || (libraryCategory != null && libraryCategory != "All"))

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val edgeThresholdPx = with(density) { 40.dp.toPx() }

        // --- 修正後的拖拽邊緣切頁邏輯 ---
        var lastEdgeTriggerTime by remember { mutableLongStateOf(0L) }

        LaunchedEffect(draggingApp != null) {
            if (draggingApp == null) return@LaunchedEffect

            while (true) {
                val currentX = touchPosition.x + dragOffset.x
                val isAtRightEdge = currentX > screenWidthPx - edgeThresholdPx
                val isAtLeftEdge = currentX < edgeThresholdPx

                if (isAtRightEdge || isAtLeftEdge) {
                    if (lastEdgeTriggerTime == 0L) {
                        lastEdgeTriggerTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - lastEdgeTriggerTime > 800) {
                        // 觸發翻頁或新增
                        val currentPage = pagerState.currentPage
                        if (isAtRightEdge) {
                            if (currentPage < desktopStartIndex + desktopPageCount - 1) {
                                pagerState.animateScrollToPage(currentPage + 1)
                            } else if (currentPage == desktopStartIndex + desktopPageCount - 1) {
                                viewModel.addEmptyPage()
                                delay(100) // 等待列表更新
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else if (isAtLeftEdge) {
                            if (currentPage > desktopStartIndex) {
                                pagerState.animateScrollToPage(currentPage - 1)
                            }
                        }
                        lastEdgeTriggerTime = 0L // 觸發後重置計時
                        delay(1000) // 防止連續快速翻頁
                    }
                } else {
                    lastEdgeTriggerTime = 0L
                }
                delay(50) // 每 50ms 檢查一次位置
            }
        }

        val screenRatio = maxHeight / maxWidth

        val rows = if (isBalanced) 6 else if (userRows > 0) userRows else (if (screenRatio < 2.0f) 5 else 6)

        val showWidgetLabel = if (rows >= 7) screenRatio >= 2.22f else true

        // 畫質調整不會影響這個顯示尺寸
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
                                size.height.toInt(),
                                emojiPatternStyle
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
                        snapPositionalThreshold = pagerSnapThreshold,
                        snapAnimationSpec = spring(
                            dampingRatio = pagerDampingRatio,
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
                                onRemoveWidget = { viewModel.removeWidgetWithAnimation(it) },
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
                                iconCornerRadius = iconCornerRadius,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration,
                                pageIndex = pageIndex,
                                confirmedHoveredSlotIdx = if (confirmedHoveredKey?.startsWith("$pageIndex-") == true)
                                    confirmedHoveredKey?.substringAfter("-")?.toIntOrNull() else null,
                                draggingApp = draggingApp,
                                onAppClick = { app, pos ->
                                    if (isEditMode) {
                                        if (!app.isFolder) appToEdit = app
                                        return@AppGrid
                                    }
                                    
                                    // 計算點擊位置的 Rect (用於啟動動畫)
                                    val density = mContext.resources.displayMetrics.density
                                    val sizePx = (iconSize.value * density).toInt()
                                    val rect = android.graphics.Rect(
                                        pos.x.toInt(), 
                                        pos.y.toInt(), 
                                        (pos.x + sizePx).toInt(), 
                                        (pos.y + sizePx).toInt()
                                    )

                                    if (app.isFolder) {
                                        folderIconPosition = pos
                                        folderToOpenId = app.uniqueId
                                    } else if (app.isFrozen) {
                                        appToUnfreeze = app
                                    } else viewModel.launchApp(app, rect)
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
                                        emojiAnimStartPos = null // 非鎖定狀態清空動畫坐標
                                    } else {
                                        emojiAnimStartPos = offset
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
                                    rawHoveredKey = if (maxOverlap > 0.15f) bestKey else null
                                    
                                    // 調整門檻：提高 FOLDER 的門檻，讓使用者必須更精確地「瞄準」
                                    // 同時讓 REORDER 更難誤觸
                                    confirmedIntent = MainViewModel.DropType.REORDER
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
                                            viewModel.handleAppDrop(
                                                fromId = draggingApp!!.uniqueId,
                                                targetId = targetApp?.uniqueId,
                                                targetPageIndex = tPageIdx - desktopStartIndex,
                                                targetSlotIndex = tSlotIdx,
                                                isFromLibrary = false
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
                                                    fromId = draggingApp!!.uniqueId,
                                                    targetId = null,
                                                    targetPageIndex = targetIdx,
                                                    targetSlotIndex = null,
                                                    isFromLibrary = false
                                                )
                                            }
                                        }
                                    } else if (isEditMode && isDesktopLocked && emojiAnimStartPos != null) {
                                        // 鎖定狀態下嘗試拖動，放開後觸發👆漂浮
                                        showFloatingEmoji = true
                                    }
                                    
                                    // 在拖拽結束時，命令 ViewModel 檢查並清理多餘的空白頁面
                                    // 我們傳入當前頁面索引，避免刪掉使用者正看著的那一頁
                                    viewModel.cleanupEmptyPages(pagerState.currentPage - desktopStartIndex)

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
                                onBackgroundSwipeDown = { x ->
                                    if (isSwipeDownSplit) {
                                        val isLeft = x < screenWidthPx / 2
                                        if (isLeft) {
                                            performGestureAction(swipeDownLeftAction, swipeDownLeftApp)
                                        } else {
                                            performGestureAction(swipeDownRightAction, swipeDownRightApp)
                                        }
                                    } else {
                                        performGestureAction(swipeDownAction, swipeDownApp)
                                    }
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
                                iconCornerRadius = iconCornerRadius,
                                libraryCornerRadius = libraryCornerRadius,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration,
                                horizontalPadding = if (isBalanced) 28.dp else horizontalPadding,
                                iconSize = (if (isBalanced) 70.dp else 72.dp) * iconScaleFactor,
                                labelFontSize = if (isBalanced) 11.sp else labelFontSize,
                                onAppClick = { app, pos ->
                                    if (app.isFolder) {
                                        folderToOpenId = app.uniqueId
                                    } else if (app.isFrozen) {
                                        appToUnfreeze = app
                                    } else {
                                        val density = mContext.resources.displayMetrics.density
                                        val sizePx = (iconSize.value * density).toInt()
                                        val rect = android.graphics.Rect(
                                            pos.x.toInt(), 
                                            pos.y.toInt(), 
                                            (pos.x + sizePx).toInt(), 
                                            (pos.y + sizePx).toInt()
                                        )
                                        viewModel.launchApp(app, rect)
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
                                        fromId = draggingApp!!.uniqueId,
                                        targetId = null,
                                        targetPageIndex = (pagerState.currentPage - desktopStartIndex).coerceIn(0, desktopPageCount - 1),
                                        targetSlotIndex = null,
                                        isFromLibrary = true
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
                Button(
                    onClick = { viewModel.setEditMode(false) },
                    modifier = Modifier.onGloballyPositioned {
                        doneButtonPosition = it.positionInRoot()
                    }
                ) {
                    Text(stringResource(R.string.done))
                }
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
                        iconCornerRadius = iconCornerRadius,
                        dockStyle = dockStyle,
                        dockCornerRadius = dockCornerRadius,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        desktopPageCount = desktopPageCount,
                        currentPage = pagerState.currentPage - desktopStartIndex,
                        dockOffset = dockOffset,
                        showNavigationBar = showNavigationBar,
                        dockApps = dockApps,
                        isEditMode = isEditMode,
                        myPackageName = myPackageName,
                        notificationCounts = notificationCounts,
                        onSearchClick = { showGlobalSearch = true },
                        onAppClick = { app, pos ->
                            if (app.isFolder) {
                                folderIconPosition = pos
                                folderToOpenId = app.uniqueId
                            } else if (app.isFrozen) {
                                appToUnfreeze = app
                            } else {
                                val density = mContext.resources.displayMetrics.density
                                val sizePx = (iconSize.value * density).toInt()
                                val rect = android.graphics.Rect(
                                    pos.x.toInt(), 
                                    pos.y.toInt(), 
                                    (pos.x + sizePx).toInt(), 
                                    (pos.y + sizePx).toInt()
                                )
                                viewModel.launchApp(app, rect)
                            }
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

        // Emoji 漂浮動畫 (置於最後以確保在所有 UI 之上)
        if (showFloatingEmoji && emojiAnimStartPos != null) {
            val animProgress = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
                showFloatingEmoji = false
                viewModel.setEditMode(false) // 碰觸到 Done 離開編輯模式
            }

            val currentX = emojiAnimStartPos!!.x + (doneButtonPosition.x + 50f - emojiAnimStartPos!!.x) * animProgress.value
            val currentY = emojiAnimStartPos!!.y + (doneButtonPosition.y + 20f - emojiAnimStartPos!!.y) * animProgress.value
            // 降低淡化速度：只在最後 8% 的路程中才淡出
            val currentAlpha = if (animProgress.value > 0.92f) 1f - (animProgress.value - 0.92f) * 12.5f else 1f

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ) {
                // 🤪 臉孔 (位於手指左下方)
                Text(
                    text = "🤪",
                    fontSize = 60.sp,
                    modifier = Modifier
                        .offset(
                            x = with(LocalDensity.current) { (currentX - 155f).toDp() },
                            y = with(LocalDensity.current) { (currentY + 135f).toDp() }
                        )
                        .graphicsLayer { 
                            alpha = currentAlpha
                        }
                )
                
                // 👆 手指
                Text(
                    text = "👆",
                    fontSize = 40.sp,
                    modifier = Modifier
                        .offset(
                            x = with(LocalDensity.current) { currentX.toDp() },
                            y = with(LocalDensity.current) { currentY.toDp() }
                        )
                        .graphicsLayer { 
                            alpha = currentAlpha
                            scaleX = 1.2f
                            scaleY = 1.2f
                        }
                )
            }
        }

        lastDraggingApp?.let { app ->
            if (draggingAlpha > 0f) {
                // 手中的圖示縮放
                val inHandScale by animateFloatAsState(1.2f, label = "inHandScale")
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = touchPosition.x + dragOffset.x - iconSizePx / 2
                            translationY = touchPosition.y + dragOffset.y - iconSizePx / 2
                            // 改為完全不透明 (1.0f)，讓 icon 看起來更紮實
                            alpha = draggingAlpha
                            scaleX = inHandScale
                            scaleY = inHandScale
                            // 稍微增加一點陰影感（如果硬體支援渲染效果，這裡可以用 renderEffect，目前先用 alpha 保證清晰）
                        }
                ) {
                    AppItem(
                        app = app,
                        iconSize = iconSize,
                        showLabel = false,
                        iconCornerRadius = iconCornerRadius,
                        libraryCornerRadius = iconCornerRadius,
                        isLiquidGlass = isLiquidGlassEnabled,
                        backdrop = backdrop,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        getIcon = { pkg -> viewModel.getIcon(pkg) })
                }
            }
        }

        GlobalSearchOverlay(
            isVisible = showGlobalSearch,
            dragOffset = searchDragOffset,
            onDismiss = { showGlobalSearch = false },
            allApps = allAppsFlat,
            suggestedApps = viewModel.suggestedApps.collectAsState().value,
            iconCornerRadius = iconCornerRadius,
            onAppClick = { app, pos ->
                if (app.isFrozen) appToUnfreeze = app
                else {
                    val density = mContext.resources.displayMetrics.density
                    val sizePx = (iconSize.value * density).toInt()
                    val rect = android.graphics.Rect(
                        pos.x.toInt(), 
                        pos.y.toInt(), 
                        (pos.x + sizePx).toInt(), 
                        (pos.y + sizePx).toInt()
                    )
                    viewModel.launchApp(app, rect)
                }
            },
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isLiquidGlassGlobalSearchEnabled = isLiquidGlassGlobalSearchEnabled,
            backdrop = backdrop,
            blurRadius = blurRadius,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
            chromaticAberration = chromaticAberration
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
        onDismissFolder = { folderToOpenId = null; viewModel.clearFocusState() },
        currentPage = pagerState.currentPage - desktopStartIndex,
        pages = pages,
        allAppsFlat = allAppsFlat,
        isDefaultLauncher = isDefaultLauncher,
        isEditMode = isEditMode,
        iconCornerRadius = iconCornerRadius,
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
        onWallpaperClick = { 
            val intent = Intent(mContext, com.liferlighdow.iteration.SettingsActivity::class.java).apply {
                putExtra("start_page", "WALLPAPER")
            }
            mContext.startActivity(intent)
        },
        onSettingsClick = onSettingsClick,
        onAppClick = { app, pos ->
            if (app.isFrozen) appToUnfreeze = app
            else {
                val density = mContext.resources.displayMetrics.density
                val sizePx = (iconSize.value * density).toInt()
                val rect = android.graphics.Rect(
                    pos.x.toInt(), 
                    pos.y.toInt(), 
                    (pos.x + sizePx).toInt(), 
                    (pos.y + sizePx).toInt()
                )
                viewModel.launchApp(app, rect)
            }
        }
    )

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

fun parseEmojis(text: String): List<String> {
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

fun drawEmojiPattern(
    canvas: android.graphics.Canvas,
    emojis: List<String>,
    width: Int,
    height: Int,
    style: com.liferlighdow.iteration.data.EmojiPatternStyle = com.liferlighdow.iteration.data.EmojiPatternStyle.MEDIUM_GRID
) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
    }

    when (style) {
        com.liferlighdow.iteration.data.EmojiPatternStyle.SMALL_GRID,
        com.liferlighdow.iteration.data.EmojiPatternStyle.MEDIUM_GRID,
        com.liferlighdow.iteration.data.EmojiPatternStyle.LARGE_GRID -> {
            val columns = when (style) {
                com.liferlighdow.iteration.data.EmojiPatternStyle.SMALL_GRID -> 8
                com.liferlighdow.iteration.data.EmojiPatternStyle.LARGE_GRID -> 3
                else -> 5
            }
            val itemWidth = width / columns.toFloat()
            val itemHeight = itemWidth * 1.3f
            val rows = (height / itemHeight).toInt() + 2
            paint.textSize = itemWidth * 0.6f

            for (row in 0 until rows) {
                for (col in 0 until columns) {
                    val emojiIndex = (row + col) % emojis.size
                    val x = col * itemWidth + itemWidth / 2f
                    // 加入交錯偏移，讓佈局更有序但不死板
                    val offsetX = if (row % 2 == 1) itemWidth / 4f else -itemWidth / 4f
                    val y = row * itemHeight + itemHeight / 2f - ((paint.descent() + paint.ascent()) / 2f)
                    canvas.drawText(emojis[emojiIndex], x + offsetX, y, paint)
                }
            }
        }

        com.liferlighdow.iteration.data.EmojiPatternStyle.RINGS -> {
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 1.8f
            val ringSpacing = width / 5f
            paint.textSize = width / 10f

            var ring = 0
            var radius = ringSpacing / 2f
            while (radius < maxRadius) {
                val count = (2 * Math.PI * radius / (paint.textSize * 1.2)).toInt().coerceAtLeast(1)
                for (i in 0 until count) {
                    val angle = (2 * Math.PI * i / count) + (ring * 0.5) // 每圈旋轉一點
                    val x = centerX + (radius * Math.cos(angle)).toFloat()
                    val y = centerY + (radius * Math.sin(angle)).toFloat() - ((paint.descent() + paint.ascent()) / 2f)
                    canvas.drawText(emojis[i % emojis.size], x, y, paint)
                }
                radius += ringSpacing
                ring++
            }
        }

        com.liferlighdow.iteration.data.EmojiPatternStyle.SPIRAL -> {
            val centerX = width / 2f
            val centerY = height / 2f
            paint.textSize = width / 10f
            
            var angle = 0.0
            var radius = 0.0
            val growth = paint.textSize * 0.5 // 螺旋增長速度
            
            for (i in 0 until 100) { // 繪製 100 個 Emoji 構成螺旋
                val x = centerX + (radius * Math.cos(angle)).toFloat()
                val y = centerY + (radius * Math.sin(angle)).toFloat() - ((paint.descent() + paint.ascent()) / 2f)
                
                if (x < -100 || x > width + 100 || y < -100 || y > height + 100) break
                
                canvas.drawText(emojis[i % emojis.size], x, y, paint)
                
                // 阿基米德螺旋方程微調
                angle += 0.6 // 旋轉角度增加
                radius += growth * 0.15 // 半徑增加
            }
        }
    }
}
