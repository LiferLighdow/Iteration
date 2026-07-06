package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun AnalogClockWidget(
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

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    var time by remember { mutableStateOf(Calendar.getInstance()) }

    val isGlass = displayMode == WidgetDisplayMode.GLASS
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
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
            Text(stringResource(R.string.clock_12), modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass))
            Text(stringResource(R.string.clock_6), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass))
            Text(stringResource(R.string.clock_3), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass))
            Text(stringResource(R.string.clock_9), modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp), color = contentColor, style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass))
        }
    }
}
