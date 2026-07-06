package com.liferlighdow.iteration.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.widgets.*
import com.liferlighdow.iteration.ui.dialogs.*
import com.liferlighdow.iteration.data.WeatherProvider
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType

@Composable
fun MinusOnePage(
    widgets: List<WidgetModel>,
    viewModel: MainViewModel,
    backdrop: Backdrop? = null,
    isEditMode: Boolean = false,
    onAddClick: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetMode: (String, WidgetDisplayMode) -> Unit
) {
    var isReorderMode by remember { mutableStateOf(false) }
    val effectiveEditMode = isEditMode || isReorderMode
    
    // 拖動相關狀態
    var draggingWidgetId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val widgetPositions = remember { mutableStateMapOf<String, Rect>() }

    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var noteToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var weatherToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToAdjust by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToPick by remember { mutableStateOf<WidgetModel?>(null) }
    var showCropDialogByUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val mContext = LocalContext.current

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                mContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            showCropDialogByUri = it
        }
    }
    val mediaInfo by NotificationService.currentMedia.collectAsState()

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
                    (widget.type as? WidgetType.Music)?.isWide == true ||
                    (widget.type as? WidgetType.Note)?.isWide == true ||
                    (widget.type as? WidgetType.Weather)?.isWide == true ||
                    (widget.type as? WidgetType.Stack)?.isWide == true) 4 else 2
                GridItemSpan(span)
            }) { widget ->
                var showContextMenu by remember { mutableStateOf(false) }
                val index = widgets.indexOfFirst { it.id == widget.id }
                val isDragging = draggingWidgetId == widget.id

                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -1.5f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(150, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "jiggle"
                )

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { layoutCoordinates ->
                            val pos = layoutCoordinates.positionInRoot()
                            widgetPositions[widget.id] = Rect(pos, layoutCoordinates.size.toSize())
                        }
                        .graphicsLayer {
                            if (effectiveEditMode && !isDragging) {
                                rotationZ = rotation
                            }
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.05f
                                scaleY = 1.05f
                                alpha = 0.9f
                            }
                        }
                        .zIndex(if (isDragging) 10f else 0f)
                        .pointerInput(widget.id, effectiveEditMode) {
                            if (effectiveEditMode) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggingWidgetId = widget.id
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount

                                        val currentPos = widgetPositions[widget.id]?.center?.plus(dragOffset)
                                        if (currentPos != null) {
                                            val targetWidget = widgetPositions.entries.find { (id, rect) ->
                                                id != widget.id && rect.contains(currentPos)
                                            }
                                            targetWidget?.let { (targetId, _) ->
                                                val targetIndex = widgets.indexOfFirst { it.id == targetId }
                                                if (targetIndex != -1 && index != -1 && targetIndex != index) {
                                                    viewModel.reorderMinusOneWidgets(index, targetIndex)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingWidgetId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingWidgetId = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                        }
                        .pointerInput(widget.type, effectiveEditMode) {
                            if (!effectiveEditMode) {
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
                                                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
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
                                )
                            }
                        }
                ) {
                    when (widget.type) {
                        is WidgetType.Battery -> BatteryWidget(
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Clock -> AnalogClockWidget(
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Calendar -> CalendarWidget(
                            widget = widget,
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Photo -> PhotoWidget(widget = widget, viewModel = viewModel)
                        is WidgetType.Music -> MusicWidget(
                            widget = widget,
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Note -> NoteWidget(
                            widget = widget,
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Weather -> WeatherWidget(
                            displayMode = widget.displayMode,
                            backdrop = backdrop
                        )
                        is WidgetType.Stack -> StackWidget(
                            widget = widget,
                            viewModel = viewModel,
                            backdrop = backdrop
                        )
                    }

                    if (effectiveEditMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(24.dp)
                                .background(Color.Gray.copy(alpha = 0.9f), CircleShape)
                                .clickable { onRemoveWidget(widget.id) },
                            contentAlignment = Alignment.Center
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
                        if (widget.type !is WidgetType.Stack && widget.type !is WidgetType.Photo) {
                            if (widget.displayMode == WidgetDisplayMode.COLOR) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.widget_glass_mode)) },
                                    leadingIcon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        onUpdateWidgetMode(widget.id, WidgetDisplayMode.GLASS)
                                        showContextMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.widget_color_mode)) },
                                    leadingIcon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        onUpdateWidgetMode(widget.id, WidgetDisplayMode.COLOR)
                                        showContextMenu = false
                                    }
                                )
                            }
                        }
                        if (widget.type is WidgetType.Photo) {
                            val type = widget.type as WidgetType.Photo
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.choose_picture)) },
                                leadingIcon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    photoToPick = widget
                                    photoPickerLauncher.launch("image/*")
                                    showContextMenu = false
                                }
                            )

                            if (type.uri != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.adjust_position)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        photoToAdjust = widget
                                        showContextMenu = false
                                    }
                                )
                            }
                        }
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
                        if (widget.type is WidgetType.Note) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_note)) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    noteToEdit = widget
                                    showContextMenu = false
                                }
                            )
                        }
                        if (widget.type is WidgetType.Weather) {
                            val currentProvider by viewModel.weatherProvider.collectAsState()
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.choose_location)) },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                onClick = {
                                    weatherToEdit = widget
                                    showContextMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(if (currentProvider == WeatherProvider.MET_NORWAY) R.string.use_open_meteo else R.string.use_met_norway)) },
                                leadingIcon = { Icon(Icons.Default.Cloud, null) },
                                onClick = {
                                    viewModel.setWeatherProvider(
                                        if (currentProvider == WeatherProvider.MET_NORWAY) WeatherProvider.OPEN_METEO
                                        else WeatherProvider.MET_NORWAY
                                    )
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

    if (weatherToEdit != null) {
        LocationSearchDialog(
            viewModel = viewModel,
            onDismiss = { weatherToEdit = null }
        )
    }

    if (photoToAdjust != null) {
        val type = photoToAdjust!!.type as? WidgetType.Photo
        val uriStr = type?.uri
        if (uriStr != null) {
            ImageCropDialog(
                uri = android.net.Uri.parse(uriStr),
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
