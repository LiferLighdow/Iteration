package com.liferlighdow.iteration.ui.widgets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.viewmodel.MainViewModel

@Composable
fun StackWidget(
    widget: WidgetModel,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val isWide = (widget.type as? WidgetType.Stack)?.isWide ?: false
    val stackItems = (widget.type as? WidgetType.Stack)?.children ?: emptyList()
    val pagerState = rememberPagerState { stackItems.size.coerceAtLeast(1) }

    Card(
        modifier = modifier.aspectRatio(if (isWide) 2.0f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.1f))
    ) {
        if (stackItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_stack_hint), color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = stackItems[page]
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val type = item.type) {
                            is WidgetType.Battery -> BatteryWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.Clock -> {
                                if (type.isDigital) {
                                    DigitalClockWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                                } else {
                                    AnalogClockWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                                }
                            }
                            is WidgetType.Calendar -> CalendarWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.Photo -> PhotoWidget(widget = item, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            is WidgetType.Music -> MusicWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.Note -> NoteWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.ToDoList -> TodoWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.Weather -> WeatherWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.RSS -> RSSWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            is WidgetType.InfoHub -> InfoHubWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage)
                            else -> {}
                        }
                    }
                }

                // Page Indicator on the right
                if (stackItems.size > 1) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(stackItems.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val size by animateDpAsState(
                                targetValue = if (isSelected) 6.dp else 4.dp,
                                label = "dotSize"
                            )
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
