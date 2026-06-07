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
import androidx.compose.ui.draw.blur
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
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
            .fillMaxWidth()
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

@Composable
fun WideCalendarWidget(displayMode: WidgetDisplayMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val events = remember { mutableStateListOf<CalendarEvent>() }
    
    // 權限狀態追蹤
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
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

                    val list = mutableListOf<CalendarEvent>()
                    while (cursor.moveToNext()) {
                        list.add(CalendarEvent(
                            cursor.getString(titleIdx) ?: "No Title",
                            cursor.getLong(startIdx),
                            0L // End time not strictly used here
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
            .fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                    leadingContent = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
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
                val span = if ((widget.type as? WidgetType.Photo)?.isWide == true ||
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

    val isDefaultLauncher = remember(allAppsFlat) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(launcherBlur) // 套用高強度模糊
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
                                pageOffset = pageOffset, // 傳入偏移量
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
                    visible = !isAppLibraryPage && !isMinusOnePage,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
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
                    .width(320.dp)
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
                                    DropdownMenuItem(text = { Text(stringResource(R.string.folder_delete)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { viewModel.deleteFolder(folder.uniqueId); folderToOpenId = null; showMoreMenu = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val itemsPerPage = 9
                    val folderPages = remember(folder.folderItems) { folder.folderItems.chunked(itemsPerPage) }
                    val folderPagerState = rememberPagerState { folderPages.size }
                    
                    Box(modifier = Modifier.size(320.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = 0.25f)).padding(16.dp), contentAlignment = Alignment.TopCenter) {
                        HorizontalPager(state = folderPagerState, modifier = Modifier.fillMaxSize()) { pageIdx ->
                            val pageItems = folderPages[pageIdx]
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                pageItems.chunked(3).forEach { rowItems ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        rowItems.forEach { app ->
                                            val lastPos = remember { object { var pos = Offset.Zero } }
                                            Box(modifier = Modifier.weight(1f).onGloballyPositioned { lastPos.pos = it.positionInRoot() }, contentAlignment = Alignment.Center) {
                                                AppItem(app = app, onAppClick = { onAppClick(app.packageName); folderToOpenId = null }, iconSize = 58.dp, modifier = Modifier.pointerInput(app.uniqueId) { detectDragGesturesAfterLongPress(onDragStart = { offset -> draggingApp = app; touchPosition = lastPos.pos + offset; dragOffset = Offset.Zero; folderToOpenId = null }, onDrag = { _, _ -> }, onDragCancel = {}, onDragEnd = {}) })
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
                    value = globalSearchQuery, onValueChange = { globalSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
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
    apps: List<AppModel>, columns: Int, rows: Int, iconSize: androidx.compose.ui.unit.Dp, 
    draggingApp: AppModel?, 
    isEditMode: Boolean,
    viewModel: MainViewModel,
    pageOffset: Float = 0f, // 新增
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
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (app.isWidget) {
                        var showContextMenu by remember { mutableStateOf(false) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .graphicsLayer { alpha = if (app.uniqueId == draggingUniqueId) 0f else 1f }
                                .pointerInput(app.uniqueId, isEditMode) {
                                    detectTapGestures(
                                        onLongPress = { if (!isEditMode) showContextMenu = true }
                                    )
                                }
                                .pointerInput(app.uniqueId, isEditMode) {
                                    if (isEditMode) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { onDragStart(app, lastPosition.pos + it) },
                                            onDrag = { _, delta -> onDrag(delta) },
                                            onDragCancel = onDragEnd,
                                            onDragEnd = onDragEnd
                                        )
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                when (app.widget?.type) {
                                    is WidgetType.Battery -> BatteryWidget(displayMode = app.widget.displayMode)
                                    is WidgetType.Clock -> AnalogClockWidget(displayMode = app.widget.displayMode)
                                    is WidgetType.Calendar -> CalendarWidget(widget = app.widget, displayMode = app.widget.displayMode)
                                    is WidgetType.Photo -> PhotoWidget(widget = app.widget, viewModel = viewModel)
                                    else -> {}
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
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete_home)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { viewModel.removeAppFromHome(app.uniqueId); showContextMenu = false })
                            }
                        }
                    } else {
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
                                    detectTapGestures(
                                        onLongPress = { if (!isEditMode) showContextMenu = true },
                                        onTap = { onAppClick(app, lastPosition.pos) }
                                    )
                                }
                                .pointerInput(app.uniqueId, isEditMode) {
                                    if (isEditMode) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { onDragStart(app, lastPosition.pos + it) },
                                            onDrag = { _, delta -> onDrag(delta) },
                                            onDragCancel = onDragEnd,
                                            onDragEnd = onDragEnd
                                        )
                                    }
                                }
                            )
                            DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete_home)) }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { viewModel.removeAppFromHome(app.uniqueId); showContextMenu = false })
                                if (!app.isFolder) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_uninstall)) }, leadingIcon = { Icon(Icons.Default.DeleteForever, null) }, onClick = { val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:${app.packageName}") }; context.startActivity(intent); showContextMenu = false })
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
fun AppItem(app: AppModel, modifier: Modifier = Modifier, showLabel: Boolean = true, iconSize: androidx.compose.ui.unit.Dp = 62.dp, onAppClick: (() -> Unit)? = null) {
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
                        .clip(RoundedCornerShape(iconSize * 0.238f))
                        .background(Color.White.copy(alpha = 0.3f))
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(iconSize * 0.238f)
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
                    items(visibleApps, key = { it.packageName }) { app ->
                        ListItem(headlineContent = { Text(app.label) }, leadingContent = { val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap(); if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.4.dp)).background(Color.White)) }, trailingContent = { Checkbox(checked = selectedPackages.contains(app.packageName), onCheckedChange = { if (it) selectedPackages.add(app.packageName) else selectedPackages.remove(app.packageName) }) }, modifier = Modifier.clickable { if (selectedPackages.contains(app.packageName)) selectedPackages.remove(app.packageName) else selectedPackages.add(app.packageName) })
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
                    items(allApps, key = { it.packageName }) { app ->
                        ListItem(headlineContent = { Text(app.label) }, leadingContent = { val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap(); if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.7.dp)).background(Color.White)) }, modifier = Modifier.clickable { onAppSelected(app.packageName) })
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
    val context = LocalContext.current

    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

    // 用於重新命名的狀態
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var newLabelText by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null) {
        if (selectedCategory != null) selectedCategory = null
        else { searchQuery = ""; focusManager.clearFocus(); isSearchFocused = false }
    }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    val hiddenApps = remember(filteredApps) { filteredApps.filter { it.isHidden } }
    val normalApps = remember(filteredApps) { filteredApps.filter { !it.isHidden } }

    val categories = remember(normalApps, userCategories) {
        val grouped = normalApps.groupBy { it.displayCategory }
        val result = mutableListOf<Pair<String, List<AppModel>>>()
        userCategories.forEach { name -> grouped[name]?.let { result.add(name to it) } }
        val handledNames = userCategories.toSet()
        grouped.forEach { (name, apps) -> if (!handledNames.contains(name)) result.add(name to apps) }
        result
    }
    
    val showHiddenFolder = hiddenApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 搜尋欄：即使在分類中也顯示，但標題不同
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectedCategory != null) {
                IconButton(onClick = { selectedCategory = null }) { 
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            OutlinedTextField(
                value = searchQuery, 
                onValueChange = { searchQuery = it }, 
                modifier = Modifier.weight(1f).onFocusChanged { isSearchFocused = it.isFocused }, 
                placeholder = { 
                    Text(
                        if (selectedCategory != null) "Search in $selectedCategory" else stringResource(R.string.library_hint), 
                        color = Color.White.copy(alpha = 0.6f)
                    ) 
                }, 
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) }, 
                trailingIcon = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        }
                        if (isHiddenUnlocked && selectedCategory == null) {
                            IconButton(onClick = { isHiddenUnlocked = false }) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock_hidden_apps), tint = Color.White)
                            }
                        }
                    }
                }, 
                shape = RoundedCornerShape(28.dp), 
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), 
                singleLine = true
            )

            if (isSearchFocused || searchQuery.isNotEmpty()) {
                TextButton(onClick = { searchQuery = ""; focusManager.clearFocus(); isSearchFocused = false }) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        }

        val isLibraryView = isSearchFocused || searchQuery.isNotEmpty() || selectedCategory != null

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
                val appsToShow = remember(filteredApps, selectedCategory, isHiddenUnlocked, searchQuery) {
                    val baseList = if (selectedCategory != null) {
                        val catApps = if (selectedCategory == "Hidden Apps") hiddenApps
                                     else if (selectedCategory == "Suggestions") suggestedApps
                                     else categories.find { it.first == selectedCategory }?.second ?: emptyList()
                        // 如果有搜尋文字，則在分類內過濾
                        if (searchQuery.isNotBlank()) catApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                        else catApps
                    } else filteredApps

                    if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) emptyList()
                    else baseList.filter { !it.isHidden || selectedCategory == "Hidden Apps" }.sortedBy { it.label.lowercase() }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(appsToShow, key = { it.packageName }) { app ->
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
                                ).pointerInput(app.packageName) {
                                    if (!app.isHidden) detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd)
                                }
                            )

                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                                        context.startActivity(intent)
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_uninstall)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:${app.packageName}") }
                                        context.startActivity(intent)
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
                        AppLibraryFolder(name = name, apps = apps, isLocked = name == "Hidden Apps" && !isHiddenUnlocked, onAppClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else onAppClick(it) }, onMoreClick = { if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true else selectedCategory = name }, onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd)
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
fun AppLibraryFolder(name: String, apps: List<AppModel>, onAppClick: (String) -> Unit, onMoreClick: () -> Unit, onDragStart: (AppModel, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit, modifier: Modifier = Modifier, isLocked: Boolean = false) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    Column(modifier = modifier) {
        Box(modifier = Modifier.aspectRatio(1f).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(12.dp)) {
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
    val context = LocalContext.current
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
            ).pointerInput(app.packageName) {
                if (folderName != "Hidden Apps") detectDragGesturesAfterLongPress(onDragStart = { onDragStart(app, lastPos.pos + it) }, onDrag = { _, delta -> onDrag(delta) }, onDragCancel = onDragEnd, onDragEnd = onDragEnd)
            }
        )

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                    context.startActivity(intent)
                    showMenu = false
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_uninstall)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:${app.packageName}") }
                    context.startActivity(intent)
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

data class CalendarEvent(val title: String, val startTime: Long, val endTime: Long)
