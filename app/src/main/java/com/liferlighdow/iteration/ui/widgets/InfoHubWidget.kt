package com.liferlighdow.iteration.ui.widgets

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InfoHubWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null
    val isGlass = displayMode == WidgetDisplayMode.GLASS
    
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val context = LocalContext.current
    
    // Data states
    var batteryLevel by remember { mutableIntStateOf(0) }
    var batteryStatus by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var ramUsage by remember { mutableFloatStateOf(0f) }
    var ramText by remember { mutableStateOf("") }
    var storageUsage by remember { mutableFloatStateOf(0f) }
    var storageText by remember { mutableStateOf("") }
    var uptimeText by remember { mutableStateOf("") }

    // Update data
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            
            // System Uptime
            val upTimeMillis = android.os.SystemClock.elapsedRealtime()
            val hours = (upTimeMillis / (1000 * 60 * 60)).toInt()
            val minutes = ((upTimeMillis / (1000 * 60)) % 60).toInt()
            uptimeText = "${hours}h ${minutes}m"

            // Update RAM
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem
            val availRam = memInfo.availMem
            val usedRam = totalRam - availRam
            ramUsage = usedRam.toFloat() / totalRam.toFloat()
            ramText = "${Formatter.formatShortFileSize(context, usedRam)} / ${Formatter.formatShortFileSize(context, totalRam)}"

            // Update Storage
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalStorage = stat.blockCountLong * stat.blockSizeLong
            val availStorage = stat.availableBlocksLong * stat.blockSizeLong
            val usedStorage = totalStorage - availStorage
            storageUsage = usedStorage.toFloat() / totalStorage.toFloat()
            storageText = "${Formatter.formatShortFileSize(context, usedStorage)} / ${Formatter.formatShortFileSize(context, totalStorage)}"

            kotlinx.coroutines.delay(1000)
        }
    }

    // Battery Receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                batteryLevel = if (scale != 0) (level * 100 / scale.toFloat()).toInt() else 0
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                batteryStatus = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    else -> "Discharging"
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Section: Time & Device
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ).withGlassShadow(isGlass),
                        color = contentColor
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.labelMedium.withGlassShadow(isGlass),
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                // Right Section: Device Status Tags
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StatusTag(uptimeText, Icons.Default.Timer, contentColor)
                    StatusTag(Build.MODEL, Icons.Default.Smartphone, contentColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Grid - Two columns
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        label = "Battery",
                        value = "$batteryLevel%",
                        progress = batteryLevel / 100f,
                        icon = Icons.Default.BatteryStd,
                        color = if (batteryLevel > 20) Color(0xFF4CAF50) else Color(0xFFF44336),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        label = "RAM",
                        value = ramText.split(" / ").first(),
                        progress = ramUsage,
                        icon = Icons.Default.Memory,
                        color = Color(0xFF2196F3),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        label = "Storage",
                        value = storageText.split(" / ").first(),
                        progress = storageUsage,
                        icon = Icons.Default.SdStorage,
                        color = Color(0xFFFF9800),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        label = "Patch",
                        value = Build.VERSION.SECURITY_PATCH,
                        progress = 1.0f,
                        icon = Icons.Default.Security,
                        color = Color(0xFF9C27B0),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTag(
    value: String,
    icon: ImageVector,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = contentColor.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = contentColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    contentColor: Color,
    isGlass: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    tint = if (isGlass) Color.White else color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold).withGlassShadow(isGlass),
                    color = contentColor
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp).withGlassShadow(isGlass),
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (isGlass) Color.White else color,
            trackColor = contentColor.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
