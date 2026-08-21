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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.data.*
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
 * 內部使用的顏色參數決策邏輯
 */
@Composable
private fun resolveLiquidGlassParams(
    viewModel: MainViewModel,
    component: LiquidGlassComponent?,
    // 手動傳入優先
    blur: Float?,
    refractionHeight: Float?,
    refractionAmount: Float?,
    chromaticAberration: Boolean?,
    colorAdjustmentEnabled: Boolean?,
    hue: Float?,
    saturation: Float?,
    brightness: Float?,
    alpha: Float?
): ResolvedParams {
    val configs by viewModel.liquidGlassComponentConfigs.collectAsState()
    val compCfg = component?.let { configs[it.key] }

    val gBlur by viewModel.liquidGlassBlur.collectAsState()
    val gReH by viewModel.liquidGlassRefractionHeight.collectAsState()
    val gReA by viewModel.liquidGlassRefractionAmount.collectAsState()
    val gChA by viewModel.liquidGlassChromaticAberration.collectAsState()
    val gAdj by viewModel.liquidGlassColorAdjustmentEnabled.collectAsState()
    val gH by viewModel.liquidGlassHue.collectAsState()
    val gS by viewModel.liquidGlassSaturation.collectAsState()
    val gB by viewModel.liquidGlassBrightness.collectAsState()
    val gA by viewModel.liquidGlassAlpha.collectAsState()

    // 物理參數邏輯：如果組件有自定義(非null)，則無視全域直接採用。
    // 注意：這裡不應受 compCfg.enabled 的影響。
    return ResolvedParams(
        blur = compCfg?.blur ?: blur ?: gBlur,
        refractionHeight = compCfg?.refractionHeight ?: refractionHeight ?: gReH,
        refractionAmount = compCfg?.refractionAmount ?: refractionAmount ?: gReA,
        chromaticAberration = compCfg?.chromaticAberration ?: chromaticAberration ?: gChA,
        
        // 顏色參數邏輯：只有當 enabled 為 true 時才採用組件顏色，否則回退。
        adjEnabled = colorAdjustmentEnabled ?: compCfg?.enabled ?: gAdj,
        h = (if (compCfg?.enabled == true) compCfg.hue else null) ?: hue ?: gH,
        s = (if (compCfg?.enabled == true) compCfg.saturation else null) ?: saturation ?: gS,
        b = (if (compCfg?.enabled == true) compCfg.brightness else null) ?: brightness ?: gB,
        a = (if (compCfg?.enabled == true) compCfg.alpha else null) ?: alpha ?: gA
    )
}

private data class ResolvedParams(
    val blur: Float,
    val refractionHeight: Float,
    val refractionAmount: Float,
    val chromaticAberration: Boolean,
    val adjEnabled: Boolean,
    val h: Float,
    val s: Float,
    val b: Float,
    val a: Float
)

/**
 * 滿血版 Liquid Glass 實現，支持物理形變與高級混合模式
 */
fun Modifier.liquidGlass(
    enabled: Boolean,
    backdrop: Backdrop?,
    cornerRadius: Dp = 0.dp,
    shape: Shape? = null,
    blurRadius: Float? = null,
    refractionHeight: Float? = null,
    refractionAmount: Float? = null,
    chromaticAberration: Boolean? = null,
    colorAdjustmentEnabled: Boolean? = null,
    hue: Float? = null,
    saturation: Float? = null,
    brightness: Float? = null,
    alpha: Float? = null,
    component: LiquidGlassComponent? = null
): Modifier = composed {
    val viewModel: MainViewModel = viewModel()
    val p = resolveLiquidGlassParams(viewModel, component, blurRadius, refractionHeight, refractionAmount, chromaticAberration, colorAdjustmentEnabled, hue, saturation, brightness, alpha)
    
    val fallbackColor = glassFallbackColor()
    val finalShape = shape ?: RoundedCornerShape(cornerRadius)
    val isAndroid12 = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    if (!enabled || backdrop == null || !isAndroid12) {
        this.drawBehind {
            val color = if (p.adjEnabled) {
                Color.hsv(p.h, p.s.coerceIn(0f, 1f), p.b.coerceIn(0f, 1f), p.a)
            } else {
                fallbackColor
            }
            if (shape != null) {
                val outline = shape.createOutline(size, layoutDirection, this)
                when (outline) {
                    is Outline.Generic -> drawPath(outline.path, color = color)
                    is Outline.Rectangle -> drawRect(color = color, topLeft = outline.rect.topLeft, size = outline.rect.size)
                    is Outline.Rounded -> {
                        val rr = outline.roundRect
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(rr.left, rr.top),
                            size = Size(rr.width, rr.height),
                            cornerRadius = CornerRadius(rr.bottomLeftCornerRadius.x, rr.bottomLeftCornerRadius.y)
                        )
                    }
                }
            } else {
                val cr = cornerRadius.toPx()
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(cr, cr)
                )
            }
        }
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { finalShape },
            effects = {
                if (p.blur > 0f) blur(radius = p.blur.dp.toPx())
                if (p.refractionHeight > 0f || p.refractionAmount > 0f) {
                    lens(
                        refractionHeight = p.refractionHeight.dp.toPx(),
                        refractionAmount = p.refractionAmount.dp.toPx(),
                        depthEffect = false,
                        chromaticAberration = p.chromaticAberration
                    )
                }
                if (p.refractionHeight > 0f || p.refractionAmount > 0f) vibrancy()
            },
            onDrawSurface = {
                if (p.adjEnabled) {
                    if (p.s < 1f) drawRect(color = Color.Gray.copy(alpha = 1f - p.s), blendMode = BlendMode.Color)
                    if (p.h != 0f) drawRect(color = Color.hsv(p.h, 1f, 1f), blendMode = BlendMode.Hue)
                    drawRect(color = Color.hsv(p.h, p.s, p.b).copy(alpha = p.a), blendMode = BlendMode.SrcOver)
                    if (p.b > 0.8f && p.s < 0.2f) {
                        drawRect(color = Color.White.copy(alpha = (p.b - 0.8f) * p.a), blendMode = BlendMode.Screen)
                    }
                }
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
    blurRadius: Float? = null,
    refractionHeight: Float? = null,
    refractionAmount: Float? = null,
    chromaticAberration: Boolean? = null,
    colorAdjustmentEnabled: Boolean? = null,
    hue: Float? = null,
    saturation: Float? = null,
    brightness: Float? = null,
    alpha: Float? = null
): Modifier = composed {
    val finalShape = if (dockStyle == DockStyle.PLATFORM) PlatformDockShape() else null
    val finalCornerRadius = if (dockStyle == DockStyle.CLASSIC) 0.dp else cornerRadius

    this.liquidGlass(
        enabled = isLiquidGlass,
        backdrop = backdrop,
        cornerRadius = finalCornerRadius,
        shape = finalShape,
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        colorAdjustmentEnabled = colorAdjustmentEnabled,
        hue = hue,
        saturation = saturation,
        brightness = brightness,
        alpha = alpha,
        component = LiquidGlassComponent.DOCK
    ).drawBehind {
        if (dockStyle == DockStyle.PLATFORM) {
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(size.width * 0.06f, 0f),
                end = Offset(size.width * 0.94f, 0f),
                strokeWidth = 2f
            )
        }
    }
}

