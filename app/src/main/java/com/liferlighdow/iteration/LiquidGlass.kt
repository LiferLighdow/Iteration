package com.liferlighdow.iteration

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 滿血版 Liquid Glass 實現，支持物理形變與高級混合模式
 */
fun Modifier.liquidGlass(
    enabled: Boolean,
    backdrop: Backdrop?,
    cornerRadius: Dp,
    tint: Color = Color.Unspecified
): Modifier = if (!enabled || backdrop == null) {
    this.drawBehind {
        val cr = cornerRadius.toPx()
        // 降低降級方案的感官
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            cornerRadius = CornerRadius(cr, cr)
        )
    }
} else {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(cornerRadius) },
        effects = {
            // 1. 徹底移除磨砂感，實現清澈的液態扭曲
            // 這裡不再使用 blur()

            // 2. 物理透鏡折射 (Lens Distortion)
            // 透過重映射背景像素產生形變，這是 Liquid 的靈魂
            lens(
                refractionHeight = 24f.dp.toPx(),
                refractionAmount = 48f.dp.toPx(),
                depthEffect = false,
                chromaticAberration = true 
            )
            
            // 3. 增加震盪感
            vibrancy()
        },
        onDrawSurface = {
            // 這裡保持完全清空，不添加任何流光、邊框或色塊填充
            // 讓視覺效果完全聚焦在物理重映射產生的液態形變上
        }
    )
}

fun Modifier.liquidGlassDock(
    isLiquidGlass: Boolean,
    backdrop: Backdrop,
    cornerRadius: Dp = 42.dp
): Modifier = this.liquidGlass(
    enabled = isLiquidGlass,
    backdrop = backdrop,
    cornerRadius = cornerRadius,
    tint = Color.Unspecified
)
