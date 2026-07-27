package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun CountdownWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    isMinusOnePage: Boolean = false,
    drawFrame: Boolean = true
) {
    val countdownType = widget.type as? WidgetType.Countdown ?: return
    val targetTime = countdownType.targetTimestamp
    val eventName = countdownType.eventName

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val diff = targetTime - currentTime
    val isPast = diff <= 0
    val absDiff = if (isPast) -diff else diff

    val days = TimeUnit.MILLISECONDS.toDays(absDiff)
    val hours = TimeUnit.MILLISECONDS.toHours(absDiff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(absDiff) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(absDiff) % 60

    WidgetContainer(
        displayMode = displayMode,
        modifier = modifier,
        aspectRatio = if (countdownType.isWide) 2f else 1f,
        backdrop = backdrop,
        isMinusOnePage = isMinusOnePage,
        drawFrame = drawFrame
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (eventName.isBlank()) "Countdown" else eventName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isPast && targetTime != 0L) {
                Text(
                    text = "Time's up!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (targetTime == 0L) {
                Text(
                    text = "Set a date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (days > 0) {
                        Text(
                            text = days.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "d ",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        style = if (days > 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (targetTime != 0L) {
                val locale = androidx.compose.ui.text.intl.Locale.current.platformLocale
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", locale)
                Text(
                    text = sdf.format(Date(targetTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
