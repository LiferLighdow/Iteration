package com.liferlighdow.iteration

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 實現極具物理感的 Liquid Glass
 */
fun Modifier.liquidGlassDock(
    isLiquidGlass: Boolean,
    backdrop: Backdrop,
    cornerRadius: Dp = 42.dp
): Modifier = if (!isLiquidGlass) {
    this.drawBehind {
        val cr = cornerRadius.toPx()
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
            vibrancy()
            
            // 由於現在採樣源（backdrop）是半清晰的，我們在這裡實施強效模糊
            blur(24f.dp.toPx()) 

            // 實施強烈的透鏡折射
            // 這會讓 Dock 邊緣與主背景對接處產生明顯的「光學位移」，極大增強液態感
            lens(20f.dp.toPx(), 128f.dp.toPx()) 
        },
        onDrawSurface = {
            val cr = cornerRadius.toPx()
            
            // 表面通透漸變
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.2f),
                    1.0f to Color.Transparent
                )
            )

            // 高級三維邊框
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.05f)
                    )
                ),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    )
}
