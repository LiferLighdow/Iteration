package com.liferlighdow.iteration.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun glassFallbackColor(alpha: Float = 0.3f): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) Color.Black.copy(alpha = alpha) else Color.White.copy(alpha = alpha)
}

/**
 * 為玻璃模式下的文字提供陰影，增強在複雜背景下的可讀性
 */
fun TextStyle.withGlassShadow(enabled: Boolean = true): TextStyle {
    if (!enabled) return this
    return this.copy(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )
    )
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

class PlatformDockShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.06f, 0f)
            lineTo(size.width * 0.94f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
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
): Modifier = composed {
    val fallbackColor = glassFallbackColor()
    
    val shape = remember(dockStyle, cornerRadius) {
        when (dockStyle) {
            DockStyle.CLASSIC -> RoundedCornerShape(0.dp)
            DockStyle.MODERN, DockStyle.LITE -> RoundedCornerShape(cornerRadius)
            DockStyle.PLATFORM -> PlatformDockShape()
        }
    }

    if (dockStyle == DockStyle.LITE) {
        // LITE style doesn't draw any background or glass effect
        return@composed this
    }

    if (!isLiquidGlass || (dockStyle != DockStyle.PLATFORM && backdrop == null)) {
        this.drawBehind {
            if (dockStyle == DockStyle.PLATFORM) {
                val path = Path().apply {
                    moveTo(size.width * 0.06f, 0f)
                    lineTo(size.width * 0.94f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.2f))
            } else {
                val cr = if (dockStyle == DockStyle.CLASSIC) 0f else cornerRadius.toPx()
                drawRoundRect(
                    color = fallbackColor,
                    cornerRadius = CornerRadius(cr, cr)
                )
            }
        }
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (blurRadius > 0f) {
                    blur(radius = blurRadius.dp.toPx())
                }
                // 修正：lens 效果不支持自定義 Path 形狀 (PlatformDockShape)，會導致閃退
                // 因此在 PLATFORM 樣式下跳過 lens 效果
                if (dockStyle != DockStyle.PLATFORM && (refractionHeight > 0f || refractionAmount > 0f)) {
                    lens(
                        refractionHeight = refractionHeight.dp.toPx(),
                        refractionAmount = refractionAmount.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = chromaticAberration
                    )
                }
                if (refractionHeight > 0f || refractionAmount > 0f) {
                    vibrancy()
                }
            },
            onDrawSurface = {
                // Platform style adds a highlight line at the top
                if (dockStyle == DockStyle.PLATFORM) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(size.width * 0.06f, 0f),
                        end = Offset(size.width * 0.94f, 0f),
                        strokeWidth = 2f
                    )
                }
            }
        )
    }
}
