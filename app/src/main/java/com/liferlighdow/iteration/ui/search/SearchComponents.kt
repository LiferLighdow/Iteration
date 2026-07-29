package com.liferlighdow.iteration.ui.search

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.ui.glassFallbackColor
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SearchLinkItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White) },
        leadingContent = { Icon(icon, contentDescription = null, tint = Color.White) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun BaseConversionRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun UnitConverterCard(result: String, context: Context, clipboard: ClipboardManager, label: String, icon: ImageVector, iconBgColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
            clipboard.setText(AnnotatedString(result))
            Toast.makeText(context, "Result Copied: $result", Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(iconBgColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = result, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SnowfallEffect() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tiltOffset by rememberTiltOffset(context)
    
    val snowflakes = remember {
        List(100) {
            Snowflake(
                x = (0..1000).random().toFloat() / 1000f,
                y = (0..1000).random().toFloat() / 1000f,
                speed = (10..40).random().toFloat() / 2000f,
                size = (2..8).random().toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        if (width > 0 && height > 0) {
            snowflakes.forEach { flake ->
                val currentY = (flake.y + time * (flake.speed * 50)) % 1f
                // 基礎小幅擺動 + 根據重力傾斜的偏移 (tiltOffset 影響橫向位移)
                // 調低權重，讓移動更細微且自然
                val xOffset = sin(time.toDouble() * 10.0 + flake.y.toDouble() * 100.0).toFloat() * 0.02f + (tiltOffset * 0.15f)
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = flake.size,
                    center = Offset(
                        x = ((flake.x + xOffset) % 1f + 1f) % 1f * width,
                        y = currentY * height
                    )
                )
            }
        }
    }
}

@Composable
fun SunlightEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val angleOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 畫幾個大範圍的光暈
        drawCircle(
            color = Color(0xFFFFE082).copy(alpha = alpha * 0.2f),
            radius = width * 0.6f,
            center = Offset(width * 0.9f, 0f)
        )

        // 畫光線
        val rayCount = 8
        for (i in 0 until rayCount) {
            val baseAngle = (i.toFloat() / rayCount) * 90f + 90f // 90 to 180 degrees (down and left)
            val finalAngle = baseAngle + angleOffset
            
            val angleRad = Math.toRadians(finalAngle.toDouble())
            val endX = width * 0.9f + Math.cos(angleRad).toFloat() * width * 1.5f
            val endY = Math.sin(angleRad).toFloat() * height * 1.5f

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.9f, 0f)
                lineTo(endX - 50.dp.toPx(), endY)
                lineTo(endX + 50.dp.toPx(), endY)
                close()
            }
            
            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = alpha * 0.4f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )
        }
    }
}

@Composable
fun rememberTiltOffset(context: Context): State<Float> {
    val offset = remember { mutableStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(accelSensor) {
        if (accelSensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            private var smoothedX = 0f
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // 使用低通濾波讓數值更平滑，減少抖動
                    val alpha = 0.1f
                    smoothedX = alpha * it.values[0] + (1 - alpha) * smoothedX
                    
                    // 加速度計 X 軸在 -9.8 到 9.8 之間
                    // 映射到一個合適的偏移範圍，並調低靈敏度
                    offset.value = (-smoothedX / 9.8f).coerceIn(-1f, 1f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return offset
}

private data class Snowflake(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float
)
