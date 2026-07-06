package com.liferlighdow.iteration.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WeatherProvider
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.viewmodel.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

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
    var showLocationSearch by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.menu_choose_widgets), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item { Text(stringResource(R.string.current_stack), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    items(children.size) { index ->
                        val item = children[index]
                        var showItemMenu by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = {
                                Text(item.label, style = MaterialTheme.typography.bodyLarge)
                            },
                            supportingContent = {
                                if (item.type !is WidgetType.Photo) {
                                    Text(stringResource(R.string.widget_mode_format, item.displayMode.name), style = MaterialTheme.typography.labelSmall)
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
                                                if (item.displayMode == WidgetDisplayMode.COLOR) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.widget_glass_mode)) },
                                                        leadingIcon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            val newList = children.toMutableList()
                                                            newList[index] = item.copy(displayMode = WidgetDisplayMode.GLASS)
                                                            children = newList
                                                            showItemMenu = false
                                                        }
                                                    )
                                                } else {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.widget_color_mode)) },
                                                        leadingIcon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            val newList = children.toMutableList()
                                                            newList[index] = item.copy(displayMode = WidgetDisplayMode.COLOR)
                                                            children = newList
                                                            showItemMenu = false
                                                        }
                                                    )
                                                }
                                            }

                                            if (item.type is WidgetType.Photo) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.choose_picture)) },
                                                    leadingIcon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
                                                    onClick = {
                                                        photoTargetId = item.id
                                                        photoLauncher.launch("image/*")
                                                        showItemMenu = false
                                                    }
                                                )
                                            }

                                            if (item.type is WidgetType.Note) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.edit_note)) },
                                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                                                    onClick = {
                                                        noteToEditInStack = index to item
                                                        showItemMenu = false
                                                    }
                                                )
                                            }

                                            if (item.type is WidgetType.Weather) {
                                                val currentProvider by viewModel.weatherProvider.collectAsState()
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.choose_location)) },
                                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                                                    onClick = {
                                                        showLocationSearch = true
                                                        showItemMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(if (currentProvider == WeatherProvider.MET_NORWAY) R.string.use_open_meteo else R.string.use_met_norway)) },
                                                    leadingIcon = { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary) },
                                                    onClick = {
                                                        viewModel.setWeatherProvider(if (currentProvider == WeatherProvider.MET_NORWAY) WeatherProvider.OPEN_METEO else WeatherProvider.MET_NORWAY)
                                                        showItemMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
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
                    item { Text(stringResource(R.string.available_widgets), style = MaterialTheme.typography.labelMedium) }

                    val available = listOf(
                        Triple(WidgetType.Battery, R.string.widget_battery, Icons.Default.BatteryStd),
                        Triple(WidgetType.Clock, R.string.widget_clock, Icons.Default.Schedule),
                        Triple(WidgetType.Calendar(false), R.string.widget_calendar, Icons.Default.CalendarMonth),
                        Triple(WidgetType.Music(false), R.string.widget_music, Icons.Default.MusicNote),
                        Triple(WidgetType.Photo(false), R.string.widget_photo, Icons.Default.AddAPhoto),
                        Triple(WidgetType.Note(text = "", isWide = false), null, Icons.Default.Note),
                        Triple(WidgetType.Weather(false), null, Icons.Default.WbSunny)
                    )
                    
                    val availableFiltered = if (isWide) {
                         listOf(
                            Triple(WidgetType.Calendar(true), R.string.widget_calendar_wide, Icons.Default.EventNote),
                            Triple(WidgetType.Music(true), R.string.widget_music_wide, Icons.Default.MusicVideo),
                            Triple(WidgetType.Photo(true), R.string.widget_photo_wide, Icons.Default.Rectangle),
                            Triple(WidgetType.Note(text = "", isWide = true), null, Icons.Default.Description),
                            Triple(WidgetType.Weather(true), null, Icons.Default.WbSunny)
                        )
                    } else available

                    items(availableFiltered.size) { idx ->
                        val (type, labelRes, icon) = availableFiltered[idx]
                        val label = if (labelRes != null) stringResource(labelRes) else {
                            when (type) {
                                is WidgetType.Note -> if (type.isWide) stringResource(R.string.widget_note_wide) else stringResource(R.string.widget_note)
                                is WidgetType.Weather -> if (type.isWide) stringResource(R.string.widget_weather_forecast) else stringResource(R.string.widget_weather)
                                else -> ""
                            }
                        }

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
                                    children = children + WidgetModel(widgetType = type, label = label)
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
        val targetIsWide = children.find { it.id == photoTargetId }?.let {
            (it.type as? WidgetType.Photo)?.isWide
        } ?: false
        ImageCropDialog(
            uri = cropUri!!,
            isWide = targetIsWide,
            onDismiss = { cropUri = null; photoTargetId = null },
            onConfirm = { croppedBitmap ->
                viewModel.saveWidgetPhoto(photoTargetId!!, croppedBitmap)
                cropUri = null
                photoTargetId = null
            }
        )
    }

    if (noteToEditInStack != null) {
        val (index, widget) = noteToEditInStack!!
        val initialText = (widget.type as? WidgetType.Note)?.text ?: ""
        
        var text by remember { mutableStateOf(initialText) }

        AlertDialog(
            onDismissRequest = { noteToEditInStack = null },
            title = { Text(stringResource(R.string.edit_note)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text(stringResource(R.string.note_placeholder)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newList = children.toMutableList()
                    val oldWidget = newList[index]
                    val newType = (oldWidget.type as WidgetType.Note).copy(text = text)
                    newList[index] = oldWidget.copy(widgetType = newType)
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

    if (showLocationSearch) {
        LocationSearchDialog(
            viewModel = viewModel,
            onDismiss = { showLocationSearch = false }
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

    canvas.drawBitmap(original, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
    return result
}

@Composable
fun LocationSearchDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob: Job? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.choose_location), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))

                // 使用 IP 定位按鈕
                Button(
                    onClick = {
                        viewModel.resetToIpLocation()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ip_location_auto))
                }

                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        searchJob?.cancel() // 取消之前的搜尋任務
                        if (it.length >= 2) {
                            searchJob = scope.launch(Dispatchers.IO) {
                                delay(500) // 延遲 500ms，防止使用者快速打字造成大量請求
                                isSearching = true
                                try {
                                    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(it, "UTF-8")}&count=10")
                                    val res = url.readText()
                                    val json = JSONObject(res)
                                    val array = json.optJSONArray("results")
                                    val list = mutableListOf<JSONObject>()
                                    if (array != null) {
                                        for (i in 0 until array.length()) list.add(array.getJSONObject(i))
                                    }
                                    withContext(Dispatchers.Main) {
                                        results = list
                                        isSearching = false
                                    }
                                } catch (e: Exception) {
                                    isSearching = false
                                }
                            }
                        } else {
                            results = emptyList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_city_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results) { item ->
                        val name = item.getString("name")
                        val country = item.optString("country", "")
                        val admin = item.optString("admin1", "")
                        val lat = item.getDouble("latitude")
                        val lon = item.getDouble("longitude")

                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(stringResource(R.string.location_admin_country, admin, country)) },
                            modifier = Modifier.clickable {
                                viewModel.updateLocation(lat, lon, name)
                                onDismiss()
                            }
                        )
                    }
                }
            }
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
        title = { Text(stringResource(R.string.edit_note)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text(stringResource(R.string.note_placeholder)) }
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
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.widget_picker_desc),
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
                        Triple(WidgetType.Stack(isWide = true), null, Icons.Default.DashboardCustomize),
                        Triple(WidgetType.Weather(isWide = true), null, Icons.Default.WbSunny)
                    )

                    items(widgets) { (type, labelRes, icon) ->
                        val label = if (labelRes != null) stringResource(labelRes) else {
                            when (type) {
                                is WidgetType.Note -> if (type.isWide) stringResource(R.string.widget_note_wide) else stringResource(R.string.widget_note)
                                is WidgetType.Stack -> if (type.isWide) stringResource(R.string.widget_stacker_wide) else stringResource(R.string.widget_stacker)
                                is WidgetType.Weather -> stringResource(R.string.widget_weather_forecast)
                                else -> ""
                            }
                        }
                        
                        val desc = when (type) {
                            is WidgetType.Battery -> stringResource(R.string.desc_battery)
                            is WidgetType.Clock -> stringResource(R.string.desc_clock)
                            is WidgetType.Calendar -> if (type.isWide) stringResource(R.string.desc_calendar_wide) else stringResource(R.string.desc_calendar)
                            is WidgetType.Photo -> stringResource(R.string.desc_photo)
                            is WidgetType.Music -> stringResource(R.string.desc_music)
                            is WidgetType.Note -> stringResource(R.string.desc_note)
                            is WidgetType.Stack -> if (type.isWide) stringResource(R.string.desc_stack_wide) else stringResource(R.string.desc_stack)
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
                            border = BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
