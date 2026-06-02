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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlin.math.max
import kotlin.math.min
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
                        onAppClick = { pkg ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: MainViewModel = viewModel(),
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val allAppsFlat by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    var shouldRefreshOnResume by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && shouldRefreshOnResume) {
                viewModel.loadApps()
                shouldRefreshOnResume = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val context = LocalContext.current
    val myPackageName = context.packageName

    // 拖拽核心狀態
    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var folderToOpen by remember { mutableStateOf<AppModel?>(null) }
    
    // 選單與對話框狀態
    var showDesktopMenu by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    
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
    val dockApps = dockPkgNames.mapNotNull { pkg -> allAppsFlat.find { it.packageName == pkg } }

    var showGlobalSearch by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }
    if (showGlobalSearch) BackHandler { showGlobalSearch = false; globalSearchQuery = "" }
    
    if (isEditMode) BackHandler { viewModel.setEditMode(false) }

    val pageCount = (pages.size.coerceAtLeast(1)) + (if (draggingApp != null) 2 else 1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isAppLibraryPage = pagerState.currentPage == pageCount - 1

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
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

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState, modifier = Modifier.fillMaxSize(),
                userScrollEnabled = draggingApp == null && !isEditMode, beyondViewportPageCount = 1
            ) { pageIndex ->
                val isLibrary = pageIndex >= pages.size && draggingApp == null
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = if (isLibrary) 0.dp else 140.dp)
                        .pointerInput(draggingApp, isEditMode, isLibrary) {
                            if (draggingApp == null && !isEditMode && !isLibrary) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 20) showGlobalSearch = true
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { if (!isLibrary) showDesktopMenu = true },
                                onTap = { if (isEditMode) viewModel.setEditMode(false) }
                            )
                        }
                ) {
                    when {
                        pageIndex < pages.size -> {
                            AppGrid(
                                apps = pages.getOrNull(pageIndex) ?: emptyList(),
                                columns = columns, rows = rows, iconSize = iconSize,
                                isEditMode = isEditMode,
                                viewModel = viewModel,
                                draggingUniqueId = draggingApp?.uniqueId,
                                confirmedHoveredSlotIdx = if (confirmedHoveredKey?.startsWith("$pageIndex-") == true) 
                                    confirmedHoveredKey?.substringAfter("-")?.toInt() else null,
                                confirmedIntent = if (isEditMode) MainViewModel.DropType.REORDER else confirmedIntent,
                                onAppClick = { pkg ->
                                    if (isEditMode) return@AppGrid
                                    val app = pages[pageIndex].find { it.packageName == pkg }
                                    if (app?.isFolder == true) folderToOpen = app else onAppClick(pkg)
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
                                    // 只有在非編輯模式下才允許拖拽形成資料夾
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
                                            val targetApp = pages.getOrNull(tPageIdx)?.getOrNull(tSlotIdx)
                                            val dropType = if (!isEditMode && maxOverlap > 0.50f && targetApp != null) MainViewModel.DropType.FOLDER else MainViewModel.DropType.REORDER
                                            viewModel.handleAppDrop(draggingApp!!.uniqueId, targetApp?.uniqueId, tPageIdx, false, dropType)
                                        } else {
                                            viewModel.handleAppDrop(draggingApp!!.uniqueId, null, pageIndex, false, MainViewModel.DropType.REORDER)
                                        }
                                    }
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey = null
                                }
                            )
                        }
                        else -> {
                            AppLibraryPage(
                                allApps = allAppsFlat, 
                                onAppClick = { pkg ->
                                    val app = allAppsFlat.find { it.packageName == pkg }
                                    if (app?.isFolder == true) folderToOpen = app else onAppClick(pkg)
                                },
                                onDragStart = { app, offset -> draggingApp = app; touchPosition = offset; dragOffset = Offset.Zero },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                    if (draggingApp != null) viewModel.handleAppDrop(draggingApp!!.packageName, null, pagerState.currentPage, true, MainViewModel.DropType.REORDER)
                                    draggingApp = null; rawHoveredKey = null; confirmedHoveredKey = null
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isEditMode, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), contentAlignment = Alignment.TopEnd) {
                    Button(onClick = { viewModel.setEditMode(false) }) { Text("Done") }
                }
            }

            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
                BackHandler(enabled = isAppLibraryPage && !showGlobalSearch) { scope.launch { pagerState.animateScrollToPage(0) } }
                AnimatedVisibility(visible = !isAppLibraryPage, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PageIndicator(pageCount = pageCount - 1, currentPage = pagerState.currentPage)
                        Dock(apps = dockApps, iconSize = iconSize, onAppClick = { pkg -> if (pkg == myPackageName) { shouldRefreshOnResume = true; onSettingsClick() } else onAppClick(pkg) }, onLongClick = { showDockPicker = it })
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

    if (showDesktopMenu) {
        ModalBottomSheet(onDismissRequest = { showDesktopMenu = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ListItem(headlineContent = { Text("Enter Edit Mode") }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { viewModel.setEditMode(true); showDesktopMenu = false })
                ListItem(headlineContent = { Text("New Folder") }, leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }, modifier = Modifier.clickable { showCreateFolderDialog = true; showDesktopMenu = false })
                ListItem(headlineContent = { Text("Launcher Settings") }, leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) }, modifier = Modifier.clickable { onSettingsClick(); showDesktopMenu = false })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = { OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Folder Name") }, singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.createFolder(pagerState.currentPage, folderName); showCreateFolderDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") } }
        )
    }

    showDockPicker?.let { slotIndex -> AppPickerDialog(allApps = allAppsFlat, onDismiss = { showDockPicker = null }, onAppSelected = { pkg -> viewModel.updateDockApp(slotIndex, pkg); showDockPicker = null }) }
    
    folderToOpen?.let { folder ->
        FolderDialog(
            folder = folder,
            allApps = allAppsFlat,
            onDismiss = { folderToOpen = null },
            onAppClick = { onAppClick(it); folderToOpen = null },
            onRename = { viewModel.updateFolderName(folder.uniqueId, it) },
            onDeleteFolder = { viewModel.deleteFolder(folder.uniqueId); folderToOpen = null },
            onAddApps = { viewModel.addAppsToFolder(folder.uniqueId, it) },
            onDragStartFromFolder = { app, offset -> draggingApp = app; touchPosition = offset; dragOffset = Offset.Zero; folderToOpen = null }
        )
    }
    
    // 全域搜尋遮罩層保持不變...
    AnimatedVisibility(visible = showGlobalSearch, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut() + slideOutVertically { -it / 2 }) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { showGlobalSearch = false; globalSearchQuery = "" }.statusBarsPadding()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).clickable(enabled = false) { }) {
                OutlinedTextField(
                    value = globalSearchQuery, onValueChange = { globalSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    placeholder = { Text("Search Apps...", color = Color.White.copy(alpha = 0.6f)) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    shape = RoundedCornerShape(28.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.2f), unfocusedContainerColor = Color.White.copy(alpha = 0.2f), focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent),
                    singleLine = true
                )
                val filteredResults = remember(globalSearchQuery, allAppsFlat) { if (globalSearchQuery.isBlank()) allAppsFlat.take(8) else allAppsFlat.filter { it.label.contains(globalSearchQuery, ignoreCase = true) } }
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (globalSearchQuery.isBlank() && filteredResults.isNotEmpty()) {
                        item { Text("App Suggestions", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    items(filteredResults, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) }, leadingContent = { if (app.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onAppClick(app.packageName); showGlobalSearch = false }
                        )
                    }

                    if (globalSearchQuery.isNotBlank()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                            Text("More Searches", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        item {
                            ListItem(
                                headlineContent = { Text("Search on Web", color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${globalSearchQuery}"))
                                    context.startActivity(intent)
                                    showGlobalSearch = false
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text("Search on Store", color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Shop, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${globalSearchQuery}"))
                                    context.startActivity(intent)
                                    showGlobalSearch = false
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text("Search on Maps", color = Color.White) },
                                leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${globalSearchQuery}"))
                                    context.startActivity(intent)
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
    apps: List<AppModel>, columns: Int, rows: Int, iconSize: androidx.compose.ui.unit.Dp, draggingUniqueId: String?,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    confirmedHoveredSlotIdx: Int?, confirmedIntent: MainViewModel.DropType,
    onAppClick: (String) -> Unit, onSlotPositioned: (Int, Rect) -> Unit, onDragStart: (AppModel, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(columns) { colIndex ->
                    val slotIndex = rowIndex * columns + colIndex
                    val lastPosition = remember { object { var pos = Offset.Zero } }
                    Box(modifier = Modifier.weight(1f).onGloballyPositioned {
                        val pos = it.positionInRoot()
                        lastPosition.pos = pos
                        onSlotPositioned(slotIndex, Rect(pos, Size(it.size.width.toFloat(), it.size.height.toFloat())))
                    }, contentAlignment = Alignment.Center) {
                        val visualAppIndex = if (confirmedHoveredSlotIdx != null && confirmedIntent == MainViewModel.DropType.REORDER && slotIndex >= confirmedHoveredSlotIdx) {
                            slotIndex - 1
                        } else { slotIndex }
                        val isHoveredFolder = confirmedHoveredSlotIdx == slotIndex && confirmedIntent == MainViewModel.DropType.FOLDER
                        val scale by animateFloatAsState(if (isHoveredFolder) 1.25f else 1.0f)
                        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = -1.5f, targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(animation = tween(100, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
                        )
                        val app = apps.getOrNull(visualAppIndex)
                        if (app != null) {
                            var showContextMenu by remember { mutableStateOf(false) }
                            val context = LocalContext.current

                            Box {
                                AppItem(
                                    app = app, iconSize = iconSize, modifier = Modifier.graphicsLayer { 
                                        alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f
                                        scaleX = scale; scaleY = scale
                                        if (isEditMode && app.uniqueId != draggingUniqueId) rotationZ = rotation
                                    }
                                    .pointerInput(app.uniqueId, isEditMode) { 
                                        if (isEditMode) {
                                            detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPosition.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd)
                                        } else {
                                            detectTapGestures(
                                                onLongPress = { showContextMenu = true },
                                                onTap = { onAppClick(app.packageName) }
                                            )
                                        }
                                    }
                                )

                                DropdownMenu(
                                    expanded = showContextMenu,
                                    onDismissRequest = { showContextMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete from Home") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            viewModel.removeAppFromHome(app.uniqueId)
                                            showContextMenu = false
                                        }
                                    )
                                    if (!app.isFolder) {
                                        DropdownMenuItem(
                                            text = { Text("Uninstall") },
                                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                                    data = Uri.parse("package:${app.packageName}")
                                                }
                                                context.startActivity(intent)
                                                showContextMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else Spacer(modifier = Modifier.height(iconSize + 28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppModel, modifier: Modifier = Modifier, showLabel: Boolean = true, iconSize: androidx.compose.ui.unit.Dp = 62.dp, onAppClick: (() -> Unit)? = null) {
    Column(modifier = modifier.padding(vertical = if (showLabel) 4.dp else 0.dp).then(if (onAppClick != null) Modifier.clickable { onAppClick() } else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        if (app.isFolder) {
            Box(modifier = Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.22f)).background(Color.White.copy(alpha = 0.3f)).padding(4.dp), contentAlignment = Alignment.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(0), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(1), iconSize / 2.5f) }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(2), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(3), iconSize / 2.5f) }
                }
            }
        } else if (app.processedIcon != null) {
            Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.22f)).background(Color.White), contentScale = ContentScale.FillBounds)
        }
        if (showLabel) Text(text = app.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp), color = Color.White)
    }
}

@Composable
fun FolderPreviewIcon(app: AppModel?, size: androidx.compose.ui.unit.Dp) {
    if (app?.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)).background(Color.White))
    else Spacer(modifier = Modifier.size(size))
}

@Composable
fun FolderDialog(
    folder: AppModel,
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit,
    onRename: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onAddApps: (List<String>) -> Unit,
    onDragStartFromFolder: (AppModel, Offset) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(folder.label) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // 增加遮罩暗度
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Centered Title and Menu
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = tempName, onValueChange = { tempName = it },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            trailingIcon = { 
                                IconButton(onClick = { onRename(tempName); isEditingName = false }) { 
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White) 
                                } 
                            }
                        )
                    } else {
                        Text(
                            text = folder.label, 
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.clickable { isEditingName = true },
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Folder Menu", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { isEditingName = true; showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Add App") }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { showAppPicker = true; showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Delete Folder") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { onDeleteFolder(); showMoreMenu = false })
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                val itemsPerPage = 9
                val pages = remember(folder.folderItems) { folder.folderItems.chunked(itemsPerPage) }
                val pagerState = rememberPagerState { pages.size }

                // Square Container for apps (320x320)
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable(enabled = false) {}
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val pageItems = pages[pageIdx]
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val rows = pageItems.chunked(3)
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowItems.forEach { app ->
                                        val lastPos = remember { object { var pos = Offset.Zero } }
                                        Box(
                                            modifier = Modifier.weight(1f).onGloballyPositioned { lastPos.pos = it.positionInRoot() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppItem(
                                                app = app,
                                                onAppClick = { onAppClick(app.packageName) },
                                                iconSize = 58.dp,
                                                modifier = Modifier.pointerInput(app.uniqueId) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset -> onDragStartFromFolder(app, lastPos.pos + offset) },
                                                        onDrag = { _, _ -> },
                                                        onDragCancel = {},
                                                        onDragEnd = {}
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    // Fill the rest of the Row if it's not full
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            // No need to repeat Spacers for missing rows here as it's a Column with spacedBy
                        }
                    }
                }

                if (pages.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PageIndicator(pageCount = pages.size, currentPage = pagerState.currentPage)
                }
            }
        }
    }

    if (showAppPicker) {
        MultiAppPickerDialog(
            allApps = allApps, 
            onDismiss = { showAppPicker = false }, 
            onAppsSelected = { onAddApps(it); showAppPicker = false }
        )
    }
}

@Composable
fun MultiAppPickerDialog(
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onAppsSelected: (List<String>) -> Unit
) {
    val selectedPackages = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Apps", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAppsSelected(selectedPackages.toList()) }) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                if (app.processedIcon != null) {
                                    Image(
                                        bitmap = app.processedIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                                    )
                                } else {
                                    Image(
                                        bitmap = app.icon!!.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = selectedPackages.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPackages.add(app.packageName)
                                        else selectedPackages.remove(app.packageName)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (selectedPackages.contains(app.packageName)) selectedPackages.remove(app.packageName)
                                else selectedPackages.add(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Dock(apps: List<AppModel>, iconSize: androidx.compose.ui.unit.Dp, onAppClick: (String) -> Unit, onLongClick: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(105.dp).background(color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(42.dp)), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            apps.forEachIndexed { index, app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppItem(app = app, modifier = Modifier.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { onLongClick(index) }), showLabel = false, iconSize = iconSize)
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
fun AppPickerDialog(allApps: List<AppModel>, onDismiss: () -> Unit, onAppSelected: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select App", style = MaterialTheme.typography.headlineSmall); Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.packageName }) { app ->
                        ListItem(headlineContent = { Text(app.label) }, leadingContent = { Image(bitmap = app.icon!!.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))) }, modifier = Modifier.clickable { onAppSelected(app.packageName) })
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppLibraryPage(allApps: List<AppModel>, onAppClick: (String) -> Unit, onDragStart: (AppModel, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = viewModel()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null) {
        if (selectedCategory != null) {
            selectedCategory = null
        } else {
            searchQuery = ""
            focusManager.clearFocus()
            isSearchFocused = false
        }
    }

    val filteredApps = remember(allApps, searchQuery) { if (searchQuery.isBlank()) allApps else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) } }
    val hiddenApps = remember(filteredApps) { filteredApps.filter { it.isHidden } }
    val normalApps = remember(filteredApps) { filteredApps.filter { !it.isHidden } }
    val categories = remember(normalApps) { normalApps.groupBy { it.displayCategory }.toList().sortedBy { it.first }.toMutableList() }
    val showHiddenFolder = hiddenApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (selectedCategory == null) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it }, 
                    modifier = Modifier.weight(1f).onFocusChanged { isSearchFocused = it.isFocused },
                    placeholder = { Text("App Library", color = Color.White.copy(alpha = 0.6f)) }, 
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                    singleLine = true
                )
                if (isSearchFocused || searchQuery.isNotEmpty()) {
                    TextButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                        isSearchFocused = false
                    }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCategory = null }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Text(text = selectedCategory!!, style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.padding(start = 8.dp))
            }
        }
        
        if (isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null) {
            val appsToShow = remember(filteredApps, selectedCategory, isHiddenUnlocked) {
                val baseList = if (selectedCategory != null) { if (selectedCategory == "Hidden Apps") hiddenApps else categories.find { it.first == selectedCategory }?.second ?: emptyList() } else filteredApps
                if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) emptyList() else { if (selectedCategory == "Hidden Apps") baseList.sortedBy { it.label.lowercase() } else baseList.filter { !it.isHidden }.sortedBy { it.label.lowercase() } }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(appsToShow, key = { it.packageName }) { app ->
                    val lastPos = remember { object { var pos = Offset.Zero } }
                    Column(modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }) {
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) }, leadingContent = { if (app.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color.White)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (!app.isHidden) detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }
                        ); HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                val folderList = categories.toMutableList(); if (showHiddenFolder) folderList.add("Hidden Apps" to hiddenApps)
                items(folderList.chunked(2)) { chunk ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        chunk.forEach { (name, apps) ->
                            AppLibraryFolder(name = name, apps = apps, isLocked = name == "Hidden Apps" && !isHiddenUnlocked, onAppClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else onAppClick(it) }, onMoreClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else selectedCategory = name }, onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd, modifier = Modifier.weight(1f))
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showPasswordDialog = false }, title = { Text("Hidden Apps") }, text = { OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text("Enter Password") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()) }, confirmButton = { Button(onClick = { if (passwordInput == viewModel.getPassword()) { isHiddenUnlocked = true; showPasswordDialog = false } }) { Text("Unlock") } })
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppLibraryFolder(name: String, apps: List<AppModel>, onAppClick: (String) -> Unit, onMoreClick: () -> Unit, onDragStart: (AppModel, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit, modifier: Modifier = Modifier, isLocked: Boolean = false) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.aspectRatio(1f).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(0)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(1)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(2)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onMoreClick() })
                        else if (apps.size > 4) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).clickable { onMoreClick() }, contentAlignment = Alignment.Center) { Text("+${apps.size - 3}", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        else apps.getOrNull(3)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                }
            }
        }
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
    }
}
