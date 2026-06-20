package com.liferlighdow.iteration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import android.content.Intent
import android.net.Uri

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
fun AppGrid(
    apps: List<AppModel>, columns: Int, rows: Int, iconSize: androidx.compose.ui.unit.Dp,
    draggingApp: AppModel?,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    pageOffset: Float = 0f,
    isLiquidGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    iconShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    confirmedHoveredSlotIdx: Int?, confirmedIntent: MainViewModel.DropType,
    onAppClick: (AppModel, Offset) -> Unit,
    onSlotPositioned: (Int, Rect) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onBackgroundLongPress: () -> Unit = {},
    onBackgroundClick: () -> Unit = {},
    onBackgroundDoubleTap: () -> Unit = {},
    onBackgroundSwipeUp: () -> Unit = {},
    onBackgroundSwipeDown: () -> Unit = {},
    onBackgroundTwoFingerSwipeUp: () -> Unit = {},
    onBackgroundTwoFingerSwipeDown: () -> Unit = {},
    onEditApp: (AppModel) -> Unit = {}
) {
    val draggingUniqueId = draggingApp?.uniqueId
    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var noteToEdit by remember { mutableStateOf<WidgetModel?>(null) }

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

    // 1. 預先計算佈局座標，避免在重組時重複運算
    val layoutItems = remember(displayApps, columns, rows) {
        val grid = Array(rows) { BooleanArray(columns) { false } }
        val result = mutableListOf<Triple<AppModel, Int, Int>>() // app, row, col
        
        displayApps.forEach { app ->
            val w: Int
            val h: Int
            if (app.isWidget) {
                val type = app.widget?.type
                w = when (type) {
                    is WidgetType.Calendar -> if (type.isWide) 4 else 2
                    is WidgetType.Photo -> if (type.isWide) 4 else 2
                    is WidgetType.Music -> if (type.isWide) 4 else 2
                    is WidgetType.Note -> if (type.isWide) 4 else 2
                    is WidgetType.Stack -> if (type.isWide) 4 else 2
                    is WidgetType.Battery, is WidgetType.Clock -> 2
                    null -> 1
                }
                h = 2
            } else {
                w = 1; h = 1
            }

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
                        result.add(Triple(app, foundRow, foundCol))
                        break@outer
                    }
                }
            }
        }
        result
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = false)
                            var hasTriggered = false
                            var totalDragY = 0f
                            var totalDragX = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                // 優先讓子元件（如 Stack Widget 或 Wide Calendar）處理手勢
                                if (event.changes.any { it.isConsumed }) break

                                val dragEvent = event.changes.firstOrNull() ?: break
                                if (!dragEvent.pressed) break 

                                totalDragY += (dragEvent.position.y - dragEvent.previousPosition.y)
                                totalDragX += (dragEvent.position.x - dragEvent.previousPosition.x)

                                if (!hasTriggered) {
                                    // 加入方向判定：垂直位移必須是水平位移的 2 倍以上，防止換頁誤觸
                                    val isVertical = kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 2f
                                    val pointerCount = event.changes.size
                                    
                                    if (isVertical) {
                                        if (pointerCount == 2) {
                                            if (totalDragY < -80f) {
                                                onBackgroundTwoFingerSwipeUp()
                                                hasTriggered = true
                                            } else if (totalDragY > 80f) {
                                                onBackgroundTwoFingerSwipeDown()
                                                hasTriggered = true
                                            }
                                        } else if (pointerCount == 1) {
                                            // 單指門檻提高
                                            if (totalDragY < -360f) {
                                                onBackgroundSwipeUp()
                                                hasTriggered = true
                                            } else if (totalDragY > 360f) {
                                                onBackgroundSwipeDown()
                                                hasTriggered = true
                                            }
                                        }
                                    }
                                }
                                
                                if (hasTriggered) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onDoubleTap = { onBackgroundDoubleTap() },
                    onLongPress = { onBackgroundLongPress() },
                    onTap = { onBackgroundClick() }
                )
            }
    ) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows

        layoutItems.forEachIndexed { index, (app, foundRow, foundCol) ->
            // 使用 key 讓 Compose 追蹤每個元件，減少不必要的重組
            key(app.uniqueId) {
                val w: Int
                val h: Int
                if (app.isWidget) {
                    val type = app.widget?.type
                    w = when (type) {
                        is WidgetType.Calendar -> if (type.isWide) 4 else 2
                        is WidgetType.Photo -> if (type.isWide) 4 else 2
                        is WidgetType.Music -> if (type.isWide) 4 else 2
                        is WidgetType.Note -> if (type.isWide) 4 else 2
                        is WidgetType.Stack -> if (type.isWide) 4 else 2
                        is WidgetType.Battery, is WidgetType.Clock -> 2
                        null -> 1
                    }
                    h = 2
                } else { w = 1; h = 1 }

                val lastPosition = remember { object { var pos = Offset.Zero } }
                val isHoveredFolder = confirmedHoveredSlotIdx == index && confirmedIntent == MainViewModel.DropType.FOLDER
                val scale by animateFloatAsState(if (isHoveredFolder) 1.25f else 1.0f, label = "folderScale")
                
                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -2.5f, targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), 
                    label = "jiggle"
                )

                // 目標位置計算
                val targetX = cellWidth * foundCol
                val targetY = cellHeight * foundRow
                
                // 只有在非滑動頁面時才使用平滑動畫，減少滑動時的計算壓力
                val isScrolling = pageOffset != 0f
                val animX by animateDpAsState(targetX, label = "x")
                val animY by animateDpAsState(targetY, label = "y")

                val density = LocalDensity.current
                // 彈性位移 (iOS 感)
                val elasticOffset = with(density) { pageOffset * (foundCol - (columns - 1) / 2f) * 12.dp.toPx() }
                var showContextMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // 改用 translation 處理位置，並整合彈性位移
                            // 在 Draw 階段計算，比 Modifier.offset 效能更好
                            translationX = with(density) { (if (isScrolling) targetX else animX).toPx() } + elasticOffset
                            translationY = with(density) { (if (isScrolling) targetY else animY).toPx() }
                        }
                        .size(cellWidth * w, cellHeight * h)
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
                                    onLongPress = { showContextMenu = true },
                                    onTap = { if (!app.isWidget) onAppClick(app, lastPosition.pos) }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (app.isWidget) {
                        WidgetGridItem(
                            app = app,
                            draggingUniqueId = draggingUniqueId,
                            isEditMode = isEditMode,
                            backdrop = backdrop,
                            viewModel = viewModel,
                            showContextMenu = showContextMenu,
                            onContextMenuDismiss = { showContextMenu = false },
                            onUpdateStackToEdit = { stackToEdit = it },
                            onUpdateNoteToEdit = { noteToEdit = it }
                        )
                    } else {
                        AppGridItem(
                            app = app,
                            iconSize = iconSize,
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            iconShape = iconShape,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration,
                            isEditMode = isEditMode,
                            draggingUniqueId = draggingUniqueId,
                            scale = scale,
                            rotation = rotation,
                            showContextMenu = showContextMenu,
                            onContextMenuDismiss = { showContextMenu = false },
                            onAppClick = { onAppClick(app, lastPosition.pos) },
                            onEditApp = { onEditApp(app) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (stackToEdit != null) {
        WidgetStackPickerDialog(
            currentChildren = (stackToEdit!!.type as WidgetType.Stack).children,
            isWide = (stackToEdit!!.type as WidgetType.Stack).isWide,
            viewModel = viewModel,
            onDismiss = { stackToEdit = null },
            onConfirm = { newChildren ->
                viewModel.updateStackChildren(stackToEdit!!.id, newChildren)
            }
        )
    }

    if (noteToEdit != null) {
        NoteEditDialog(
            widgetId = noteToEdit!!.id,
            initialText = (noteToEdit!!.type as WidgetType.Note).text,
            viewModel = viewModel,
            onDismiss = { noteToEdit = null }
        )
    }
}

@Composable
private fun WidgetGridItem(
    app: AppModel,
    draggingUniqueId: String?,
    isEditMode: Boolean,
    backdrop: com.kyant.backdrop.Backdrop?,
    viewModel: MainViewModel,
    showContextMenu: Boolean,
    onContextMenuDismiss: () -> Unit,
    onUpdateStackToEdit: (WidgetModel) -> Unit,
    onUpdateNoteToEdit: (WidgetModel) -> Unit
) {
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
                    is WidgetType.Note -> NoteWidget(widget = widget, displayMode = widget.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                    is WidgetType.Stack -> StackWidget(widget = widget, viewModel = viewModel, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                }
            }

            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .size(24.dp)
                        .background(Color.Gray.copy(alpha = 0.9f), CircleShape)
                        .clickable { viewModel.removeAppFromHome(app.uniqueId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
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

        DropdownMenu(expanded = showContextMenu, onDismissRequest = onContextMenuDismiss) {
            if (app.widget?.type !is WidgetType.Stack) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_glass_mode)) },
                    leadingIcon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        app.widget?.let { viewModel.updateWidgetDisplayMode(it.id, WidgetDisplayMode.GLASS) }
                        onContextMenuDismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_color_mode)) },
                    leadingIcon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        app.widget?.let { viewModel.updateWidgetDisplayMode(it.id, WidgetDisplayMode.COLOR) }
                        onContextMenuDismiss()
                    }
                )
            }
            if (app.widget?.type is WidgetType.Stack) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_choose_widgets)) },
                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateStackToEdit(app.widget)
                        onContextMenuDismiss()
                    }
                )
            }
            if (app.widget?.type is WidgetType.Note) {
                DropdownMenuItem(
                    text = { Text("Edit Note") },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateNoteToEdit(app.widget!!)
                        onContextMenuDismiss()
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_delete_home)) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    viewModel.removeAppFromHome(app.uniqueId)
                    onContextMenuDismiss()
                }
            )
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppModel,
    iconSize: androidx.compose.ui.unit.Dp,
    isLiquidGlass: Boolean,
    backdrop: com.kyant.backdrop.Backdrop?,
    iconShape: IconShape,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    isEditMode: Boolean,
    draggingUniqueId: String?,
    scale: Float,
    rotation: Float,
    showContextMenu: Boolean,
    onContextMenuDismiss: () -> Unit,
    onAppClick: () -> Unit,
    onEditApp: () -> Unit,
    viewModel: MainViewModel
) {
    val mContext = LocalContext.current
    val shortcuts = remember(showContextMenu) { if (showContextMenu && !app.isFolder) viewModel.getShortcuts(app.packageName) else emptyList() }
    Box {
        AppItem(
            app = app,
            iconSize = iconSize,
            isLiquidGlass = isLiquidGlass,
            backdrop = backdrop,
            iconShape = iconShape,
            blurRadius = blurRadius,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
            chromaticAberration = chromaticAberration,
            isEditMode = isEditMode,
            getIcon = { pkg -> viewModel.getIcon(pkg) },
            onDeleteClick = {
                if (app.isFolder) {
                    viewModel.removeAppFromHome(app.uniqueId)
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        mContext.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("Iteration", "Uninstall failed", e)
                    }
                }
            },
            modifier = Modifier.graphicsLayer {
                alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f
                scaleX = scale; scaleY = scale
                if (isEditMode && app.uniqueId != draggingUniqueId) rotationZ = rotation
            }
        )
        val menuOptions by viewModel.homeMenuOptions.collectAsState()

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = onContextMenuDismiss,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    DropdownMenuItem(
                        text = { Text(shortcut.label) },
                        leadingIcon = {
                            shortcut.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            viewModel.launchShortcut(app.packageName, shortcut.id)
                            onContextMenuDismiss()
                        }
                    )
                }
                HorizontalDivider()
            }
            if (menuOptions.contains("delete_home")) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_delete_home)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        viewModel.removeAppFromHome(app.uniqueId)
                        onContextMenuDismiss()
                    }
                )
            }
            if (menuOptions.contains("edit")) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (app.isFolder) R.string.rename else R.string.menu_edit)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = {
                        if (app.isFolder) onAppClick() else onEditApp()
                        onContextMenuDismiss()
                    }
                )
            }
            if (!app.isFolder) {
                if (menuOptions.contains("uninstall")) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_uninstall)) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                mContext.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("Iteration", "Uninstall failed", e)
                            }
                            onContextMenuDismiss()
                        }
                    )
                }
                if (menuOptions.contains("hide")) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_hide)) },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                        onClick = {
                            viewModel.toggleHiddenApp(app.packageName)
                            onContextMenuDismiss()
                        }
                    )
                }
                if (menuOptions.contains("app_info")) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_app_info)) },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                mContext.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("Iteration", "Open App Info failed", e)
                            }
                            onContextMenuDismiss()
                        }
                    )
                }
                if (menuOptions.contains("favorite")) {
                    val favoritePackages by viewModel.favoritePackages.collectAsState()
                    val isFavorite = favoritePackages.contains(app.packageName)
                    DropdownMenuItem(
                        text = { Text(stringResource(if (isFavorite) R.string.menu_remove_favorite else R.string.menu_add_favorite)) },
                        leadingIcon = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline, null) },
                        onClick = {
                            viewModel.toggleFavoriteApp(app.packageName)
                            onContextMenuDismiss()
                        }
                    )
                }
            }
        }
    }
}
