package com.liferlighdow.iteration.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.AcUnit
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.widgets.*
import com.liferlighdow.iteration.ui.dialogs.*
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.WeatherProvider
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.data.CustomComponent
import com.liferlighdow.iteration.viewmodel.removeAppFromHomeWithAnimation
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.abs

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
    apps: List<AppModel>, columns: Int, rows: Int, iconSize: Dp,
    horizontalPadding: Dp = 16.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    draggingApp: AppModel?,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    pageOffsetProvider: () -> Float = { 0f },
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
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
    onBackgroundDragY: (Float) -> Unit = {},
    onBackgroundDragEnd: (Float) -> Unit = {},
    onEditApp: (AppModel) -> Unit = {},
    showWidgetLabel: Boolean = true
) {
    val notificationCounts by NotificationService.notifications.collectAsState()
    val mediaInfo by NotificationService.currentMedia.collectAsState()
    val mContext = LocalContext.current

    val draggingUniqueId = draggingApp?.uniqueId
    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var noteToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var todoToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var weatherToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var rssToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToAdjust by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToPick by remember { mutableStateOf<WidgetModel?>(null) }
    var showCropDialogByUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                mContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            showCropDialogByUri = it
        }
    }

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
                    is WidgetType.Weather -> if (type.isWide) 4 else 2
                    is WidgetType.ToDoList -> if (type.isWide) 4 else 2
                    is WidgetType.RSS -> 4
                    is WidgetType.InfoHub -> 4
                    is WidgetType.InfoHub2 -> 4
                    is WidgetType.Custom -> if (type.size == "4x2") 4 else 2
                    is WidgetType.Battery, is WidgetType.Clock -> 2
                    null -> 1
                }
                h = when (type) {
                    is WidgetType.RSS -> if (type.isTall) 4 else 2
                    null -> 1
                    else -> 2
                }
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
            .padding(horizontal = horizontalPadding)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            var hasTriggered = false
                            var directionLocked = 0 // 0: 未定, 1: 垂直鎖定 (Search), 2: 水平鎖定 (Pager), 3: 多指鎖定
                            var totalDragY = 0f
                            var totalDragX = 0f
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val pointerCount = event.changes.size
                                val anyPressed = event.changes.any { it.pressed }
                                if (!anyPressed) break

                                // 1. 偵測多指：一旦出現兩隻手指以上，立刻切換到多指模式
                                if (pointerCount >= 2 && directionLocked != 3) {
                                    directionLocked = 3
                                    // 多指模式下，立刻歸零搜尋位移，防止搜尋介面卡在半空中
                                    onBackgroundDragY(0f) 
                                }

                                val dragEvent = event.changes.firstOrNull() ?: break
                                totalDragY += (dragEvent.position.y - dragEvent.previousPosition.y)
                                totalDragX += (dragEvent.position.x - dragEvent.previousPosition.x)

                                // 2. 方向鎖定邏輯 (僅在單指時運作)
                                if (directionLocked == 0) {
                                    if (kotlin.math.abs(totalDragY) > touchSlop || kotlin.math.abs(totalDragX) > touchSlop) {
                                        if (kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 1.2f) {
                                            directionLocked = 1 // 垂直
                                        } else {
                                            directionLocked = 2 // 水平
                                        }
                                    }
                                }

                                when (directionLocked) {
                                    1 -> { // 單指垂直：全域搜尋拉動 (採用防誤觸高門檻)
                                        event.changes.forEach { it.consume() }
                                        onBackgroundDragY(totalDragY)
                                        if (!hasTriggered) {
                                            if (totalDragY > 80f) { // 下滑搜尋保持 80px 靈敏度
                                                onBackgroundSwipeDown(); hasTriggered = true
                                            } else if (totalDragY < -180f) { // 上滑門檻調高至 180px 以徹底防止誤觸
                                                onBackgroundSwipeUp(); hasTriggered = true
                                            }
                                        }
                                    }
                                    3 -> { // 多指模式：觸發雙指手勢
                                        if (!hasTriggered) {
                                            // 雙指手勢通常較為刻意，採用較平衡的門檻
                                            if (totalDragY > 80f) {
                                                onBackgroundTwoFingerSwipeDown(); hasTriggered = true
                                            } else if (totalDragY < -120f) {
                                                onBackgroundTwoFingerSwipeUp(); hasTriggered = true
                                            }
                                        }
                                        // 多指手勢也需消耗事件，防止 Pager 亂動
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                            // 放開手指後的收尾
                            if (directionLocked == 1) onBackgroundDragEnd(totalDragY)
                            else onBackgroundDragY(0f)
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
                        is WidgetType.Weather -> if (type.isWide) 4 else 2
                        is WidgetType.ToDoList -> if (type.isWide) 4 else 2
                        is WidgetType.RSS -> 4
                        is WidgetType.InfoHub -> 4
                        is WidgetType.InfoHub2 -> 4
                        is WidgetType.Custom -> if (type.size == "4x2") 4 else 2
                        is WidgetType.Battery, is WidgetType.Clock -> 2
                        else -> 1
                    }
                    h = when (type) {
                        is WidgetType.RSS -> if (type.isTall) 4 else 2
                        null -> 1
                        else -> 2
                    }
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
                
                val animX by animateDpAsState(targetX, label = "x")
                val animY by animateDpAsState(targetY, label = "y")

                val density = LocalDensity.current
                var showContextMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // 徹底移除彈性位移，保持圖示間距固定
                            // 同時移除 isScrolling 判定，確保座標系統始終一致，消除末端跳躍感
                            translationX = animX.toPx()
                            translationY = animY.toPx()
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
                                    onTap = { 
                                    if (!app.isWidget) {
                                        onAppClick(app, lastPosition.pos)
                                    } else {
                                        val widget = app.widget
                                        if (widget != null) {
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
                                                is WidgetType.Music -> {
                                                    mediaInfo?.packageName?.let { pkg ->
                                                        val intent = mContext.packageManager.getLaunchIntentForPackage(pkg)
                                                        if (intent != null) mContext.startActivity(intent)
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                }
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
                            labelFontSize = labelFontSize,
                            showLabel = showWidgetLabel,
                            showContextMenu = showContextMenu,
                            onContextMenuDismiss = { showContextMenu = false },
                            onUpdateStackToEdit = { stackToEdit = it },
                            onUpdateNoteToEdit = { noteToEdit = it },
                            onUpdateTodoToEdit = { todoToEdit = it },
                            onUpdateWeatherToEdit = { weatherToEdit = it },
                            onUpdateRssToEdit = { rssToEdit = it },
                            onUpdatePhotoToAdjust = { photoToAdjust = it },
                            onUpdatePhotoToPick = { photoToPick = it },
                            onShowContextMenu = { showContextMenu = true },
                            photoPickerLauncher = photoPickerLauncher
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
                            labelFontSize = labelFontSize,
                            showContextMenu = showContextMenu,
                            onContextMenuDismiss = { showContextMenu = false },
                            onAppClick = { onAppClick(app, lastPosition.pos) },
                            onEditApp = { onEditApp(app) },
                            notificationCountProvider = {
                                if (app.isFolder) {
                                    app.folderItems.sumOf { notificationCounts[it.packageName] ?: 0 }
                                } else {
                                    notificationCounts[app.packageName] ?: 0
                                }
                            },
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

    if (todoToEdit != null) {
        TodoEditDialog(
            widgetId = todoToEdit!!.id,
            initialTasks = (todoToEdit!!.type as WidgetType.ToDoList).tasks,
            viewModel = viewModel,
            onDismiss = { todoToEdit = null }
        )
    }

    if (weatherToEdit != null) {
        LocationSearchDialog(
            viewModel = viewModel,
            onDismiss = { weatherToEdit = null }
        )
    }

    if (rssToEdit != null) {
        RssEditDialog(
            widgetId = rssToEdit!!.id,
            initialUrl = (rssToEdit!!.type as WidgetType.RSS).url,
            viewModel = viewModel,
            onDismiss = { rssToEdit = null }
        )
    }

    if (photoToAdjust != null) {
        val type = photoToAdjust!!.type as? WidgetType.Photo
        val uriStr = type?.uri
        if (uriStr != null) {
            ImageCropDialog(
                uri = Uri.parse(uriStr),
                isWide = type.isWide,
                onDismiss = { photoToAdjust = null },
                onConfirm = { cropped ->
                    viewModel.saveWidgetPhoto(photoToAdjust!!.id, cropped)
                    photoToAdjust = null
                }
            )
        }
    }

    if (showCropDialogByUri != null && photoToPick != null) {
        val isWide = (photoToPick!!.type as? WidgetType.Photo)?.isWide ?: false
        ImageCropDialog(
            uri = showCropDialogByUri!!,
            isWide = isWide,
            onDismiss = { showCropDialogByUri = null; photoToPick = null },
            onConfirm = { cropped ->
                viewModel.saveWidgetPhoto(photoToPick!!.id, cropped)
                viewModel.updatePhotoWidgetUri(photoToPick!!.id, showCropDialogByUri.toString())
                showCropDialogByUri = null
                photoToPick = null
            }
        )
    }
}

@Composable
private fun WidgetGridItem(
    app: AppModel,
    draggingUniqueId: String?,
    isEditMode: Boolean,
    backdrop: Backdrop?,
    viewModel: MainViewModel,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    showLabel: Boolean = true,
    showContextMenu: Boolean,
    onContextMenuDismiss: () -> Unit,
    onUpdateStackToEdit: (WidgetModel) -> Unit,
    onUpdateNoteToEdit: (WidgetModel) -> Unit,
    onUpdateTodoToEdit: (WidgetModel) -> Unit,
    onUpdateWeatherToEdit: (WidgetModel) -> Unit,
    onUpdateRssToEdit: (WidgetModel) -> Unit,
    onUpdatePhotoToAdjust: (WidgetModel) -> Unit,
    onUpdatePhotoToPick: (WidgetModel) -> Unit,
    onShowContextMenu: () -> Unit,
    photoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val isWide = when (val type = app.widget?.type) {
        is WidgetType.Calendar -> type.isWide
        is WidgetType.Photo -> type.isWide
        is WidgetType.Music -> type.isWide
        is WidgetType.Note -> type.isWide
        is WidgetType.Stack -> type.isWide
        is WidgetType.Weather -> type.isWide
        is WidgetType.ToDoList -> type.isWide
        is WidgetType.RSS -> type.isWide
        is WidgetType.InfoHub -> true
        is WidgetType.InfoHub2 -> true
        is WidgetType.Custom -> type.size == "4x2"
        else -> false
    }

    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()

    val isBeingDragged = app.uniqueId == draggingUniqueId
    val alphaAnim by animateFloatAsState(
        targetValue = if (isBeingDragged) 0f else 1f,
        animationSpec = if (isBeingDragged) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "widgetAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isBeingDragged) 0.8f else 1.0f,
        animationSpec = if (isBeingDragged) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "widgetScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = if (isWide) 0.dp else 8.dp, // Wide Widget 整體下移 8dp
                bottom = if (isWide) 0.dp else 8.dp,
                start = 8.dp,
                end = 8.dp
            )
            .graphicsLayer { 
                alpha = alphaAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(if (showLabel) 0.85f else 1f)) {
            val widget = app.widget
            if (widget != null) {
                when (val type = widget.type) {
                    is WidgetType.Battery -> BatteryWidget(
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Clock -> {
                        if (type.isDigital) {
                            DigitalClockWidget(
                                displayMode = widget.displayMode,
                                modifier = Modifier.fillMaxSize(),
                                backdrop = backdrop
                            )
                        } else {
                            AnalogClockWidget(
                                displayMode = widget.displayMode,
                                modifier = Modifier.fillMaxSize(),
                                backdrop = backdrop
                            )
                        }
                    }
                    is WidgetType.Calendar -> CalendarWidget(
                        widget = widget,
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Photo -> PhotoWidget(
                        widget = widget,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    is WidgetType.Music -> MusicWidget(
                        widget = widget,
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Note -> NoteWidget(
                        widget = widget,
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Weather -> WeatherWidget(
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.ToDoList -> TodoWidget(
                        widget = widget,
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Stack -> StackWidget(
                        widget = widget,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.RSS -> RSSWidget(
                        widget = widget,
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.InfoHub -> InfoHubWidget(
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.InfoHub2 -> InfoHub2Widget(
                        displayMode = widget.displayMode,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop
                    )
                    is WidgetType.Custom -> CustomWidget(
                        widget = widget,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop,
                        onLongClick = onShowContextMenu
                    )
                }
            }

            if (isEditMode && !isDesktopLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .size(24.dp)
                        .background(Color.Gray.copy(alpha = 0.9f), CircleShape)
                        .clickable { viewModel.removeAppFromHomeWithAnimation(app.uniqueId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 標籤現在不參與 weight 計算，調整它的 padding 只會移動文字，不會縮放組件矩形
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = labelFontSize,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (isWide) 8.dp else 10.dp)
            )
        }

        DropdownMenu(expanded = showContextMenu, onDismissRequest = onContextMenuDismiss) {
            val widget = app.widget
            if (widget != null && widget.type !is WidgetType.Stack && widget.type !is WidgetType.Photo) {
                if (widget.displayMode == WidgetDisplayMode.COLOR) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.widget_glass_mode)) },
                        leadingIcon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            viewModel.updateWidgetDisplayMode(widget.id, WidgetDisplayMode.GLASS)
                            onContextMenuDismiss()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.widget_color_mode)) },
                        leadingIcon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            viewModel.updateWidgetDisplayMode(widget.id, WidgetDisplayMode.COLOR)
                            onContextMenuDismiss()
                        }
                    )
                }
            }
            if (app.widget?.type is WidgetType.Photo) {
                val type = app.widget.type as WidgetType.Photo
                
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.choose_picture)) },
                    leadingIcon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdatePhotoToPick(app.widget)
                        photoPickerLauncher.launch("image/*")
                        onContextMenuDismiss()
                    }
                )

                if (type.uri != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.adjust_position)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            onUpdatePhotoToAdjust(app.widget)
                            onContextMenuDismiss()
                        }
                    )
                }
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
                    text = { Text(stringResource(R.string.edit_note)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateNoteToEdit(app.widget!!)
                        onContextMenuDismiss()
                    }
                )
            }
            if (app.widget?.type is WidgetType.ToDoList) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_todo)) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAddCheck, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateTodoToEdit(app.widget!!)
                        onContextMenuDismiss()
                    }
                )
            }
            if (app.widget?.type is WidgetType.Weather) {
                val currentProvider by viewModel.weatherProvider.collectAsState()
                
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.choose_location)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateWeatherToEdit(app.widget!!)
                        onContextMenuDismiss()
                    }
                )
                
                DropdownMenuItem(
                    text = { Text(stringResource(if (currentProvider == WeatherProvider.MET_NORWAY) R.string.use_open_meteo else R.string.use_met_norway)) },
                    leadingIcon = { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        viewModel.setWeatherProvider(
                            if (currentProvider == WeatherProvider.MET_NORWAY) WeatherProvider.OPEN_METEO
                            else WeatherProvider.MET_NORWAY
                        )
                        onContextMenuDismiss()
                    }
                )
            }
            if (app.widget?.type is WidgetType.RSS) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_rss)) },
                    leadingIcon = { Icon(Icons.Default.RssFeed, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onUpdateRssToEdit(app.widget!!)
                        onContextMenuDismiss()
                    }
                )
            }
            if (!isDesktopLocked) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_delete_home)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        viewModel.removeAppFromHomeWithAnimation(app.uniqueId)
                        onContextMenuDismiss()
                    }
                )
            }
        }
    }
}


@Composable
private fun AppGridItem(
    app: AppModel,
    iconSize: Dp,
    isLiquidGlass: Boolean,
    backdrop: Backdrop?,
    iconShape: IconShape,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    isEditMode: Boolean,
    draggingUniqueId: String?,
    scale: Float,
    rotation: Float,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    showContextMenu: Boolean,
    onContextMenuDismiss: () -> Unit,
    onAppClick: () -> Unit,
    onEditApp: () -> Unit,
    notificationCountProvider: () -> Int,
    viewModel: MainViewModel
) {
    val mContext = LocalContext.current
    val isBeingDragged = app.uniqueId == draggingUniqueId
    val alphaAnim by animateFloatAsState(
        targetValue = if (isBeingDragged) 0f else 1f,
        animationSpec = if (isBeingDragged) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "itemAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isBeingDragged) 0.9f else 1.0f,
        animationSpec = if (isBeingDragged) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "itemScale"
    )

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
            labelFontSize = labelFontSize,
            getIcon = { pkg -> viewModel.getIcon(pkg) },
            onDeleteClick = {
                if (app.isPWA) {
                    showNativeUninstallDialog(mContext, app.label) {
                        viewModel.deletePWA(app)
                    }
                } else {
                    viewModel.removeAppFromHomeWithAnimation(app.uniqueId)
                }
            },
            notificationCountProvider = notificationCountProvider,
            modifier = Modifier.graphicsLayer {
                alpha = alphaAnim
                scaleX = scale * scaleAnim
                scaleY = scale * scaleAnim
                if (isEditMode && !isBeingDragged) rotationZ = rotation
            }
        )
        val menuOptions by viewModel.homeMenuOptions.collectAsState()
        val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
        
        val shortcuts = remember(showContextMenu) {
            if (showContextMenu && menuOptions.contains("shortcuts") && !app.isFolder && !app.isShortcut && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                viewModel.getAppShortcuts(app.packageName, app.userId)
            } else emptyList()
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = onContextMenuDismiss,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            val actionMode by viewModel.actionMode.collectAsState()
            if (!app.isFolder && menuOptions.contains("freeze") && (actionMode == ActionMode.SHIZUKU || actionMode == ActionMode.ROOT)) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (app.isFrozen) R.string.unfreeze else R.string.freeze)) },
                    leadingIcon = { Icon(Icons.Default.AcUnit, null) },
                    onClick = { viewModel.toggleFreezeApp(app, mContext); onContextMenuDismiss() }
                )
                HorizontalDivider()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1 && shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    DropdownMenuItem(
                        text = { 
                            @Suppress("NewApi")
                            Text(shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "") 
                        },
                        leadingIcon = {
                            val icon = viewModel.getShortcutIcon(shortcut)
                            if (icon != null) {
                                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            @Suppress("NewApi")
                            viewModel.launchShortcut(app.packageName, shortcut.id, app.userId)
                            onContextMenuDismiss()
                        }
                    )
                }
                HorizontalDivider()
            }

            if (menuOptions.contains("delete_home") && !isDesktopLocked) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_delete_home)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        viewModel.removeAppFromHomeWithAnimation(app.uniqueId)
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
                if (menuOptions.contains("uninstall") && !app.isSystem && !isDesktopLocked) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_uninstall)) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                        onClick = {
                            if (app.isPWA) {
                                showNativeUninstallDialog(mContext, app.label) {
                                    viewModel.deletePWA(app)
                                }
                                onContextMenuDismiss()
                                return@DropdownMenuItem
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
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                mContext.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("Iteration", "Open App Info failed", e)
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


