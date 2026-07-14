package com.liferlighdow.iteration.ui.widgets

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
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
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

@Composable
fun InfoHub2Widget(
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
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val context = LocalContext.current
    
    // Data states
    var wifiSsid by remember { mutableStateOf("Disconnected") }
    var ipAddress by remember { mutableStateOf("0.0.0.0") }
    var brightness by remember { mutableIntStateOf(0) }
    var volume by remember { mutableIntStateOf(0) }
    var kernelVersion by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var refreshRate by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            // Wi-Fi & IP
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                wifiSsid = info.ssid.replace("\"", "")
                if (wifiSsid == "<unknown ssid>") wifiSsid = "Connected"
            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                wifiSsid = "Cellular Data"
            } else {
                wifiSsid = "Disconnected"
            }
            
            ipAddress = getIPAddress()

            // Brightness
            brightness = try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) * 100 / 255
            } catch (e: Exception) { 0 }

            // Volume
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            volume = if (maxVol != 0) (currVol * 100 / maxVol) else 0

            // Versions
            androidVersion = "Android ${Build.VERSION.RELEASE}"
            kernelVersion = System.getProperty("os.version") ?: "Unknown"
            
            // Refresh Rate
            refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.refreshRate ?: 60f
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.refreshRate
            }

            kotlinx.coroutines.delay(2000)
        }
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "System",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ).withGlassShadow(isGlass),
                        color = contentColor
                    )
                    Text(
                        text = "Dash Board v2",
                        style = MaterialTheme.typography.labelMedium.withGlassShadow(isGlass),
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StatusTag2(androidVersion, Icons.Default.Android, contentColor)
                    StatusTag2("${refreshRate.toInt()}Hz Display", Icons.Default.Refresh, contentColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow2(
                        label = "Network",
                        value = wifiSsid,
                        progress = 1.0f,
                        icon = Icons.Default.Wifi,
                        color = Color(0xFF00BCD4),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow2(
                        label = "Brightness",
                        value = "$brightness%",
                        progress = brightness / 100f,
                        icon = Icons.Default.LightMode,
                        color = Color(0xFFFFEB3B),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow2(
                        label = "Volume",
                        value = "$volume%",
                        progress = volume / 100f,
                        icon = Icons.Default.VolumeUp,
                        color = Color(0xFFE91E63),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow2(
                        label = "IP Address",
                        value = ipAddress,
                        progress = 1.0f,
                        icon = Icons.Default.Lan,
                        color = Color(0xFF8BC34A),
                        contentColor = contentColor,
                        isGlass = isGlass
                    )
                }
            }
        }
    }
}

private fun getIPAddress(): String {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: continue
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (isIPv4) return sAddr
                }
            }
        }
    } catch (e: Exception) { }
    return "0.0.0.0"
}

@Composable
private fun StatusTag2(
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
private fun InfoRow2(
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
