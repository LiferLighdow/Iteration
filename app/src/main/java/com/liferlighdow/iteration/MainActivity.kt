package com.liferlighdow.iteration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    LauncherScreen(
                        onAppClick = { packageName ->
                            // 如果點擊的是 Launcher 自己，跳轉到設定
                            if (packageName == this@MainActivity.packageName) {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            } else {
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) startActivity(intent)
                            }
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

@Composable
fun LauncherScreen(
    viewModel: MainViewModel = viewModel(),
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val allAppsFlat by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    
    var shouldRefreshOnResume by remember { mutableStateOf(false) }

    // 當 Activity 恢復時（例如從設定返回），重新加載資料
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (shouldRefreshOnResume) {
                    viewModel.loadApps()
                    shouldRefreshOnResume = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 初次載入
    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }

    val context = LocalContext.current
    val myPackageName = context.packageName

    // 包裝點擊事件，設定刷新標記
    val handleAppClick: (String) -> Unit = { pkg ->
        if (pkg == myPackageName) {
            shouldRefreshOnResume = true
        }
        onAppClick(pkg)
    }
    
    val handleSettingsClick: () -> Unit = {
        shouldRefreshOnResume = true
        onSettingsClick()
    }

    // 拖拽狀態
    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    
    val itemBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }

    var showDockPicker by remember { mutableStateOf<Int?>(null) }
    val dockApps = dockPkgNames.mapNotNull { pkg -> allAppsFlat.find { it.packageName == pkg } }

    // 全域搜尋狀態
    var showGlobalSearch by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }

    if (showGlobalSearch) {
        BackHandler {
            showGlobalSearch = false
            globalSearchQuery = ""
        }
    }

    // 關鍵：最後一頁是 App Library，拖拽時增加一個分頁空間
    val actualPageCount = pages.size.coerceAtLeast(1)
    val pageCount = if (draggingApp != null) actualPageCount + 2 else actualPageCount + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    
    // 判斷目前是否在 App Library 頁面 (最後一頁)
    val isAppLibraryPage = pagerState.currentPage == pageCount - 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(draggingApp, showGlobalSearch, isAppLibraryPage) {
                if (draggingApp == null && !showGlobalSearch) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // 偵測下滑動作且不在 App Library 頁面
                        if (dragAmount > 50 && !isAppLibraryPage) {
                            showGlobalSearch = true
                        }
                    }
                }
            }
    ) {
        val aspectRatio = maxHeight / maxWidth
        
        val columns = 4
        val rows = if (aspectRatio < 2.0f) 5 else 6
        val iconSize = 62.dp

        val pageSize = columns * rows
        // 通知 ViewModel 目前每頁的大小
        LaunchedEffect(pageSize) {
            viewModel.setPageSize(pageSize)
        }

        val density = androidx.compose.ui.platform.LocalDensity.current

        // 自動翻頁偵測邏輯... (保持不變)
        LaunchedEffect(draggingApp) {
            if (draggingApp == null) return@LaunchedEffect
            while (true) {
                val finalX = touchPosition.x + dragOffset.x
                val screenWidthPx = with(density) { maxWidth.toPx() }
                val edgeWidth = with(density) { 40.dp.toPx() }
                if (finalX < edgeWidth && pagerState.currentPage > 0) {
                    delay(800)
                    if (touchPosition.x + dragOffset.x < edgeWidth) {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        delay(500)
                    }
                } else if (finalX > screenWidthPx - edgeWidth && pagerState.currentPage < pageCount - 1) {
                    delay(800)
                    if (touchPosition.x + dragOffset.x > screenWidthPx - edgeWidth) {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        delay(500)
                    }
                }
                delay(100)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 1,
                userScrollEnabled = draggingApp == null
            ) { pageIndex ->
                // 為一般頁面留出 Dock 的空間，App Library 則佔滿全螢幕
                val isLibrary = pageIndex >= pages.size && draggingApp == null
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = if (isLibrary) 0.dp else 140.dp)
                ) {
                    when {
                        pageIndex < pages.size -> {
                            val pageItems = pages.getOrNull(pageIndex) ?: emptyList()
                            AppGrid(
                                apps = pageItems,
                                columns = columns,
                                rows = rows,
                                iconSize = iconSize,
                                draggingUniqueId = draggingApp?.uniqueId,
                                onAppClick = handleAppClick,
                                onPositioned = { id, rect -> itemBounds[id] = rect },
                                onDragStart = { app, offset -> 
                                    draggingApp = app
                                    touchPosition = offset
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    val finalPos = touchPosition + dragOffset
                                    val targetId = itemBounds.entries.find { it.value.contains(finalPos) }?.key
                                    if (draggingApp != null) {
                                        if (targetId != null) {
                                            viewModel.moveApp(draggingApp!!.uniqueId, targetId = targetId)
                                        } else {
                                            viewModel.moveApp(draggingApp!!.uniqueId, targetPageIndex = pagerState.currentPage)
                                        }
                                    }
                                    draggingApp = null
                                    dragOffset = Offset.Zero
                                }
                            )
                        }
                        pageIndex == pages.size && draggingApp != null -> {
                            AppGrid(
                                apps = emptyList(),
                                columns = columns,
                                rows = rows,
                                iconSize = iconSize,
                                draggingUniqueId = draggingApp?.uniqueId,
                                onAppClick = { },
                                onPositioned = { id, rect -> itemBounds[id] = rect },
                                onDragStart = { _, _ -> },
                                onDrag = { _ -> },
                                onDragEnd = { }
                            )
                        }
                        else -> {
                            // App Library 頁面
                            AppLibraryPage(
                                allApps = allAppsFlat, 
                                onAppClick = handleAppClick,
                                onDragStart = { app, offset ->
                                    draggingApp = app
                                    touchPosition = offset
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    val finalPos = touchPosition + dragOffset
                                    val targetId = itemBounds.entries.find { it.value.contains(finalPos) }?.key
                                    if (draggingApp != null) {
                                        if (targetId != null) {
                                            viewModel.moveApp(draggingApp!!.packageName, targetId = targetId, isFromLibrary = true)
                                        } else {
                                            viewModel.moveApp(draggingApp!!.packageName, targetPageIndex = pagerState.currentPage, isFromLibrary = true)
                                        }
                                    }
                                    draggingApp = null
                                    dragOffset = Offset.Zero
                                }
                            )
                        }
                    }
                }
            }

            // Dock 與指示器浮動在最上層，避免切換時觸發 Pager 重新佈局
            Box(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // 如果在 App Library 頁面，按下返回鍵跳回第一頁
                BackHandler(enabled = isAppLibraryPage && !showGlobalSearch) {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
                
                // 阻止主畫面按下返回鍵導致 Activity 結束或重新載入
                BackHandler(enabled = pagerState.currentPage == 0 && !showGlobalSearch) {
                    // 什麼都不做，防止返回鍵干擾 Launcher
                }

                AnimatedVisibility(
                    visible = !isAppLibraryPage,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PageIndicator(pageCount = pageCount - 1, currentPage = pagerState.currentPage)

                        Dock(
                            apps = dockApps,
                            iconSize = iconSize,
                            onAppClick = { pkg ->
                                if (pkg == myPackageName) handleSettingsClick() else handleAppClick(pkg)
                            },
                            onLongClick = { slotIndex ->
                                showDockPicker = slotIndex
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // 渲染拖拽中的「幻影圖示」
        draggingApp?.let { app ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            val iconSizePx = with(density) { iconSize.toPx() }
            
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            (touchPosition.x + dragOffset.x - iconSizePx / 2).roundToInt(),
                            (touchPosition.y + dragOffset.y - iconSizePx / 2).roundToInt()
                        ) 
                    }
                    .size(iconSize)
                    .graphicsLayer(alpha = 0.7f, scaleX = 1.2f, scaleY = 1.2f)
            ) {
                AppItem(app = app, iconSize = iconSize)
            }
        }
    }

    // Dock 選擇彈窗
    showDockPicker?.let { slotIndex ->
        AppPickerDialog(
            allApps = allAppsFlat,
            onDismiss = { showDockPicker = null },
            onAppSelected = { pkg ->
                viewModel.updateDockApp(slotIndex, pkg)
                showDockPicker = null
            }
        )
    }

    // 全域搜尋遮罩層
    AnimatedVisibility(
        visible = showGlobalSearch,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { 
                    showGlobalSearch = false
                    globalSearchQuery = ""
                }
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .clickable(enabled = false) { } // 阻止點擊列表區域關閉
            ) {
                // 搜尋框
                OutlinedTextField(
                    value = globalSearchQuery,
                    onValueChange = { globalSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    placeholder = { Text("Search Apps...", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // 搜尋結果
                val filteredResults = remember(globalSearchQuery, allAppsFlat) {
                    if (globalSearchQuery.isBlank()) {
                        // 當沒輸入文字時，顯示前 8 個作為 Suggestion
                        allAppsFlat.take(8)
                    } else {
                        allAppsFlat.filter { it.label.contains(globalSearchQuery, ignoreCase = true) }
                    }
                }

                val context = LocalContext.current

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (globalSearchQuery.isBlank() && filteredResults.isNotEmpty()) {
                        item {
                            Text(
                                "App Suggestions",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    items(filteredResults) { app ->
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) },
                            leadingContent = {
                                if (app.processedIcon != null) {
                                    val radius = 48.dp * 0.22f
                                    Image(
                                        bitmap = app.processedIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(radius)).background(Color.White)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { 
                                handleAppClick(app.packageName)
                                showGlobalSearch = false
                                globalSearchQuery = ""
                            }
                        )
                    }

                    if (globalSearchQuery.isNotBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Search in Web
                        item {
                            SearchActionItem("Search in Web", androidx.compose.material.icons.Icons.Default.Search) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${globalSearchQuery}"))
                                context.startActivity(intent)
                                showGlobalSearch = false
                                globalSearchQuery = ""
                            }
                        }
                        // Search in Store
                        item {
                            SearchActionItem("Search in Store", androidx.compose.material.icons.Icons.Default.Search) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${globalSearchQuery}"))
                                context.startActivity(intent)
                                showGlobalSearch = false
                                globalSearchQuery = ""
                            }
                        }
                        // Search in Maps
                        item {
                            SearchActionItem("Search in Maps", androidx.compose.material.icons.Icons.Default.Search) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${globalSearchQuery}"))
                                context.startActivity(intent)
                                showGlobalSearch = false
                                globalSearchQuery = ""
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White) },
        leadingContent = { 
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun AppGrid(
    apps: List<AppModel>,
    columns: Int,
    rows: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    draggingUniqueId: String?,
    onAppClick: (String) -> Unit,
    onPositioned: (String, androidx.compose.ui.geometry.Rect) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(columns) { colIndex ->
                    val index = rowIndex * columns + colIndex
                    // 使用 remember 建立一個持久的對象來儲存座標，而不觸發重繪
                    val lastPosition = remember { object { var pos = Offset.Zero } }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { coordinates ->
                                if (index < apps.size) {
                                    val pos = coordinates.positionInRoot()
                                    lastPosition.pos = pos
                                    val rect = androidx.compose.ui.geometry.Rect(
                                        pos,
                                        androidx.compose.ui.geometry.Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                                    )
                                    onPositioned(apps[index].uniqueId, rect)
                                }
                            }, 
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < apps.size) {
                            val app = apps[index]
                            
                            AppItem(
                                app = app,
                                modifier = Modifier
                                    .graphicsLayer {
                                        // 優化：使用 Lambda 形式的 graphicsLayer，避免 alpha 變化導致重組
                                        alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f
                                    }
                                    .pointerInput(app.uniqueId) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                onDragStart(app, lastPosition.pos + offset)
                                            },
                                            onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                            onDragCancel = { onDragEnd() },
                                            onDragEnd = { onDragEnd() }
                                        )
                                    }
                                    .clickable { onAppClick(app.packageName) },
                                iconSize = iconSize
                            )
                        } else {
                            Spacer(modifier = Modifier.height(iconSize + 28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppModel, 
    modifier: Modifier = Modifier,
    showLabel: Boolean = true, 
    iconSize: androidx.compose.ui.unit.Dp = 62.dp
) {
    Column(
        modifier = modifier
            .padding(vertical = if (showLabel) 4.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 直接使用預處理好的 ImageBitmap，效率極高
        if (app.processedIcon != null) {
            val radius = iconSize * 0.22f
            Image(
                bitmap = app.processedIcon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(radius))
                    .background(Color.White),
                contentScale = ContentScale.FillBounds
            )
        }
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                color = Color.White
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Dock(
    apps: List<AppModel>, 
    iconSize: androidx.compose.ui.unit.Dp,
    onAppClick: (String) -> Unit,
    onLongClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(105.dp)
            .background(color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(42.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEachIndexed { index, app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppItem(
                        app = app, 
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val color = if (currentPage == index) Color.White else Color.White.copy(alpha = 0.5f)
            Box(
                modifier = Modifier.padding(4.dp).size(8.dp).background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun AppPickerDialog(
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select App", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                Image(
                                    bitmap = app.icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                )
                            },
                            modifier = Modifier.clickable { onAppSelected(app.packageName) }
                        )
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
    onAppClick: (String) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // 隱藏 App 的狀態
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = viewModel()

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps else {
            allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val hiddenApps = remember(filteredApps) { filteredApps.filter { it.isHidden } }
    val normalApps = remember(filteredApps) { filteredApps.filter { !it.isHidden } }

    val categories = remember(normalApps) {
        normalApps.groupBy { app ->
            when (app.category) {
                0 -> "Games"
                1 -> "Audio"
                2 -> "Video"
                3 -> "Imaging"
                4 -> "Social"
                5 -> "News"
                6 -> "Maps"
                7 -> "Productivity"
                else -> "Other"
            }
        }.toList().sortedBy { it.first }.toMutableList()
    }

    // 如果有隱藏的 App，且不在搜尋模式下，則加入 "Hidden Apps" 分類
    val showHiddenFolder = hiddenApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (selectedCategory == null) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                placeholder = { Text("App Library", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
        } else {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = selectedCategory!!,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        if (isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null) {
            // 搜尋或分類模式：條列式
            val appsToShow = remember(filteredApps, selectedCategory, isHiddenUnlocked) {
                val baseList = if (selectedCategory != null) {
                    if (selectedCategory == "Hidden Apps") hiddenApps
                    else categories.find { it.first == selectedCategory }?.second ?: emptyList()
                } else {
                    filteredApps
                }
                
                // 如果是 Hidden Apps 分類且未解鎖，則不顯示（或在條列式中過濾掉）
                if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) {
                    emptyList()
                } else {
                    // 條列式平常不顯示 hidden apps，除非是進入了 Hidden Apps 分區
                    if (selectedCategory == "Hidden Apps") baseList.sortedBy { it.label.lowercase() }
                    else baseList.filter { !it.isHidden }.sortedBy { it.label.lowercase() }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(appsToShow) { app ->
                    val lastPosition = remember { object { var pos = Offset.Zero } }
                    Column(
                        modifier = Modifier.onGloballyPositioned { lastPosition.pos = it.positionInRoot() }
                    ) {
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) },
                            leadingContent = {
                                if (app.processedIcon != null) {
                                    val radius = 40.dp * 0.22f
                                    Image(
                                        bitmap = app.processedIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(radius)).background(Color.White)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { onAppClick(app.packageName) },
                                    onLongClick = { /* Optional */ }
                                )
                                .pointerInput(app.packageName) {
                                    if (!app.isHidden) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset -> onDragStart(app, lastPosition.pos + offset) },
                                            onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                            onDragCancel = { onDragEnd() },
                                            onDragEnd = { onDragEnd() }
                                        )
                                    }
                                }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        } else {
            // 類別模式
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                val folderList = categories.toMutableList()
                if (showHiddenFolder) {
                    folderList.add("Hidden Apps" to hiddenApps)
                }

                val chunks = folderList.chunked(2)
                items(chunks) { chunk ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        chunk.forEach { (name, apps) ->
                            AppLibraryFolder(
                                name = name, 
                                apps = apps, 
                                isLocked = name == "Hidden Apps" && !isHiddenUnlocked,
                                onAppClick = { pkg ->
                                    if (name == "Hidden Apps" && !isHiddenUnlocked) {
                                        showPasswordDialog = true
                                    } else {
                                        onAppClick(pkg)
                                    }
                                }, 
                                onMoreClick = { 
                                    if (name == "Hidden Apps" && !isHiddenUnlocked) {
                                        showPasswordDialog = true
                                    } else {
                                        selectedCategory = name 
                                    }
                                },
                                onDragStart = onDragStart,
                                onDrag = onDrag,
                                onDragEnd = onDragEnd,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Hidden Apps") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordInput == viewModel.getPassword()) {
                        isHiddenUnlocked = true
                        showPasswordDialog = false
                    }
                }) {
                    Text("Unlock")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppLibraryFolder(
    name: String,
    apps: List<AppModel>,
    onAppClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            // 2x2 預覽圖示
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        } else {
                            apps.getOrNull(0)?.let { app ->
                                val lastPos = remember { object { var pos = Offset.Zero } }
                                AppItem(
                                    app, 
                                    showLabel = false, 
                                    iconSize = 72.dp, 
                                    modifier = Modifier
                                        .onGloballyPositioned { lastPos.pos = it.positionInRoot() }
                                        .combinedClickable(
                                            onClick = { onAppClick(app.packageName) },
                                            onLongClick = { }
                                        )
                                        .pointerInput(app.packageName) {
                                            if (name != "Hidden Apps") {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset -> onDragStart(app, lastPos.pos + offset) },
                                                    onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                                    onDragCancel = { onDragEnd() },
                                                    onDragEnd = { onDragEnd() }
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        } else {
                            apps.getOrNull(1)?.let { app ->
                                val lastPos = remember { object { var pos = Offset.Zero } }
                                AppItem(
                                    app, 
                                    showLabel = false, 
                                    iconSize = 72.dp, 
                                    modifier = Modifier
                                        .onGloballyPositioned { lastPos.pos = it.positionInRoot() }
                                        .combinedClickable(
                                            onClick = { onAppClick(app.packageName) },
                                            onLongClick = { }
                                        )
                                        .pointerInput(app.packageName) {
                                            if (name != "Hidden Apps") {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset -> onDragStart(app, lastPos.pos + offset) },
                                                    onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                                    onDragCancel = { onDragEnd() },
                                                    onDragEnd = { onDragEnd() }
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        } else {
                            apps.getOrNull(2)?.let { app ->
                                val lastPos = remember { object { var pos = Offset.Zero } }
                                AppItem(
                                    app, 
                                    showLabel = false, 
                                    iconSize = 72.dp, 
                                    modifier = Modifier
                                        .onGloballyPositioned { lastPos.pos = it.positionInRoot() }
                                        .combinedClickable(
                                            onClick = { onAppClick(app.packageName) },
                                            onLongClick = { }
                                        )
                                        .pointerInput(app.packageName) {
                                            if (name != "Hidden Apps") {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset -> onDragStart(app, lastPos.pos + offset) },
                                                    onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                                    onDragCancel = { onDragEnd() },
                                                    onDragEnd = { onDragEnd() }
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        } else {
                            if (apps.size > 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                        .clickable { onMoreClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${apps.size - 3}", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                                }
                            } else {
                                apps.getOrNull(3)?.let { app ->
                                    val lastPos = remember { object { var pos = Offset.Zero } }
                                    AppItem(
                                        app, 
                                        showLabel = false, 
                                        iconSize = 72.dp, 
                                        modifier = Modifier
                                            .onGloballyPositioned { lastPos.pos = it.positionInRoot() }
                                            .combinedClickable(
                                                onClick = { onAppClick(app.packageName) },
                                                onLongClick = { }
                                            )
                                            .pointerInput(app.packageName) {
                                                if (name != "Hidden Apps") {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset -> onDragStart(app, lastPos.pos + offset) },
                                                        onDrag = { _, dragAmount -> onDrag(dragAmount) },
                                                        onDragCancel = { onDragEnd() },
                                                        onDragEnd = { onDragEnd() }
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
