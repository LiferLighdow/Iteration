package com.liferlighdow.iteration

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun glassFallbackColor(alpha: Float = 0.3f): Color {
    return if (isSystemInDarkTheme()) Color.Black.copy(alpha = alpha) else Color.White.copy(alpha = alpha)
}

/**
 * 滿血版 Liquid Glass 實現，支持物理形變與高級混合模式
 */
fun Modifier.liquidGlass(
    enabled: Boolean,
    backdrop: Backdrop?,
    cornerRadius: Dp,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true
): Modifier = composed {
    val fallbackColor = glassFallbackColor()

    if (!enabled || backdrop == null) {
        this.drawBehind {
            val cr = cornerRadius.toPx()
            // 降低降級方案的感官
            drawRoundRect(
                color = fallbackColor,
                cornerRadius = CornerRadius(cr, cr)
            )
        }
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(cornerRadius) },
            effects = {
                // 1. 磨砂感 (Blur) - 僅在有數值時執行
                if (blurRadius > 0f) {
                    blur(radius = blurRadius.dp.toPx())
                }

                // 2. 物理透鏡折射 (Lens Distortion) - 僅在高度或強度大於 0 時執行
                // 這是最耗能的部分，跳過它可以大幅減輕 GPU 負擔
                if (refractionHeight > 0f || refractionAmount > 0f) {
                    lens(
                        refractionHeight = refractionHeight.dp.toPx(),
                        refractionAmount = refractionAmount.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = chromaticAberration
                    )
                }

                // 3. 增加震盪感 - 只有在開啟折射時才需要，或可視需求關閉以節能
                if (refractionHeight > 0f || refractionAmount > 0f) {
                    vibrancy()
                }
            },
            onDrawSurface = {
                // 這裡保持完全清空，不添加任何流光、邊框或色塊填充
            }
        )
    }
}

fun Modifier.liquidGlassDock(
    isLiquidGlass: Boolean,
    backdrop: Backdrop,
    dockStyle: DockStyle = DockStyle.MODERN,
    cornerRadius: Dp = 42.dp,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true
): Modifier = this.liquidGlass(
    enabled = isLiquidGlass,
    backdrop = backdrop,
    cornerRadius = if (dockStyle == DockStyle.CLASSIC) 0.dp else cornerRadius,
    blurRadius = blurRadius,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    chromaticAberration = chromaticAberration
)
