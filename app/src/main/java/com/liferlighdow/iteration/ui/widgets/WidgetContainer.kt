package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.ui.components.GlassBox
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.viewmodel.MainViewModel

/**
 * 通用的 Widget 外框組件，統一處理玻璃擬態邏輯與容器外觀。
 */
@Composable
fun WidgetContainer(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f, // 預設 2x2 為 1.0, 4x2 為 2.0
    containerColor: Color? = null, // COLOR 模式下的自定義背景色
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false,
    drawFrame: Boolean = true, // 用於 StackWidget 內部時可設為 false
    content: @Composable BoxScope.() -> Unit
) {
    if (!drawFrame) {
        Box(modifier = modifier, content = content)
        return
    }

    val viewModel: MainViewModel = viewModel()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()

    val isGlassMode = displayMode == WidgetDisplayMode.GLASS
    val useLiquid = isGlassMode && isLiquidWidgetsEnabled

    if (useLiquid && backdrop != null) {
        GlassBox(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            backdrop = backdrop,
            cornerRadius = 24.dp,
            content = content
        )
    } else {
        val actualContainerColor = containerColor ?: when (displayMode) {
            WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
            WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.surfaceVariant
        }
        Card(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = actualContainerColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}
