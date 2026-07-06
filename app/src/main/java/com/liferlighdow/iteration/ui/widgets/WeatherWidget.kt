package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.fetchWeather

@Composable
fun WeatherWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    val isNetworkEnabled by viewModel.isNetworkAccessEnabled.collectAsState()
    val weatherInfo by viewModel.weatherInfo.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    LaunchedEffect(isNetworkEnabled) {
        if (isNetworkEnabled && weatherInfo == null) {
            viewModel.fetchWeather()
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
        if (!isNetworkEnabled) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, null, tint = contentColor.copy(alpha = 0.5f))
                    Text(stringResource(R.string.network_disabled), color = contentColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else if (weatherInfo == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        weatherError ?: stringResource(R.string.loading_weather),
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    if (weatherError != null) {
                        Text(
                            stringResource(R.string.tap_to_retry),
                            color = contentColor.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { viewModel.fetchWeather() }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val today = weatherInfo!!.daily.firstOrNull()
                    Icon(
                        imageVector = getWeatherIcon(today?.weatherCode ?: 0),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "${weatherInfo!!.currentTemp.toInt()}°",
                        style = MaterialTheme.typography.headlineLarge,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weatherInfo?.cityName ?: "Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                VerticalDivider(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp), color = contentColor.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.weight(3f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weatherInfo!!.daily.drop(1).take(5).forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.date.split("-").lastOrNull() ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Icon(
                                imageVector = getWeatherIcon(day.weatherCode),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${day.maxTemp.toInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${day.minTemp.toInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.CloudQueue
        45, 48 -> Icons.Default.Cloud
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.Umbrella
        71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.Cloud
    }
}
