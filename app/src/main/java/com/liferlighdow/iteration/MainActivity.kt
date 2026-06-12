package com.liferlighdow.iteration

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.graphics.rememberGraphicsLayer
import java.util.Calendar
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            IterationTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    LauncherScreen(
                        viewModel = viewModel,
                        onAppClick = { pkg ->
                            viewModel.logAppLaunch(pkg)
                            val intent = packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) startActivity(intent)
                        },
                        onSettingsClick = {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

fun calculateOverlap(r1: Rect, r2: Rect): Float {
    val intersection = Rect(
        left = max(r1.left, r2.left),
        top = max(r1.top, r2.top),
        right = min(r1.right, r2.right),
        bottom = min(r1.bottom, r2.bottom)
    )
    return if (intersection.width > 0 && intersection.height > 0) {
        (intersection.width * intersection.height) / (r1.width * r1.height)
    } else 0f
}



@Composable
fun MinusOnePage(
    widgets: List<WidgetModel>,
    viewModel: MainViewModel,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    onAddClick: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetMode: (String, WidgetDisplayMode) -> Unit
) {
    var isReorderMode by remember { mutableStateOf(false) }
    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    val mContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { isReorderMode = !isReorderMode }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = if (isReorderMode) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(widgets, key = { it.id }, span = { widget ->
                val span = if ((widget.type as? WidgetType.Photo)?.isWide == true ||
                    (widget.type as? WidgetType.Calendar)?.isWide == true ||
                    (widget.type as? WidgetType.Music)?.isWide == true) 4 else 2
                GridItemSpan(span)
            }) { widget ->
                var showContextMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.pointerInput(widget.type) {
                        detectTapGestures(
                            onLongPress = {
                                if (widget.type is WidgetType.Stack) {
                                    stackToEdit = widget
                                } else {
                                    showContextMenu = true
                                }
                            },
                            onTap = {
                                when (widget.type) {
                                    is WidgetType.Clock -> {
                                        try {
                                            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            mContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val pm = mContext.packageManager
                                                val fallbackIntent = pm.getLaunchIntentForPackage("com.google.android.deskclock")
                                                    ?: pm.getLaunchIntentForPackage("com.android.deskclock")
                                                if (fallbackIntent != null) mContext.startActivity(fallbackIntent)
                                            } catch (e2: Exception) {}
                                        }
                                    }
                                    is WidgetType.Calendar -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_APP_CALENDAR)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            mContext.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                ) {
                    when (widget.type) {
                        is WidgetType.Battery -> BatteryWidget(displayMode = widget.displayMode, backdrop = backdrop)
                        is WidgetType.Clock -> AnalogClockWidget(displayMode = widget.displayMode, backdrop = backdrop)
                        is WidgetType.Calendar -> CalendarWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop)
                        is WidgetType.Photo -> PhotoWidget(widget = widget, viewModel = viewModel)
                        is WidgetType.Music -> MusicWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop)
                        is WidgetType.Stack -> StackWidget(widget = widget, viewModel = viewModel, backdrop = backdrop)
                    }

                    if (isReorderMode) {
                        IconButton(
                            onClick = { onRemoveWidget(widget.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.widget_glass_mode)) },
                            leadingIcon = { Icon(Icons.Default.BlurOn, null) },
                            onClick = {
                                onUpdateWidgetMode(widget.id, WidgetDisplayMode.GLASS)
                                showContextMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.widget_color_mode)) },
                            leadingIcon = { Icon(Icons.Default.Palette, null) },
                            onClick = {
                                onUpdateWidgetMode(widget.id, WidgetDisplayMode.COLOR)
                                showContextMenu = false
                            }
                        )
                        if (widget.type is WidgetType.Stack) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_choose_widgets)) },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = {
                                    stackToEdit = widget
                                    showContextMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (stackToEdit != null) {
        WidgetStackPickerDialog(
            currentChildren = (stackToEdit!!.type as WidgetType.Stack).children,
            viewModel = viewModel,
            onDismiss = { stackToEdit = null },
            onConfirm = { newChildren ->
                viewModel.updateStackChildren(stackToEdit!!.id, newChildren)
            }
        )
    }
}

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

    val mContext = LocalContext.current
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
        val resolveInfo = mContext.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
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

    var showDesktopMenu by remember { mutableStateOf(false) }
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

    var showGlobalSearch by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }

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
    
    if (showGlobalSearch) BackHandler { showGlobalSearch = false; globalSearchQuery = "" }

    if (isEditMode) BackHandler { viewModel.setEditMode(false) }

    val desktopPageCount = pages.size.coerceAtLeast(1)
    val pageCount = 1 + desktopPageCount + (if (draggingApp != null) 2 else 1)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    
    // 計算精確的連續分頁位置 (例如 1.1 代表離開第一頁往第二頁滑動了 10%)
    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction

    // 讓 Dock 顯示的條件更嚴苛，只要離開主畫面區域 15% (0.15f) 就開始隱藏
    // 主畫面分頁範圍是 1 到 desktopPageCount
    val showDockAndIndicator = continuousPage > 0.85f && continuousPage < (desktopPageCount + 0.15f)
    
    val isMinusOnePage = pagerState.currentPage == 0
    val isAppLibraryPage = pagerState.currentPage == pageCount - 1

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        
        // 徹底移除桌布 Image 層，回歸純粹的 UI 採樣

        val iconSize = 62.dp
        val iconSizePx = with(density) { iconSize.toPx() }
        val columns = 4
        val rows = if (maxHeight / maxWidth < 2.0f) 5 else 6
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

        // 1. 唯一的、真實的桌布背景層 (採樣源)
        // 將其作為 UI 的一部分繪製，確保 Backdrop 庫能捕獲到高清像素進行扭曲。
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

        // 2. 桌面內容層 (Pager, Icons, Widgets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(launcherBlur)
                .graphicsLayer {
                    scaleX = launcherScale
                    scaleY = launcherScale
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = draggingApp == null,
                beyondViewportPageCount = 1
            ) { pageIndex ->
                // 計算頁面偏移量 (-1.0 到 1.0)
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)

                val isMinusOne = pageIndex == 0
                val isLibrary = pageIndex == pageCount - 1
                val isDesktop = pageIndex >= 1 && pageIndex <= desktopPageCount
                val isNewPage = pageIndex == desktopPageCount + 1 && draggingApp != null

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = if (isLibrary || isMinusOne) 0.dp else 140.dp)
                        .pointerInput(draggingApp, isEditMode, isLibrary, isMinusOne) {
                            if (draggingApp == null && !isEditMode && !isLibrary && !isMinusOne) {
                                var totalDrag = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragCancel = { totalDrag = 0f },
                                    onDragEnd = { totalDrag = 0f }
                                ) { _, dragAmount ->
                                    totalDrag += dragAmount
                                    // 提高觸發閾值，且確保是明顯的向下滑動 (正值)
                                    // 50dp 左右的位移才觸發，避免誤觸
                                    if (totalDrag > 150f) {
                                        showGlobalSearch = true
                                        totalDrag = 0f
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { if (!isLibrary && !isMinusOne && !isEditMode) showDesktopMenu = true },
                                onTap = { if (isEditMode) viewModel.setEditMode(false) }
                            )
                        }
                ) {
                    when {
                        isMinusOne -> {
                            MinusOnePage(
                                widgets = minusOneWidgets,
                                viewModel = viewModel,
                                backdrop = backdrop,
                                onAddClick = { showWidgetPicker = true },
                                onRemoveWidget = { viewModel.removeWidget(it) },
                                onUpdateWidgetMode = { id, mode -> viewModel.updateWidgetDisplayMode(id, mode) }
                            )
                        }
                        isDesktop || isNewPage -> {
                            val desktopIdx = pageIndex - 1
                            AppGrid(
                                apps = pages.getOrNull(desktopIdx) ?: emptyList(),
                                columns = columns, rows = rows, iconSize = iconSize,
                                isEditMode = isEditMode,
                                viewModel = viewModel,
                                pageOffset = pageOffset,
                                isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                backdrop = backdrop,
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
                                onSlotPositioned = { idx, rect -> slotBounds["$pageIndex-$idx"] = rect },
                                onDragStart = { app, offset ->
                                    viewModel.prepareForDrag()
                                    draggingApp = app; touchPosition = offset; dragOffset = Offset.Zero
                                },
                                onDrag = { delta ->
                                    dragOffset += delta
                                    val currentPos = touchPosition + dragOffset
                                    val dragRect = Rect(currentPos.x - iconSizePx/2, currentPos.y - iconSizePx/2, currentPos.x + iconSizePx/2, currentPos.y + iconSizePx/2)
                                    var bestKey: String? = null
                                    var maxOverlap = 0f
                                    slotBounds.forEach { (key, rect) ->
                                        val overlap = calculateOverlap(rect, dragRect)
                                        if (overlap > maxOverlap) { maxOverlap = overlap; bestKey = key }
                                    }
                                    rawHoveredKey = bestKey
                                    confirmedIntent = if (!isEditMode && maxOverlap > 0.50f) MainViewModel.DropType.FOLDER else MainViewModel.DropType.REORDER
                                },
                                onDragEnd = {
                                    if (draggingApp != null) {
                                        val finalPos = touchPosition + dragOffset
                                        val dragRect = Rect(finalPos.x - iconSizePx/2, finalPos.y - iconSizePx/2, finalPos.x + iconSizePx/2, finalPos.y + iconSizePx/2)
                                        var bestKey: String? = null
                                        var maxOverlap = 0f
                                        slotBounds.forEach { (key, rect) ->
                                            val overlap = calculateOverlap(rect, dragRect)
                                            if (overlap > maxOverlap) { maxOverlap = overlap; bestKey = key }
                                        }
                                        if (bestKey != null) {
                                            val parts = bestKey!!.split("-")
                                            val tPageIdx = parts[0].toInt()
                                            val tSlotIdx = parts[1].toInt()
                                            val targetApp = pages.getOrNull(tPageIdx - 1)?.getOrNull(tSlotIdx)
                                            val dropType = if (!isEditMode && maxOverlap > 0.50f && targetApp != null) MainViewModel.DropType.FOLDER else MainViewModel.DropType.REORDER
                                            viewModel.handleAppDrop(draggingApp!!.uniqueId, targetApp?.uniqueId, tPageIdx - 1, false, dropType)
                                        } else {
                                            val currentPage = pagerState.currentPage
                                            if (currentPage == pageCount - 1) {
                                                viewModel.removeAppFromHome(draggingApp!!.uniqueId)
                                            } else {
                                                val targetIdx = (currentPage - 1).coerceIn(0, desktopPageCount)
                                                viewModel.handleAppDrop(draggingApp!!.uniqueId, null, targetIdx, false, MainViewModel.DropType.REORDER)
                                            }
                                        }
                                    }
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey = null
                                },
                                onBackgroundLongPress = {
                                    if (!isEditMode) showDesktopMenu = true
                                },
                                onBackgroundClick = {
                                    if (isEditMode) viewModel.setEditMode(false)
                                }
                            )
                        }
                        else -> {
                            AppLibraryPage(
                                allApps = allAppsFlat,
                                isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassAppLibraryFolderEnabled,
                                isSearchLiquidGlass = isLiquidGlassEnabled && isLiquidGlassAppLibrarySearchEnabled,
                                backdrop = backdrop,
                                onAppClick = { pkg ->
                                    val app = allAppsFlat.find { it.packageName == pkg }
                                    if (app?.isFolder == true) folderToOpenId = app.uniqueId else onAppClick(pkg)
                                },
                                onDragStart = { app, offset -> draggingApp = app; touchPosition = offset; dragOffset = Offset.Zero },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    if (draggingApp != null) viewModel.handleAppDrop(draggingApp!!.packageName, null, pagerState.currentPage - 1, true, MainViewModel.DropType.REORDER)
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey = null
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isEditMode, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), contentAlignment = Alignment.TopEnd) {
                    Button(onClick = { viewModel.setEditMode(false) }) { Text(stringResource(R.string.done)) }
                }
            }

            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
                BackHandler(enabled = (isAppLibraryPage || isMinusOnePage) && !showGlobalSearch) {
                    scope.launch { pagerState.animateScrollToPage(1) }
                }
                AnimatedVisibility(
                    visible = showDockAndIndicator,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PageIndicator(pageCount = desktopPageCount, currentPage = pagerState.currentPage - 1)
                        Dock(
                            apps = dockApps,
                            iconSize = iconSize,
                            isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassDockEnabled,
                            backdrop = backdrop,
                            onAppClick = { pkg -> if (pkg == myPackageName) onSettingsClick() else onAppClick(pkg) },
                            onLongClick = { showDockPicker = it }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        draggingApp?.let { app ->
            Box(
                modifier = Modifier
                    .offset { IntOffset((touchPosition.x + dragOffset.x - iconSizePx / 2).roundToInt(), (touchPosition.y + dragOffset.y - iconSizePx / 2).roundToInt()) }
                    .size(iconSize)
                    .graphicsLayer(alpha = 0.8f, scaleX = 1.15f, scaleY = 1.15f)
            ) { AppItem(app = app, iconSize = iconSize) }
        }
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val context = mContext
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
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

    if (showDesktopMenu) {
        ModalBottomSheet(onDismissRequest = { showDesktopMenu = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ListItem(headlineContent = { Text(stringResource(R.string.menu_edit_mode)) }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { viewModel.setEditMode(true); showDesktopMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.menu_add_widget)) }, leadingContent = { Icon(Icons.Default.Add, contentDescription = null) }, modifier = Modifier.clickable {
                    widgetTargetPage = pagerState.currentPage - 1
                    showWidgetPicker = true
                    showDesktopMenu = false
                })
                ListItem(headlineContent = { Text(stringResource(R.string.menu_new_folder)) }, leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }, modifier = Modifier.clickable { showCreateFolderDialog = true; showDesktopMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.menu_add_page)) }, leadingContent = { Icon(Icons.Default.PostAdd, contentDescription = null) }, modifier = Modifier.clickable { viewModel.addEmptyPage(); showDesktopMenu = false })

                // 只有在分頁是空白且總頁數大於 1 時才顯示刪除分頁選項
                val currentPageIdx = pagerState.currentPage - 1
                val isCurrentPageEmpty = pages.getOrNull(currentPageIdx)?.isEmpty() ?: false
                if (isCurrentPageEmpty && pages.size > 1) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_delete_page), color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deletePage(currentPageIdx)
                            showDesktopMenu = false
                        }
                    )
                }

                ListItem(headlineContent = { Text(stringResource(R.string.menu_wallpaper)) }, leadingContent = { Icon(Icons.Default.Image, contentDescription = null) }, modifier = Modifier.clickable {
                    wallpaperLauncher.launch("image/*")
                    showDesktopMenu = false
                })
                
                ListItem(
                    headlineContent = { Text("Sync System Wallpaper") }, 
                    supportingContent = { Text("Use phone's current default wallpaper") },
                    leadingContent = { Icon(Icons.Default.CloudSync, contentDescription = null) }, 
                    modifier = Modifier.clickable {
                        viewModel.grabSystemWallpaper {
                            Toast.makeText(mContext, "Android restriction: Please manually select the wallpaper file.", Toast.LENGTH_LONG).show()
                        }
                        showDesktopMenu = false
                    }
                )
                if (!isDefaultLauncher) {
                    ListItem(headlineContent = { Text(stringResource(R.string.menu_set_default)) }, leadingContent = { Icon(Icons.Default.Home, contentDescription = null) }, modifier = Modifier.clickable {
                        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                        mContext.startActivity(intent)
                        showDesktopMenu = false
                    })
                }
                ListItem(headlineContent = { Text(stringResource(R.string.menu_launcher_settings)) }, leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) }, modifier = Modifier.clickable { onSettingsClick(); showDesktopMenu = false })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(stringResource(R.string.folder_create_title)) },
            text = { OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text(stringResource(R.string.folder_name_hint)) }, singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.createFolder(pagerState.currentPage - 1, folderName); showCreateFolderDialog = false }) { Text(stringResource(R.string.create)) } },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showDeleteFolderConfirm) {
        val folder = openFolder
        if (folder != null) {
            AlertDialog(
                onDismissRequest = { showDeleteFolderConfirm = false },
                title = { Text(stringResource(R.string.folder_delete_confirm_title)) },
                text = { Text(stringResource(R.string.folder_delete_confirm_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFolder(folder.uniqueId, keepIcons = true)
                        folderToOpenId = null
                        showDeleteFolderConfirm = false
                    }) {
                        Text(stringResource(R.string.folder_delete_keep))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.deleteFolder(folder.uniqueId, keepIcons = false)
                        folderToOpenId = null
                        showDeleteFolderConfirm = false
                    }) {
                        Text(stringResource(R.string.folder_delete_discard))
                    }
                }
            )
        }
    }

    if (showDockPicker != null) {
        val visibleApps = allAppsFlat.filter { !it.isHidden }
        AppPickerDialog(
            allApps = visibleApps,
            onDismiss = { showDockPicker = null },
            onAppSelected = { pkg -> viewModel.updateDockApp(showDockPicker!!, pkg); showDockPicker = null }
        )
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            onDismiss = {
                showWidgetPicker = false
                widgetTargetPage = null
            },
            onWidgetSelected = {
                viewModel.addWidget(it, widgetTargetPage ?: -1)
                showWidgetPicker = false
                widgetTargetPage = null
            }
        )
    }

    if (appToEdit != null) {
        QuickEditDialog(
            app = appToEdit!!,
            viewModel = viewModel,
            onDismiss = { appToEdit = null }
        )
    }

    AnimatedVisibility(
        visible = openFolder != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val folder = openFolder ?: return@AnimatedVisibility

        // 使用自定義全螢幕 Overlay 取代 Dialog，以實現 iOS 感的縮放與模糊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable { folderToOpenId = null },
            contentAlignment = Alignment.Center
        ) {
            var isEditingName by remember { mutableStateOf(false) }
            var tempName by remember(folder.label) { mutableStateOf(folder.label) }
            var showMoreMenu by remember { mutableStateOf(false) }
            var showAppPicker by remember { mutableStateOf(false) }

            // 內層容器動畫
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .animateEnterExit(
                        enter = scaleIn(initialScale = 0.2f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = scaleOut(targetScale = 0.2f) + fadeOut()
                    )
                    .clickable(enabled = false) { } // 阻止點擊穿透
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        if (isEditingName) {
                            OutlinedTextField(value = tempName, onValueChange = { tempName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)), trailingIcon = { IconButton(onClick = { viewModel.updateFolderName(folder.uniqueId, tempName); isEditingName = false }) { Icon(Icons.Default.Check, null, tint = Color.White) } })
                        } else {
                            Text(text = folder.label, style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.clickable { isEditingName = true }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { isEditingName = true; showMoreMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.folder_add_app)) }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { showAppPicker = true; showMoreMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.folder_delete)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { showDeleteFolderConfirm = true; showMoreMenu = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val itemsPerPage = 9
                    val folderPages = remember(folder.folderItems) { folder.folderItems.chunked(itemsPerPage) }
                    val folderPagerState = rememberPagerState { folderPages.size }
                    
                    Box(
                        modifier = Modifier
                            .width(340.dp)
                            .height(375.dp)
                            .liquidGlass(
                                enabled = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                backdrop = backdrop,
                                cornerRadius = 32.dp
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        HorizontalPager(state = folderPagerState, modifier = Modifier.fillMaxSize()) { pageIdx ->
                            val pageItems = folderPages[pageIdx]
                            Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                pageItems.chunked(3).forEach { rowItems ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                        rowItems.forEach { app ->
                                            val lastPos = remember { object { var pos = Offset.Zero } }
                                            var showItemMenu by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f).onGloballyPositioned { lastPos.pos = it.positionInRoot() }, contentAlignment = Alignment.Center) {
                                                AppItem(
                                                    app = app,
                                                    onAppClick = { onAppClick(app.packageName); folderToOpenId = null },
                                                    iconSize = 64.dp,
                                                    isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                                    backdrop = backdrop,
                                                    modifier = Modifier.pointerInput(app.uniqueId) {
                                                        detectTapGestures(
                                                            onLongPress = { showItemMenu = true }
                                                        )
                                                    }
                                                )
                                                DropdownMenu(expanded = showItemMenu, onDismissRequest = { showItemMenu = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.menu_delete_home)) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                        onClick = { viewModel.removeAppFromFolder(folder.uniqueId, app.uniqueId); showItemMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.menu_uninstall)) },
                                                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                                        onClick = {
                                                            android.util.Log.d("Iteration", "Uninstalling: ${app.packageName}")
                                                            android.widget.Toast.makeText(mContext, "Uninstalling ${app.label}...", android.widget.Toast.LENGTH_SHORT).show()
                                                            try {
                                                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                                                    data = Uri.fromParts("package", app.packageName, null)
                                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                }
                                                                mContext.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                android.util.Log.e("Iteration", "Uninstall failed", e)
                                                            }
                                                            showItemMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                    if (folderPages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PageIndicator(pageCount = folderPages.size, currentPage = folderPagerState.currentPage)
                    }
                }
            }

            if (showAppPicker) {
                MultiAppPickerDialog(allApps = allAppsFlat, onDismiss = { showAppPicker = false }, onAppsSelected = { viewModel.addAppsToFolder(folder.uniqueId, it); showAppPicker = false })
            }
        }
    }

    AnimatedVisibility(
        visible = showGlobalSearch,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // 降低遮罩濃度，讓模糊後的顏色透出來
                .clickable { showGlobalSearch = false; globalSearchQuery = "" }
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).clickable(enabled = false) { }) {
                OutlinedTextField(
                    value = globalSearchQuery, onValueChange = { globalSearchQuery = it }, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .liquidGlass(
                            enabled = isLiquidGlassEnabled && isLiquidGlassGlobalSearchEnabled,
                            backdrop = backdrop,
                            cornerRadius = 28.dp
                        ),
                    placeholder = { Text(stringResource(R.string.search_hint), color = Color.White.copy(alpha = 0.6f)) }, 
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = {
                        if (globalSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { globalSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = Color.White)
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.2f), unfocusedContainerColor = Color.White.copy(alpha = 0.2f), focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent),
                    singleLine = true
                )
                val filteredResults = remember(globalSearchQuery, allAppsFlat) {
                    val base = allAppsFlat.filter { !it.isHidden }
                    if (globalSearchQuery.isBlank()) base.take(8)
                    else base.filter { it.label.contains(globalSearchQuery, ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (globalSearchQuery.isBlank() && filteredResults.isNotEmpty()) {
                        item { Text(stringResource(R.string.app_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    items(filteredResults, key = { it.uniqueId }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) }, leadingContent = { if (app.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.8.dp)).background(Color.White)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onAppClick(app.packageName); showGlobalSearch = false }
                        )
                    }

                    if (globalSearchQuery.isNotBlank()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                            Text(stringResource(R.string.more_searches), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.search_web), color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${globalSearchQuery}"))
                                    mContext.startActivity(intent)
                                    showGlobalSearch = false
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.search_store), color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Shop, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${globalSearchQuery}"))
                                    mContext.startActivity(intent)
                                    showGlobalSearch = false
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.search_maps), color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${globalSearchQuery}"))
                                    mContext.startActivity(intent)
                                    showGlobalSearch = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGrid(
    apps: List<AppModel>, columns: Int, rows: Int, iconSize: androidx.compose.ui.unit.Dp,
    draggingApp: AppModel?,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    pageOffset: Float = 0f,
    isLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    confirmedHoveredSlotIdx: Int?, confirmedIntent: MainViewModel.DropType,
    onAppClick: (AppModel, Offset) -> Unit,
    onSlotPositioned: (Int, Rect) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onBackgroundLongPress: () -> Unit = {},
    onBackgroundClick: () -> Unit = {}
) {
    val draggingUniqueId = draggingApp?.uniqueId
    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }

    val displayApps = remember(apps, confirmedHoveredSlotIdx, draggingUniqueId, confirmedIntent) {
        val list = apps.toMutableList()
        val fromIdx = list.indexOfFirst { it.uniqueId == draggingUniqueId }

        if (confirmedHoveredSlotIdx != null && confirmedIntent == MainViewModel.DropType.REORDER) {
            if (fromIdx != -1) {
                val item = list.removeAt(fromIdx)
                val targetIdx = confirmedHoveredSlotIdx.coerceIn(0, list.size)
                list.add(targetIdx, item)
            } else if (draggingApp != null) {
                val targetIdx = confirmedHoveredSlotIdx.coerceIn(0, list.size)
                list.add(targetIdx, draggingApp)
            }
        }
        list
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = { onBackgroundLongPress() },
                    onTap = { onBackgroundClick() }
                )
            }
    ) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows

        val grid = Array(rows) { BooleanArray(columns) { false } }

        displayApps.forEachIndexed { index, app ->
            val w: Int
            val h: Int
            if (app.isWidget) {
                when (val type = app.widget?.type) {
                    is WidgetType.Battery -> { w = 2; h = 2 }
                    is WidgetType.Clock -> { w = 2; h = 2 }
                    is WidgetType.Calendar -> { w = if (type.isWide) 4 else 2; h = 2 }
                    is WidgetType.Photo -> { w = if (type.isWide) 4 else 2; h = 2 }
                    is WidgetType.Music -> { w = if (type.isWide) 4 else 2; h = 2 }
                    is WidgetType.Stack -> { w = 2; h = 2 }
                    else -> { w = 1; h = 1 }
                }
            } else {
                w = 1; h = 1
            }

            // Find first available spot
            var foundRow = -1
            var foundCol = -1
            outer@for (r in 0 until rows) {
                for (c in 0 until columns) {
                    var canFit = true
                    if (r + h > rows || c + w > columns) {
                        canFit = false
                    } else {
                        for (tr in r until r + h) {
                            for (tc in c until c + w) {
                                if (grid[tr][tc]) {
                                    canFit = false
                                    break
                                }
                            }
                            if (!canFit) break
                        }
                    }

                    if (canFit) {
                        foundRow = r
                        foundCol = c
                        for (tr in r until r + h) {
                            for (tc in c until c + w) {
                                grid[tr][tc] = true
                            }
                        }
                        break@outer
                    }
                }
            }

            if (foundRow != -1) {
                val lastPosition = remember { object { var pos = Offset.Zero } }
                val isHoveredFolder = confirmedHoveredSlotIdx == index && confirmedIntent == MainViewModel.DropType.FOLDER
                val scale by animateFloatAsState(if (isHoveredFolder) 1.25f else 1.0f)
                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -2.5f, targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
                )

                // 使用 animateDpAsState 來讓位置變動更平滑
                val targetX = cellWidth * foundCol
                val targetY = cellHeight * foundRow
                val animX by animateDpAsState(targetX, label = "x")
                val animY by animateDpAsState(targetY, label = "y")

                // 計算圖示彈性位移 (iOS 感的關鍵)
                val density = androidx.compose.ui.platform.LocalDensity.current
                val elasticOffset = with(density) { pageOffset * (foundCol - (columns - 1) / 2f) * 12.dp.toPx() }
                var showContextMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .offset(animX, animY)
                        .size(cellWidth * w, cellHeight * h)
                        .graphicsLayer {
                            // 套用彈性位移
                            translationX = elasticOffset
                        }
                        .onGloballyPositioned {
                            val pos = it.positionInRoot()
                            lastPosition.pos = pos
                            onSlotPositioned(index, Rect(pos, Size(it.size.width.toFloat(), it.size.height.toFloat())))
                        }
                        .pointerInput(app.uniqueId, isEditMode) {
                            if (isEditMode) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart(app, lastPosition.pos + it) },
                                    onDrag = { _, delta -> onDrag(delta) },
                                    onDragCancel = onDragEnd,
                                    onDragEnd = onDragEnd
                                )
                            } else {
                                detectTapGestures(
                                    onLongPress = {
                                        if (app.widget?.type is WidgetType.Stack) {
                                            stackToEdit = app.widget
                                        } else {
                                            showContextMenu = true
                                        }
                                    },
                                    onTap = { if (!app.isWidget) onAppClick(app, lastPosition.pos) }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (app.isWidget) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .graphicsLayer { alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f }
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                val widget = app.widget
                                if (widget != null) {
                                    when (widget.type) {
                                        is WidgetType.Battery -> BatteryWidget(displayMode = widget.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                                        is WidgetType.Clock -> AnalogClockWidget(displayMode = widget.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                                        is WidgetType.Calendar -> CalendarWidget(widget = widget, displayMode = widget.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                                        is WidgetType.Photo -> PhotoWidget(widget = widget, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                        is WidgetType.Music -> MusicWidget(widget = widget, displayMode = widget.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                                        is WidgetType.Stack -> StackWidget(widget = widget, viewModel = viewModel, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                                        else -> {}
                                    }
                                }
                            }

                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.widget_glass_mode)) }, leadingIcon = { Icon(Icons.Default.BlurOn, null) }, onClick = { app.widget?.let { viewModel.updateWidgetDisplayMode(it.id, WidgetDisplayMode.GLASS) }; showContextMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.widget_color_mode)) }, leadingIcon = { Icon(Icons.Default.Palette, null) }, onClick = { app.widget?.let { viewModel.updateWidgetDisplayMode(it.id, WidgetDisplayMode.COLOR) }; showContextMenu = false })
                                if (app.widget?.type is WidgetType.Stack) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_choose_widgets)) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                                        onClick = {
                                            stackToEdit = app.widget
                                            showContextMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete_home)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { viewModel.removeAppFromHome(app.uniqueId); showContextMenu = false })
                            }
                        }
                    } else {
                        val mContext = LocalContext.current
                        Box {
                            AppItem(
                                app = app,
                                iconSize = iconSize,
                                isLiquidGlass = isLiquidGlass,
                                backdrop = backdrop,
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f
                                    scaleX = scale; scaleY = scale
                                    if (isEditMode && app.uniqueId != draggingUniqueId) rotationZ = rotation
                                }
                            )
                            DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete_home)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { viewModel.removeAppFromHome(app.uniqueId); showContextMenu = false })
                                if (!app.isFolder) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_uninstall)) },
                                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                        onClick = {
                                            android.util.Log.d("Iteration", "Uninstalling: ${app.packageName}")
                                            android.widget.Toast.makeText(mContext, "Uninstalling ${app.label}...", android.widget.Toast.LENGTH_SHORT).show()
                                            try {
                                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                                    data = Uri.fromParts("package", app.packageName, null)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                mContext.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.util.Log.e("Iteration", "Uninstall failed", e)
                                            }
                                            showContextMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (stackToEdit != null) {
        WidgetStackPickerDialog(
            currentChildren = (stackToEdit!!.type as WidgetType.Stack).children,
            viewModel = viewModel,
            onDismiss = { stackToEdit = null },
            onConfirm = { newChildren ->
                viewModel.updateStackChildren(stackToEdit!!.id, newChildren)
            }
        )
    }
}

@Composable
fun AppItem(
    app: AppModel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 62.dp,
    isLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    onAppClick: (() -> Unit)? = null
) {
    val notificationCounts by NotificationService.notifications.collectAsState()

    // 計算通知數量：若是資料夾，則加總內部所有 App 的數量
    val count = if (app.isFolder) {
        app.folderItems.sumOf { notificationCounts[it.packageName] ?: 0 }
    } else {
        notificationCounts[app.packageName] ?: 0
    }

    Column(modifier = modifier.padding(vertical = if (showLabel) 4.dp else 0.dp).then(if (onAppClick != null) Modifier.clickable { onAppClick() } else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (app.packageName.isEmpty() && !app.isFolder) {
                // Dock 空位顯示添加圖示
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(iconSize * 0.238f))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
            } else if (app.isFolder) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .liquidGlass(
                            enabled = isLiquidGlass,
                            backdrop = backdrop,
                            cornerRadius = iconSize * 0.238f
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(0), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(1), iconSize / 2.5f) }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(2), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(3), iconSize / 2.5f) }
                    }
                }
            } else if (app.processedIcon != null) {
                Image(
                    bitmap = app.processedIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(iconSize * 0.238f))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(iconSize * 0.238f)
                        ),
                    contentScale = ContentScale.FillBounds
                )
            }
            if (count > 0) {
                Box(modifier = Modifier.offset(x = 4.dp, y = (-4).dp).size(20.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                    Text(text = if (count > 99) "99+" else count.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontSize = if (count > 9) 9.sp else 11.sp)
                }
            }
        }
        if (showLabel) Text(text = app.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp), color = Color.White)
    }
}

@Composable
fun FolderPreviewIcon(app: AppModel?, size: androidx.compose.ui.unit.Dp) {
    if (app?.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.238f)).background(Color.White))
    else Spacer(modifier = Modifier.size(size))
}

@Composable
fun MultiAppPickerDialog(allApps: List<AppModel>, onDismiss: () -> Unit, onAppsSelected: (List<String>) -> Unit) {
    val visibleApps = remember(allApps) { allApps.filter { !it.isHidden } }
    val selectedPackages = remember { mutableStateListOf<String>() }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.select_apps), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAppsSelected(selectedPackages.toList()) }) { Text(stringResource(R.string.done)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(visibleApps, key = { it.uniqueId }) { app ->
                        ListItem(headlineContent = { Text(app.label) }, leadingContent = { val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap(); if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.4.dp)).background(Color.White)) }, trailingContent = { Checkbox(checked = selectedPackages.contains(app.packageName), onCheckedChange = { if (it) selectedPackages.add(app.packageName) else selectedPackages.remove(app.packageName) }) }, modifier = Modifier.clickable { if (selectedPackages.contains(app.packageName)) selectedPackages.remove(app.packageName) else selectedPackages.add(app.packageName) })
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Dock(
    apps: List<AppModel>,
    iconSize: androidx.compose.ui.unit.Dp,
    isLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop,
    onAppClick: (String) -> Unit,
    onLongClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(105.dp)
            // 關鍵：將所有背景、模糊、折射邏輯統一交給 liquidGlassDock
            .liquidGlassDock(
                isLiquidGlass = isLiquidGlass,
                backdrop = backdrop,
                cornerRadius = 42.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // 內容層：不受任何模糊影響
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEachIndexed { index, app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppItem(
                        app = app,
                        isLiquidGlass = isLiquidGlass,
                        backdrop = backdrop,
                        modifier = Modifier.combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { onLongClick(index) }
                        ),
                        showLabel = false,
                        iconSize = iconSize
                    )
                }
            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
        repeat(pageCount) { index ->
            val color = if (currentPage == index) Color.White else Color.White.copy(alpha = 0.5f)
            Box(modifier = Modifier.padding(4.dp).size(8.dp).background(color, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
fun QuickEditDialog(app: AppModel, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var labelText by remember { mutableStateOf(app.label) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${app.label}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (app.processedIcon != null) {
                        Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Label") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setCustomLabel(app.packageName, labelText)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.setCustomLabel(app.packageName, "")
                viewModel.resetCustomIcon(app.packageName)
                onDismiss()
            }) { Text("Reset") }
        }
    )

    if (pickedImageUri != null) {
        IconCropperDialog(
            uri = pickedImageUri!!,
            onDismiss = { pickedImageUri = null },
            onConfirm = { croppedBitmap ->
                viewModel.setCustomIcon(app.packageName, croppedBitmap)
                pickedImageUri = null
            }
        )
    }
}

@Composable
fun AppPickerDialog(allApps: List<AppModel>, onDismiss: () -> Unit, onAppSelected: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_app), style = MaterialTheme.typography.headlineSmall); Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.uniqueId }) { app ->
                        ListItem(headlineContent = { Text(app.label) }, leadingContent = { val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap(); if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.7.dp)).background(Color.White)) }, modifier = Modifier.clickable { onAppSelected(app.packageName) })
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppLibraryPage(
    allApps: List<AppModel>,
    isLiquidGlass: Boolean = false,
    isSearchLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    onAppClick: (String) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredApps by viewModel.filteredLibraryApps.collectAsState()
    
    var isSearchFocused by remember { mutableStateOf(false) }
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val mContext = LocalContext.current

    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

    // 用於重新命名的狀態
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var newLabelText by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null) {
        if (selectedCategory != null) viewModel.setSelectedCategory("All")
        else { viewModel.setSearchQuery(""); focusManager.clearFocus(); isSearchFocused = false }
    }

    // 分類目錄邏輯：僅用於顯示「資料夾」按鈕，不涉及過濾
    val normalApps = remember(allApps) { allApps.filter { !it.isHidden } }
    val hiddenApps = remember(allApps) { allApps.filter { it.isHidden } }
    val categories = remember(normalApps, userCategories) {
        val grouped = normalApps.groupBy { it.displayCategory }
        val result = mutableListOf<Pair<String, List<AppModel>>>()
        userCategories.forEach { name -> grouped[name]?.let { result.add(name to it) } }
        val handledNames = userCategories.toSet()
        grouped.forEach { (name, apps) -> if (!handledNames.contains(name)) result.add(name to apps) }
        result
    }
    
    val hiddenAppsCount = remember(allApps) { allApps.count { it.isHidden } }
    val showHiddenFolder = hiddenAppsCount > 0 && searchQuery.isBlank() && (selectedCategory == null || selectedCategory == "All")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 搜尋欄：即使在分類中也顯示，但標題不同
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectedCategory != null && selectedCategory != "All") {
                IconButton(onClick = { viewModel.setSelectedCategory("All") }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .liquidGlass(
                        enabled = isSearchLiquidGlass,
                        backdrop = backdrop,
                        cornerRadius = 28.dp
                    ),
                placeholder = {
                    Text(
                        if (selectedCategory != null && selectedCategory != "All") "Search in $selectedCategory" else stringResource(R.string.library_hint),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        }
                        if (isHiddenUnlocked && (selectedCategory == null || selectedCategory == "All")) {
                            IconButton(onClick = { isHiddenUnlocked = false }) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock_hidden_apps), tint = Color.White)
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            if (isSearchFocused || searchQuery.isNotEmpty()) {
                TextButton(onClick = { viewModel.setSearchQuery(""); focusManager.clearFocus(); isSearchFocused = false }) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        }

        val isLibraryView = isSearchFocused || searchQuery.isNotEmpty() || (selectedCategory != null && selectedCategory != "All")

        AnimatedContent(
            targetState = isLibraryView,
            transitionSpec = {
                if (targetState) {
                    // 進入詳細列表：從右側滑入並淡入
                    (slideInHorizontally { it } + fadeIn()).togetherWith(fadeOut())
                } else {
                    // 返回分頁網格：向右側滑出並淡出
                    fadeIn().togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "LibraryTransition"
        ) { targetIsLibraryView ->
            if (targetIsLibraryView) {
                // 安全檢查：如果處於隱藏分類但未解鎖，顯示空列表
                val appsToShow = if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) emptyList() else filteredApps

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(appsToShow, key = { it.uniqueId }) { app ->
                        val lastPos = remember { object { var pos = Offset.Zero } }
                        var showMenu by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { lastPos.pos = it.positionInRoot() }) {
                            ListItem(
                                headlineContent = { Text(app.label, color = Color.White) },
                                leadingContent = {
                                    if (app.processedIcon != null) {
                                        Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.7.dp)).background(Color.White))
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.combinedClickable(
                                    onClick = { onAppClick(app.packageName) },
                                    onLongClick = { showMenu = true }
                                )
                            )

                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_add_to_home)) },
                                    leadingIcon = { Icon(Icons.Default.Add, null) },
                                    onClick = { viewModel.addAppToHome(app.packageName); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rename)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { appToRename = app; newLabelText = app.label; showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (app.isHidden) "Unhide" else "Hide") },
                                    leadingIcon = { Icon(if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) },
                                    onClick = { viewModel.toggleHiddenApp(app.packageName); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("App Info") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${app.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        mContext.startActivity(intent)
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_uninstall)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = {
                                        android.util.Log.d("Iteration", "Uninstalling: ${app.packageName}")
                                        android.widget.Toast.makeText(mContext, "Uninstalling ${app.label}...", android.widget.Toast.LENGTH_SHORT).show()
                                        try {
                                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                                data = Uri.fromParts("package", app.packageName, null)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            mContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e("Iteration", "Uninstall failed", e)
                                        }
                                        showMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 64.dp).align(Alignment.BottomCenter), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
                        }
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                    val folderList = mutableListOf<Pair<String, List<AppModel>>>()
                    if (suggestedApps.isNotEmpty()) folderList.add("Suggestions" to suggestedApps)
                    folderList.addAll(categories)
                    if (showHiddenFolder) folderList.add("Hidden Apps" to hiddenApps)
                    items(folderList) { (name, apps) ->
                        AppLibraryFolder(
                            name = name,
                            apps = apps,
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            isLocked = name == "Hidden Apps" && !isHiddenUnlocked,
                            onAppClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else onAppClick(it) },
                            onMoreClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else viewModel.setSelectedCategory(name) },
                            onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showPasswordDialog = false }, title = { Text(stringResource(R.string.hidden_apps_title)) }, text = { OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text(stringResource(R.string.password_hint)) }, singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff; IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, contentDescription = null) } }) }, confirmButton = { Button(onClick = { if (passwordInput == viewModel.getPassword()) { isHiddenUnlocked = true; showPasswordDialog = false } }) { Text(stringResource(R.string.unlock)) } })
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppLibraryFolder(
    name: String,
    apps: List<AppModel>,
    isLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    onAppClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    val mContext = LocalContext.current

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .liquidGlass(
                    enabled = isLiquidGlass,
                    backdrop = backdrop,
                    cornerRadius = 24.dp
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(0)?.let { app ->
                            LibraryItemWithMenu(app, name, onAppClick, onDragStart, onDrag, onDragEnd)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(1)?.let { app ->
                            LibraryItemWithMenu(app, name, onAppClick, onDragStart, onDrag, onDragEnd)
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(2)?.let { app ->
                            LibraryItemWithMenu(app, name, onAppClick, onDragStart, onDrag, onDragEnd)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else if (apps.size > 4) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() }, contentAlignment = Alignment.Center) { Text("+${apps.size - 3}", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        else apps.getOrNull(3)?.let { app ->
                            LibraryItemWithMenu(app, name, onAppClick, onDragStart, onDrag, onDragEnd)
                        }
                    }
                }
            }
        }
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryItemWithMenu(
    app: AppModel,
    folderName: String,
    onAppClick: (String) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val mContext = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val lastPos = remember { object { var pos = Offset.Zero } }

    // 用於重新命名的狀態（這裡稍微簡化，實際可能需要傳回 AppLibraryPage 處理更優雅，但我們先做基本功能）
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(app.label) }

    Box(modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }) {
        AppItem(
            app,
            showLabel = false,
            iconSize = 72.dp,
            modifier = Modifier.combinedClickable(
                onClick = { onAppClick(app.packageName) },
                onLongClick = { if (folderName != "Hidden Apps") showMenu = true }
            )
        )

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_add_to_home)) },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                onClick = { viewModel.addAppToHome(app.packageName); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { showRenameDialog = true; showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Hide") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { viewModel.toggleHiddenApp(app.packageName); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("App Info") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    mContext.startActivity(intent)
                    showMenu = false
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_uninstall)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = {
                    android.util.Log.d("Iteration", "Uninstalling: ${app.packageName}")
                    android.widget.Toast.makeText(mContext, "Uninstalling ${app.label}...", android.widget.Toast.LENGTH_SHORT).show()
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        mContext.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("Iteration", "Uninstall failed", e)
                    }
                    showMenu = false
                }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename App") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCustomLabel(app.packageName, renameText)
                    showRenameDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}


