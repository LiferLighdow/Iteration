package com.liferlighdow.iteration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.utils.IconShape

/**
 * 根據設定獲取統一的圖示形狀
 */
@Composable
fun getAppIconShape(iconShape: IconShape, size: Dp): Shape {
    return if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(size * 0.238f)
}

/**
 * 搜尋結果與設定中常用的半透明玻璃卡片
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.1f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(alpha)),
        border = BorderStroke(1.dp, glassFallbackColor(borderAlpha)),
        content = content
    )
}
