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
import androidx.compose.foundation.pager.HorizontalPager
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
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
fun BatteryWidget(displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.widget_battery),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { batteryLevel / 100f },
                    modifier = Modifier.size(60.dp),
                    color = if (batteryLevel > 20) {
                        if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.primary else Color.Green
                    } else Color.Red,
                    strokeWidth = 6.dp,
                    trackColor = contentColor.copy(alpha = 0.1f)
                )
                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun AnalogClockWidget(displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    val context = LocalContext.current
    
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
            .fillMaxWidth() // 使用 fillMaxWidth 以填滿 Grid 分配的空間
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
fun PhotoWidget(widget: WidgetModel, viewModel: MainViewModel, modifier: Modifier = Modifier) {
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
            .clickable { launcher.launch("image/*") },
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
    val context = LocalContext.current
    val originalBitmap = remember(uri) {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
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
fun CalendarWidget(widget: WidgetModel, displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
    val isWide = (widget.type as? WidgetType.Calendar)?.isWide ?: false
    
    if (isWide) {
        WideCalendarWidget(displayMode, modifier)
    } else {
        StandardCalendarWidget(displayMode, modifier)
    }
}

@Composable
fun StandardCalendarWidget(displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
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
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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

data class CalendarEvent(val title: String, val startTime: Long, val endTime: Long)

@Composable
fun WideCalendarWidget(displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val events = remember { mutableStateListOf<CalendarEvent>() }
    
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White.copy(alpha = 0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val accentColor = if (displayMode == WidgetDisplayMode.COLOR) MaterialTheme.colorScheme.primary else Color.Red

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val projection = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND
                )
                val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                    val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                    val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                    
                    val list = mutableListOf<CalendarEvent>()
                    while (cursor.moveToNext()) {
                        list.add(CalendarEvent(
                            cursor.getString(titleIdx),
                            cursor.getLong(startIdx),
                            cursor.getLong(endIdx)
                        ))
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
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
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
                    items(events) { event ->
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

@Composable
fun WidgetPickerDialog(onDismiss: () -> Unit, onWidgetSelected: (WidgetType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.select_widget),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_battery)) },
                    leadingContent = { Icon(Icons.Default.BatteryStd, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Battery)
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_clock)) },
                    leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Clock)
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_calendar)) },
                    leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Calendar(isWide = false))
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_calendar_wide)) },
                    leadingContent = { Icon(Icons.Default.EventNote, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Calendar(isWide = true))
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_photo)) },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Photo(isWide = false))
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_photo_wide)) },
                    leadingContent = { Icon(Icons.Default.Rectangle, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        onWidgetSelected(WidgetType.Photo(isWide = true))
                        onDismiss()
                    }
                )
                // 這裡以後可以加入更多 Widget 選項
            }
        }
    }
}

@Composable
fun MinusOnePage(
    widgets: List<WidgetModel>,
    viewModel: MainViewModel,
    onAddClick: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetMode: (String, WidgetDisplayMode) -> Unit
) {
    var isReorderMode by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                val span = if (widget.type is WidgetType.Battery || 
                    (widget.type as? WidgetType.Photo)?.isWide == true ||
                    (widget.type as? WidgetType.Calendar)?.isWide == true) 4 else 2
                GridItemSpan(span)
            }) { widget ->
                var showContextMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.pointerInput(widget.type) {
                        detectTapGestures(
                            onLongPress = { showContextMenu = true },
                            onTap = {
                                when (widget.type) {
                                    is WidgetType.Clock -> {
                                        try {
                                            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val pm = context.packageManager
                                                val fallbackIntent = pm.getLaunchIntentForPackage("com.google.android.deskclock")
                                                    ?: pm.getLaunchIntentForPackage("com.android.deskclock")
                                                if (fallbackIntent != null) context.startActivity(fallbackIntent)
                                            } catch (e2: Exception) {}
                                        }
                                    }
                                    is WidgetType.Calendar -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_APP_CALENDAR)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                ) {
                    when (widget.type) {
                        is WidgetType.Battery -> BatteryWidget(displayMode = widget.displayMode)
                        is WidgetType.Clock -> AnalogClockWidget(displayMode = widget.displayMode)
                        is WidgetType.Calendar -> CalendarWidget(widget = widget, displayMode = widget.displayMode)
                        is WidgetType.Photo -> PhotoWidget(widget = widget, viewModel = viewModel)
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
                    }
                }
            }
        }
    }
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
    val minusOneWidgets by viewModel.minusOneWidgets.collectAsState()
    
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

    val context = LocalContext.current
    val myPackageName = context.packageName

    // 檢查是否為預設啟動器
    val isDefaultLauncher = remember(allAppsFlat) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == myPackageName
    }

    // 拖拽核心狀態
    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var folderToOpen by remember { mutableStateOf<AppModel?>(null) }

    // 選單與對話框狀態
    var showDesktopMenu by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }

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

    val desktopPageCount = pages.size.coerceAtLeast(1)
    // 即使停止拖拽，如果新頁面已經建立(數據同步中)，也要維持頁數，避免 Pager 抖動
    val isPendingNewPage = draggingApp == null && pages.lastOrNull()?.isNotEmpty() == true && pages.size > 1
    val pageCount = 1 /* 負一頁 */ + desktopPageCount + (if (draggingApp != null) 2 else 1) /* Library */
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    
    val isMinusOnePage = pagerState.currentPage == 0
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
                userScrollEnabled = draggingApp == null, beyondViewportPageCount = 1
            ) { pageIndex ->
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
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 20) showGlobalSearch = true
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
                                draggingUniqueId = draggingApp?.uniqueId,
                                confirmedHoveredSlotIdx = if (confirmedHoveredKey?.startsWith("$pageIndex-") == true)
                                    confirmedHoveredKey?.substringAfter("-")?.toInt() else null,
                                confirmedIntent = if (isEditMode) MainViewModel.DropType.REORDER else confirmedIntent,
                                onAppClick = { pkg ->
                                    if (isEditMode) return@AppGrid
                                    val app = (pages.getOrNull(desktopIdx) ?: emptyList()).find { it.packageName == pkg }
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
                                            // 檢測當前所在的 Pager 頁面
                                            val currentPage = pagerState.currentPage
                                            if (currentPage == pageCount - 1) {
                                                // 丟在 App Library 頁面，視為移除
                                                viewModel.removeAppFromHome(draggingApp!!.uniqueId)
                                            } else {
                                                // 丟在普通分頁空白處，取當前頁面索引 (currentPage - 1)
                                                val targetIdx = (currentPage - 1).coerceIn(0, desktopPageCount)
                                                viewModel.handleAppDrop(draggingApp!!.uniqueId, null, targetIdx, false, MainViewModel.DropType.REORDER)
                                            }
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
                    visible = !isAppLibraryPage && !isMinusOnePage,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(400)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(animationSpec = tween(400))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PageIndicator(pageCount = desktopPageCount, currentPage = pagerState.currentPage - 1)
                        Dock(apps = dockApps, iconSize = iconSize, onAppClick = { pkg -> if (pkg == myPackageName) onSettingsClick() else onAppClick(pkg) }, onLongClick = { showDockPicker = it })
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
                ListItem(headlineContent = { Text(stringResource(R.string.menu_edit_mode)) }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { viewModel.setEditMode(true); showDesktopMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.menu_new_folder)) }, leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }, modifier = Modifier.clickable { showCreateFolderDialog = true; showDesktopMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.menu_wallpaper)) }, leadingContent = { Icon(Icons.Default.Image, contentDescription = null) }, modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_wallpaper)))
                    showDesktopMenu = false
                })
                if (!isDefaultLauncher) {
                    ListItem(headlineContent = { Text(stringResource(R.string.menu_set_default)) }, leadingContent = { Icon(Icons.Default.Home, contentDescription = null) }, modifier = Modifier.clickable {
                        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                        context.startActivity(intent)
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

    showDockPicker?.let { slotIndex -> AppPickerDialog(allApps = allAppsFlat, onDismiss = { showDockPicker = null }, onAppSelected = { pkg -> viewModel.updateDockApp(slotIndex, pkg); showDockPicker = null }) }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            onDismiss = { showWidgetPicker = false },
            onWidgetSelected = { viewModel.addWidget(it) }
        )
    }

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
                    placeholder = { Text(stringResource(R.string.search_hint), color = Color.White.copy(alpha = 0.6f)) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    shape = RoundedCornerShape(28.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.2f), unfocusedContainerColor = Color.White.copy(alpha = 0.2f), focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent),
                    singleLine = true
                )
                val filteredResults = remember(globalSearchQuery, allAppsFlat) { if (globalSearchQuery.isBlank()) allAppsFlat.take(8) else allAppsFlat.filter { it.label.contains(globalSearchQuery, ignoreCase = true) } }
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (globalSearchQuery.isBlank() && filteredResults.isNotEmpty()) {
                        item { Text(stringResource(R.string.app_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    items(filteredResults, key = { it.packageName }) { app ->
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
                                    context.startActivity(intent)
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
                                    context.startActivity(intent)
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
                            initialValue = -2.5f, targetValue = 2.5f,
                            animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
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
                                        text = { Text(stringResource(R.string.menu_delete_home)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            viewModel.removeAppFromHome(app.uniqueId)
                                            showContextMenu = false
                                        }
                                    )
                                    if (!app.isFolder) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_uninstall)) },
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
    val notificationCounts by NotificationService.notifications.collectAsState()
    val count = notificationCounts[app.packageName] ?: 0

    Column(modifier = modifier.padding(vertical = if (showLabel) 4.dp else 0.dp).then(if (onAppClick != null) Modifier.clickable { onAppClick() } else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (app.isFolder) {
                Box(modifier = Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.238f)).background(Color.White.copy(alpha = 0.3f)).padding(4.dp), contentAlignment = Alignment.Center) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(0), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(1), iconSize / 2.5f) }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FolderPreviewIcon(app.folderItems.getOrNull(2), iconSize / 2.5f); FolderPreviewIcon(app.folderItems.getOrNull(3), iconSize / 2.5f) }
                    }
                }
            } else if (app.processedIcon != null) {
                Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.238f)).background(Color.White), contentScale = ContentScale.FillBounds)
            }

            if (count > 0 && !app.isFolder) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (count > 9) 9.sp else 11.sp
                    )
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
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.folder_menu_desc), tint = Color.White)
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { isEditingName = true; showMoreMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.folder_add_app)) }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { showAppPicker = true; showMoreMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.folder_delete)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { onDeleteFolder(); showMoreMenu = false })
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
                    Text(stringResource(R.string.select_apps), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAppsSelected(selectedPackages.toList()) }) {
                        Text(stringResource(R.string.done))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap()
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.4.dp)).background(Color.White)
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
                Text(stringResource(R.string.select_app), style = MaterialTheme.typography.headlineSmall); Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap()
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.7.dp)).background(Color.White)
                                    )
                                }
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
fun AppLibraryPage(allApps: List<AppModel>, onAppClick: (String) -> Unit, onDragStart: (AppModel, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

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
    val categories = remember(normalApps, userCategories) { 
        val grouped = normalApps.groupBy { it.displayCategory }
        val result = mutableListOf<Pair<String, List<AppModel>>>()
        
        userCategories.forEach { name ->
            grouped[name]?.let { result.add(name to it) }
        }
        
        // 加入那些不在自定義分類中的 App
        val handledNames = userCategories.toSet()
        grouped.forEach { (name, apps) ->
            if (!handledNames.contains(name)) result.add(name to apps)
        }
        result
    }
    val showHiddenFolder = hiddenApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (selectedCategory == null) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).onFocusChanged { isSearchFocused = it.isFocused },
                    placeholder = { Text(stringResource(R.string.library_hint), color = Color.White.copy(alpha = 0.6f)) },
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
                        Text(stringResource(R.string.cancel), color = Color.White)
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
                val baseList = if (selectedCategory != null) { 
                    if (selectedCategory == "Hidden Apps") hiddenApps 
                    else if (selectedCategory == "Suggestions") suggestedApps
                    else categories.find { it.first == selectedCategory }?.second ?: emptyList() 
                } else filteredApps
                
                if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) emptyList() 
                else { 
                    if (selectedCategory == "Hidden Apps" || selectedCategory == "Suggestions") baseList.sortedBy { it.label.lowercase() } 
                    else baseList.filter { !it.isHidden }.sortedBy { it.label.lowercase() } 
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(appsToShow, key = { it.packageName }) { app ->
                    val lastPos = remember { object { var pos = Offset.Zero } }
                    Column(modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }) {
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) }, leadingContent = { if (app.processedIcon != null) Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.7.dp)).background(Color.White)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (!app.isHidden) detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }
                        ); HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                val folderList = mutableListOf<Pair<String, List<AppModel>>>()
                if (suggestedApps.isNotEmpty()) folderList.add("Suggestions" to suggestedApps)
                folderList.addAll(categories)
                if (showHiddenFolder) folderList.add("Hidden Apps" to hiddenApps)

                items(folderList) { (name, apps) ->
                    AppLibraryFolder(name = name, apps = apps, isLocked = name == "Hidden Apps" && !isHiddenUnlocked, onAppClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else onAppClick(it) }, onMoreClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else selectedCategory = name }, onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd)
                }
            }
        }
    }
    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false }, 
            title = { Text(stringResource(R.string.hidden_apps_title)) }, 
            text = { 
                OutlinedTextField(
                    value = passwordInput, 
                    onValueChange = { passwordInput = it }, 
                    label = { Text(stringResource(R.string.password_hint)) }, 
                    singleLine = true, 
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    }
                ) 
            }, 
            confirmButton = { Button(onClick = { if (passwordInput == viewModel.getPassword()) { isHiddenUnlocked = true; showPasswordDialog = false } }) { Text(stringResource(R.string.unlock)) } }
        )
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
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(0)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(1)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else apps.getOrNull(2)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isLocked) Box(modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() })
                        else if (apps.size > 4) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(15.1.dp)).clickable { onMoreClick() }, contentAlignment = Alignment.Center) { Text("+${apps.size - 3}", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        else apps.getOrNull(3)?.let { app -> val lastPos = remember { object { var pos = Offset.Zero } }; AppItem(app, showLabel = false, iconSize = 72.dp, modifier = Modifier.onGloballyPositioned { lastPos.pos = it.positionInRoot() }.combinedClickable(onClick = { onAppClick(app.packageName) }, onLongClick = { }).pointerInput(app.packageName) { if (name != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd) }) }
                    }
                }
            }
        }
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
    }
}
