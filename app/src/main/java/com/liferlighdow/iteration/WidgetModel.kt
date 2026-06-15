package com.liferlighdow.iteration

import android.content.ContentUris
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.CalendarContract
import java.util.Calendar
import java.util.UUID
import kotlin.math.*

sealed class WidgetType {
    object Battery : WidgetType()
    object Clock : WidgetType()
    data class Calendar(val isWide: Boolean = false) : WidgetType()
    data class Photo(val isWide: Boolean = false) : WidgetType()
    data class Music(val isWide: Boolean = false) : WidgetType()
    data class Note(val text: String = "", val isWide: Boolean = false) : WidgetType()
    data class Stack(val children: List<WidgetModel> = emptyList(), val isWide: Boolean = false) : WidgetType()
}

enum class WidgetDisplayMode {
    GLASS, COLOR
}

data class WidgetModel(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType,
    val label: String,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.GLASS
)

data class CalendarEvent(val title: String, val startTime: Long, val endTime: Long)

@Composable
fun BatteryWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mContext = LocalContext.current
    var batteryLevel by remember { mutableStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        mContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { mContext.unregisterReceiver(receiver) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(R.string.widget_battery),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor
            )

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { batteryLevel / 100f },
                    modifier = Modifier.size(70.dp),
                    color = if (batteryLevel > 20) {
                        if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.primary else Color.Green
                    } else Color.Red,
                    strokeWidth = 8.dp,
                    trackColor = contentColor.copy(alpha = 0.1f)
                )
                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            Text(
                text = if (isCharging) "Charging" else "Discharging",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AnalogClockWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    var time by remember { mutableStateOf(Calendar.getInstance()) }
    val mContext = LocalContext.current

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val secondHandColor = if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.error else Color.Red

    LaunchedEffect(Unit) {
        while (true) {
            time = Calendar.getInstance()
            delay(1000)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val radius = size.width / 2f

                // Draw Dial
                drawCircle(
                    color = contentColor.copy(alpha = 0.1f),
                    radius = radius
                )

                // 12 Ticks
                for (i in 0 until 12) {
                    val angle = i * 30f
                    rotate(angle) {
                        val tickLength = if (i % 3 == 0) 12.dp.toPx() else 8.dp.toPx()
                        drawLine(
                            color = contentColor.copy(alpha = 0.6f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, tickLength),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Hands logic
                val hour = time.get(Calendar.HOUR)
                val minute = time.get(Calendar.MINUTE)
                val second = time.get(Calendar.SECOND)

                // Hour Hand
                rotate(hour * 30f + minute * 0.5f) {
                    drawLine(
                        color = contentColor,
                        start = center,
                        end = Offset(center.x, center.y - (radius * 0.5f)),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Minute Hand
                rotate(minute * 6f) {
                    drawLine(
                        color = contentColor,
                        start = center,
                        end = Offset(center.x, center.y - (radius * 0.75f)),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Second Hand
                rotate(second * 6f) {
                    drawLine(
                        color = secondHandColor,
                        start = center,
                        end = Offset(center.x, center.y - (radius * 0.85f)),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Pin
                drawCircle(color = contentColor, radius = 4.dp.toPx())
            }

            // 4 Numbers (12, 3, 6, 9)
            Text("12", modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall)
            Text("6", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall)
            Text("3", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall)
            Text("9", modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MusicWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val isWide = (widget.type as? WidgetType.Music)?.isWide ?: false
    if (isWide) {
        WideMusicWidget(displayMode, modifier, backdrop)
    } else {
        StandardMusicWidget(displayMode, modifier, backdrop)
    }
}

@Composable
fun StackWidget(
    widget: WidgetModel,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val isWide = (widget.type as? WidgetType.Stack)?.isWide ?: false
    val stackItems = (widget.type as? WidgetType.Stack)?.children ?: emptyList()
    val pagerState = rememberPagerState { stackItems.size.coerceAtLeast(1) }

    Card(
        modifier = modifier.aspectRatio(if (isWide) 2.1f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        if (stackItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty Stack\nLong press to add", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = stackItems[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    when (item.type) {
                        is WidgetType.Battery -> BatteryWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                        is WidgetType.Clock -> AnalogClockWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                        is WidgetType.Calendar -> CalendarWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                        is WidgetType.Photo -> PhotoWidget(widget = item, viewModel = viewModel, modifier = Modifier.fillMaxSize(), enableClick = false)
                        is WidgetType.Music -> MusicWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                        is WidgetType.Note -> NoteWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetStackPickerDialog(
    currentChildren: List<WidgetModel>,
    isWide: Boolean,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<WidgetModel>) -> Unit
) {
    var children by remember { mutableStateOf(currentChildren) }
    val mContext = LocalContext.current

    // 用於處理照片選擇的狀態
    var photoTargetId by remember { mutableStateOf<String?>(null) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { cropUri = it }
    }

    var noteToEditInStack by remember { mutableStateOf<Pair<Int, WidgetModel>?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.menu_choose_widgets), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item { Text("Current Stack", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    items(children.size) { index ->
                        val item = children[index]
                        var showItemMenu by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = {
                                Text(item.label, style = MaterialTheme.typography.bodyLarge)
                            },
                            supportingContent = {
                                if (item.type !is WidgetType.Photo) {
                                    Text("Mode: ${item.displayMode.name}", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val list = children.toMutableList()
                                        val moved = list.removeAt(index)
                                        list.add((index - 1).coerceAtLeast(0), moved)
                                        children = list
                                    }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(20.dp)) }

                                    Box {
                                        IconButton(onClick = { showItemMenu = true }) {
                                            Icon(Icons.Default.MoreVert, null)
                                        }
                                        DropdownMenu(expanded = showItemMenu, onDismissRequest = { showItemMenu = false }) {
                                            if (item.type !is WidgetType.Photo) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.widget_glass_mode)) },
                                                    onClick = {
                                                        val newList = children.toMutableList()
                                                        newList[index] = item.copy(displayMode = WidgetDisplayMode.GLASS)
                                                        children = newList
                                                        showItemMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.widget_color_mode)) },
                                                    onClick = {
                                                        val newList = children.toMutableList()
                                                        newList[index] = item.copy(displayMode = WidgetDisplayMode.COLOR)
                                                        children = newList
                                                        showItemMenu = false
                                                    }
                                                )
                                            }

                                            if (item.type is WidgetType.Photo) {
                                                DropdownMenuItem(
                                                    text = { Text("Choose Picture") },
                                                    onClick = {
                                                        photoTargetId = item.id
                                                        photoLauncher.launch("image/*")
                                                        showItemMenu = false
                                                    }
                                                )
                                            }

                                            if (item.type is WidgetType.Note) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit Note") },
                                                    onClick = {
                                                        noteToEditInStack = index to item
                                                        showItemMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    children = children.filterIndexed { i, _ -> i != index }
                                                    showItemMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
                    item { Text("Available Widgets", style = MaterialTheme.typography.labelMedium) }

                    val available = listOf(
                        Triple(WidgetType.Battery, "Battery", Icons.Default.BatteryStd),
                        Triple(WidgetType.Clock, "Clock", Icons.Default.Schedule),
                        Triple(WidgetType.Calendar(false), "Calendar", Icons.Default.CalendarMonth),
                        Triple(WidgetType.Music(false), "Music Player", Icons.Default.MusicNote),
                        Triple(WidgetType.Photo(false), "Photo", Icons.Default.AddAPhoto),
                        Triple(WidgetType.Note(text = "", isWide = false), "Note", Icons.Default.Note)
                    )
                    
                    val availableFiltered = if (isWide) {
                         listOf(
                            Triple(WidgetType.Calendar(true), "Calendar (Wide)", Icons.Default.EventNote),
                            Triple(WidgetType.Music(true), "Music Player (Wide)", Icons.Default.MusicVideo),
                            Triple(WidgetType.Photo(true), "Photo (Wide)", Icons.Default.Rectangle),
                            Triple(WidgetType.Note(text = "", isWide = true), "Note (Wide)", Icons.Default.Description)
                        )
                    } else available

                    items(availableFiltered.size) { idx ->
                        val (type, label, icon) = availableFiltered[idx]
                        val canAdd = when (type) {
                            is WidgetType.Photo -> true
                            is WidgetType.Note -> true
                            else -> children.none { it.type::class == type::class }
                        }

                        if (canAdd) {
                            ListItem(
                                headlineContent = { Text(label) },
                                leadingContent = { Icon(icon, null) },
                                modifier = Modifier.clickable {
                                    children = children + WidgetModel(type = type, label = label)
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(children); onDismiss() }) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }

    if (cropUri != null && photoTargetId != null) {
        ImageCropDialog(
            uri = cropUri!!,
            isWide = false,
            onDismiss = { cropUri = null; photoTargetId = null },
            onConfirm = { croppedBitmap ->
                viewModel.saveWidgetPhoto(photoTargetId!!, croppedBitmap)
                cropUri = null
                photoTargetId = null
                // 強制 UI 刷新 (雖然 PhotoWidget 會根據 id 讀取，但這裡列表資訊不變，沒關係)
            }
        )
    }

    if (noteToEditInStack != null) {
        val (index, widget) = noteToEditInStack!!
        val initialText = (widget.type as? WidgetType.Note)?.text ?: ""
        
        var text by remember { mutableStateOf(initialText) }

        AlertDialog(
            onDismissRequest = { noteToEditInStack = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Enter your note here...") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newList = children.toMutableList()
                    val oldWidget = newList[index]
                    val newType = (oldWidget.type as WidgetType.Note).copy(text = text)
                    newList[index] = oldWidget.copy(type = newType)
                    children = newList
                    noteToEditInStack = null
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToEditInStack = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun StandardMusicWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mediaInfo by NotificationService.currentMedia.collectAsState()

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Album Art
            mediaInfo?.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (displayMode == WidgetDisplayMode.GLASS) 20.dp else 40.dp)
                        .graphicsLayer(alpha = 0.3f),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mediaInfo?.title ?: "No Music Playing",
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaInfo?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaInfo?.albumArt != null) {
                        Image(
                            bitmap = mediaInfo!!.albumArt!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = contentColor.copy(alpha = 0.5f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { NotificationService.sendMediaCommand("previous") }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor)
                    }
                    FilledTonalIconButton(
                        onClick = { NotificationService.sendMediaCommand("play_pause") },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (displayMode == WidgetDisplayMode.GLASS) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (mediaInfo?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (displayMode == WidgetDisplayMode.GLASS) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { NotificationService.sendMediaCommand("next") }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WideMusicWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mediaInfo by NotificationService.currentMedia.collectAsState()

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(2f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Album Art
            mediaInfo?.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (displayMode == WidgetDisplayMode.GLASS) 20.dp else 40.dp)
                        .graphicsLayer(alpha = 0.3f),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art on the left
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaInfo?.albumArt != null) {
                        Image(
                            bitmap = mediaInfo!!.albumArt!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = contentColor.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info and Controls on the right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = mediaInfo?.title ?: "No Music Playing",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaInfo?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { NotificationService.sendMediaCommand("previous") }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { NotificationService.sendMediaCommand("play_pause") },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (displayMode == WidgetDisplayMode.GLASS) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (mediaInfo?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(32.dp),
                                tint = if (displayMode == WidgetDisplayMode.GLASS) Color.White else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { NotificationService.sendMediaCommand("next") }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoWidget(widget: WidgetModel, viewModel: MainViewModel, modifier: Modifier = Modifier, enableClick: Boolean = true) {
    var photo by remember(widget.id) { mutableStateOf(viewModel.getWidgetPhoto(widget.id)) }
    var showCropDialog by remember { mutableStateOf<Uri?>(null) }

    val isWide = (widget.type as? WidgetType.Photo)?.isWide ?: false
    val aspectRatio = if (isWide) 2.1f else 1f

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { showCropDialog = it }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .then(if (enableClick) Modifier.clickable { launcher.launch("image/*") } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(
                    bitmap = photo!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.widget_photo), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showCropDialog != null) {
        ImageCropDialog(
            uri = showCropDialog!!,
            isWide = isWide,
            onDismiss = { showCropDialog = null },
            onConfirm = { croppedBitmap ->
                viewModel.saveWidgetPhoto(widget.id, croppedBitmap)
                photo = croppedBitmap
                showCropDialog = null
            }
        )
    }
}

@Composable
fun ImageCropDialog(uri: Uri, isWide: Boolean, onDismiss: () -> Unit, onConfirm: (Bitmap) -> Unit) {
    val mContext = LocalContext.current
    val originalBitmap = remember(uri) {
        mContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } ?: return

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { containerSize = it.size.toSize() }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale *= zoom
                                offset += pan
                            }
                        }
                ) {
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // Crop Overlay
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val overlayWidth = containerSize.width * 0.9f
                    val overlayHeight = if (isWide) overlayWidth / 2.1f else overlayWidth

                    // Draw Overlay and Border
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Darken background outside crop area
                        val path = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addRoundRect(
                                RoundRect(
                                    left = center.x - overlayWidth / 2f,
                                    top = center.y - overlayHeight / 2f,
                                    right = center.x + overlayWidth / 2f,
                                    bottom = center.y + overlayHeight / 2f,
                                    cornerRadius = CornerRadius(24.dp.toPx())
                                )
                            )
                        }
                        drawPath(path, color = Color.Black.copy(alpha = 0.7f))

                        // 2. Draw clear white border for crop area
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(center.x - overlayWidth / 2f, center.y - overlayHeight / 2f),
                            size = Size(overlayWidth, overlayHeight),
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(onClick = {
                        val cropped = cropBitmap(originalBitmap, scale, offset, containerSize, isWide)
                        onConfirm(cropped)
                    }) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

fun cropBitmap(original: Bitmap, scale: Float, offset: Offset, containerSize: Size, isWide: Boolean): Bitmap {
    val overlayWidth = containerSize.width * 0.9f
    val overlayHeight = if (isWide) overlayWidth / 2.1f else overlayWidth

    val bitmapWidth = original.width.toFloat()
    val bitmapHeight = original.height.toFloat()

    val scaleX = containerSize.width / bitmapWidth
    val scaleY = containerSize.height / bitmapHeight
    val baseScale = Math.min(scaleX, scaleY)

    val totalScale = baseScale * scale

    val centerX = containerSize.width / 2f
    val centerY = containerSize.height / 2f

    val bitmapLeftInContainer = centerX - (bitmapWidth * totalScale) / 2f + offset.x
    val bitmapTopInContainer = centerY - (bitmapHeight * totalScale) / 2f + offset.y

    val cropLeftInContainer = centerX - overlayWidth / 2f
    val cropTopInContainer = centerY - overlayHeight / 2f

    val xOffsetInBitmap = (cropLeftInContainer - bitmapLeftInContainer) / totalScale
    val yOffsetInBitmap = (cropTopInContainer - bitmapTopInContainer) / totalScale
    val widthInBitmap = overlayWidth / totalScale
    val heightInBitmap = overlayHeight / totalScale

    val result = Bitmap.createBitmap(overlayWidth.roundToInt(), overlayHeight.roundToInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)

    val srcRect = android.graphics.Rect(
        xOffsetInBitmap.roundToInt(),
        yOffsetInBitmap.roundToInt(),
        (xOffsetInBitmap + widthInBitmap).roundToInt(),
        (yOffsetInBitmap + heightInBitmap).roundToInt()
    )
    val dstRect = android.graphics.Rect(0, 0, overlayWidth.roundToInt(), overlayHeight.roundToInt())

    canvas.drawBitmap(original, srcRect, dstRect, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    return result
}

@Composable
fun CalendarWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val isWide = (widget.type as? WidgetType.Calendar)?.isWide ?: false
    
    if (isWide) {
        WideCalendarWidget(displayMode, modifier, backdrop)
    } else {
        StandardCalendarWidget(displayMode, modifier, backdrop)
    }
}

@Composable
fun StandardCalendarWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val calendar = Calendar.getInstance()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "SUN"
        Calendar.MONDAY -> "MON"
        Calendar.TUESDAY -> "TUE"
        Calendar.WEDNESDAY -> "WED"
        Calendar.THURSDAY -> "THU"
        Calendar.FRIDAY -> "FRI"
        Calendar.SATURDAY -> "SAT"
        else -> ""
    }
    val month = when (calendar.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "JAN"
        Calendar.FEBRUARY -> "FEB"
        Calendar.MARCH -> "MAR"
        Calendar.APRIL -> "APR"
        Calendar.MAY -> "MAY"
        Calendar.JUNE -> "JUN"
        Calendar.JULY -> "JUL"
        Calendar.AUGUST -> "AUG"
        Calendar.SEPTEMBER -> "SEP"
        Calendar.OCTOBER -> "OCT"
        Calendar.NOVEMBER -> "NOV"
        Calendar.DECEMBER -> "DEC"
        else -> ""
    }

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val accentColor = if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.primary else Color.Red

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = contentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WideCalendarWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mContext = LocalContext.current
    val events = remember { mutableStateListOf<CalendarEvent>() }

    // 權限狀態追蹤
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                mContext,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val accentColor = if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.primary else Color.Red

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // 使用 Instances 查詢，這能正確處理重複性行程與跨日行程
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, startOfDay)
                ContentUris.appendId(builder, endOfDay)

                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END
                )

                mContext.contentResolver.query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val startIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)

                    val list = mutableListOf<CalendarEvent>()
                    if (titleIdx != -1 && startIdx != -1) {
                        while (cursor.moveToNext()) {
                            val title = cursor.getString(titleIdx) ?: "No Title"
                            val startTime = cursor.getLong(startIdx)
                            list.add(CalendarEvent(title, startTime, 0L))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        events.clear()
                        events.addAll(list)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(R.string.calendar_permission_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { launcher.launch(android.Manifest.permission.READ_CALENDAR) }) {
                        Text(stringResource(R.string.grant_permission), color = accentColor)
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Left part: Date
                Column(
                    modifier = Modifier.width(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val cal = Calendar.getInstance()
                    Text(
                        text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = contentColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = when(cal.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.SUNDAY -> "SUN"; Calendar.MONDAY -> "MON"; Calendar.TUESDAY -> "TUE"
                            Calendar.WEDNESDAY -> "WED"; Calendar.THURSDAY -> "THU"; Calendar.FRIDAY -> "FRI"
                            Calendar.SATURDAY -> "SAT"; else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }

                VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = contentColor.copy(alpha = 0.1f))

                // Right part: Events
                if (events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No events today", style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events, key = { it.title + it.startTime }) { event ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 16.dp).clip(CircleShape).background(accentColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = event.title, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val startTime = Calendar.getInstance().apply { timeInMillis = event.startTime }
                                    val timeStr = String.format("%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE))
                                    Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    val isWide = (widget.type as? WidgetType.Note)?.isWide ?: false
    val text = (widget.type as? WidgetType.Note)?.text ?: ""

    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(if (isWide) 2f else 1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = text.ifEmpty { "Tap to edit note..." },
                style = if (isWide) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (text.isEmpty()) contentColor.copy(alpha = 0.5f) else contentColor,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteEditDialog(
    widgetId: String,
    initialText: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Enter your note here...") }
            )
        },
        confirmButton = {
            Button(onClick = {
                viewModel.updateNoteText(widgetId, text)
                onDismiss()
            }) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun WidgetPickerDialog(onDismiss: () -> Unit, onWidgetSelected: (WidgetType) -> Unit) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, start = 28.dp, end = 28.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_widget),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize your home screen with powerful extensions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val widgets = listOf(
                        Triple(WidgetType.Battery, R.string.widget_battery, Icons.Default.BatteryStd),
                        Triple(WidgetType.Clock, R.string.widget_clock, Icons.Default.Schedule),
                        Triple(WidgetType.Calendar(isWide = false), R.string.widget_calendar, Icons.Default.CalendarMonth),
                        Triple(WidgetType.Calendar(isWide = true), R.string.widget_calendar_wide, Icons.Default.EventNote),
                        Triple(WidgetType.Photo(isWide = false), R.string.widget_photo, Icons.Default.AddAPhoto),
                        Triple(WidgetType.Photo(isWide = true), R.string.widget_photo_wide, Icons.Default.Rectangle),
                        Triple(WidgetType.Music(isWide = false), R.string.widget_music, Icons.Default.MusicNote),
                        Triple(WidgetType.Music(isWide = true), R.string.widget_music_wide, Icons.Default.MusicVideo),
                        Triple(WidgetType.Note(isWide = false), null, Icons.Default.Note),
                        Triple(WidgetType.Note(isWide = true), null, Icons.Default.Description),
                        Triple(WidgetType.Stack(isWide = false), R.string.widget_stacker, Icons.Default.Layers),
                        Triple(WidgetType.Stack(isWide = true), null, Icons.Default.DashboardCustomize)
                    )

                    items(widgets) { (type, labelRes, icon) ->
                        val label = if (labelRes != null) stringResource(labelRes) else {
                            when (type) {
                                is WidgetType.Note -> if (type.isWide) "Wide Note" else "Note"
                                is WidgetType.Stack -> "Wide Widget Stacker"
                                else -> ""
                            }
                        }
                        
                        val desc = when (type) {
                            is WidgetType.Battery -> "Monitor system battery level"
                            is WidgetType.Clock -> "Elegant analog time display"
                            is WidgetType.Calendar -> if (type.isWide) "View your daily schedule" else "Quick date overview"
                            is WidgetType.Photo -> "Display your favorite memories"
                            is WidgetType.Music -> "Control active media sessions"
                            is WidgetType.Note -> "Quickly jot down thoughts"
                            is WidgetType.Stack -> if (type.isWide) "Swipe through wide widgets" else "Stacked functional blocks"
                            else -> ""
                        }

                        Card(
                            onClick = {
                                onWidgetSelected(type)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ) 
                                },
                                supportingContent = {
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = { 
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
                
                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
