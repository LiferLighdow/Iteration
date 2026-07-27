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

import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

@Composable
fun StackWidget(
    widget: WidgetModel,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val stackType = widget.type as? WidgetType.Stack
    val isWide = stackType?.isWide ?: false
    val isCyclic = stackType?.isCyclic ?: false
    val stackItems = stackType?.children ?: emptyList()
    
    // 循環滾動邏輯：如果開啟循環，則給予一個極大的初始頁數
    val initialPage = if (isCyclic && stackItems.size > 1) 500 * stackItems.size else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { 
        if (isCyclic && stackItems.size > 1) 1000 * stackItems.size else stackItems.size.coerceAtLeast(1) 
    }

    val stackAspectRatio = if (isWide) 2.0f else 1f

    Box(
        modifier = modifier.aspectRatio(stackAspectRatio)
    ) {
        if (stackItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.1f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_stack_hint), color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // 取得循環後的索引
                    val actualIndex = if (isCyclic) page % stackItems.size else page
                    val item = stackItems[actualIndex]
                    
                    // 計算縮放動畫
                    val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                    
                    // 縮小效果：從 1.0 縮小到 0.85
                    val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.15f)
                    val alpha = 1f - (pageOffset * 0.5f).coerceIn(0f, 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        when (val type = item.type) {
                            is WidgetType.Battery -> BatteryWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Clock -> {
                                if (type.isDigital) {
                                    DigitalClockWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                                } else {
                                    AnalogClockWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                                }
                            }
                            is WidgetType.Calendar -> CalendarWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Photo -> PhotoWidget(widget = item, viewModel = viewModel, modifier = Modifier.fillMaxSize(), drawFrame = true)
                            is WidgetType.Music -> MusicWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Note -> NoteWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.ToDoList -> TodoWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Weather -> WeatherWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.RSS -> RSSWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Countdown -> CountdownWidget(widget = item, displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.InfoHub -> InfoHubWidget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.InfoHub2 -> InfoHub2Widget(displayMode = item.displayMode, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
                            is WidgetType.Custom -> CustomWidget(widget = item, modifier = Modifier.fillMaxSize(), backdrop = backdrop, isMinusOnePage = isMinusOnePage, drawFrame = true)
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
                            val isSelected = (pagerState.currentPage % stackItems.size) == iteration
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
