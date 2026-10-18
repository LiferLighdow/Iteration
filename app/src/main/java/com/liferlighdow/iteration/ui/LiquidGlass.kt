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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

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

    val dockTheme by viewModel.dockTheme.collectAsState()
    val dockCustomColor by viewModel.dockCustomColor.collectAsState()
    val dockOpacity by viewModel.dockOpacity.collectAsState()

    val baseAdj = colorAdjustmentEnabled ?: compCfg?.enabled ?: gAdj
    val baseH = (if (compCfg?.enabled == true) compCfg.hue else null) ?: hue ?: gH
    val baseS = (if (compCfg?.enabled == true) compCfg.saturation else null) ?: saturation ?: gS
    val baseB = (if (compCfg?.enabled == true) compCfg.brightness else null) ?: brightness ?: gB
    val baseA = (if (compCfg?.enabled == true) compCfg.alpha else null) ?: alpha ?: gA
    val finalBlur = compCfg?.blur ?: blur ?: gBlur

    val finalAdj: Boolean
    val finalH: Float
    val finalS: Float
    val finalB: Float
    val finalA: Float

    if (component == LiquidGlassComponent.DOCK) {
        when (dockTheme) {
            DockTheme.WHITE, DockTheme.DARK, DockTheme.CUSTOM -> {
                finalAdj = true
                val hsv = FloatArray(3)
                val targetColor = when (dockTheme) {
                    DockTheme.WHITE -> android.graphics.Color.WHITE
                    DockTheme.DARK -> android.graphics.Color.BLACK
                    else -> if (dockCustomColor != 0) dockCustomColor else android.graphics.Color.WHITE
                }
                android.graphics.Color.colorToHSV(targetColor, hsv)
                finalH = hsv[0]
                finalS = hsv[1]
                finalB = hsv[2]
                finalA = dockOpacity
            }
            DockTheme.FOLLOW_SYSTEM -> {
                finalAdj = baseAdj
                finalH = baseH
                finalS = baseS
                finalB = baseB
                finalA = baseA
            }
        }
    } else {
        finalAdj = baseAdj
        finalH = baseH
        finalS = baseS
        finalB = baseB
        finalA = baseA
    }

    // 物理參數邏輯：如果組件有自定義(非null)，則無視全域直接採用。
    // 注意：這裡不應受 compCfg.enabled 的影響。
    return ResolvedParams(
        blur = finalBlur,
        refractionHeight = compCfg?.refractionHeight ?: refractionHeight ?: gReH,
        refractionAmount = compCfg?.refractionAmount ?: refractionAmount ?: gReA,
        chromaticAberration = compCfg?.chromaticAberration ?: chromaticAberration ?: gChA,
        
        adjEnabled = finalAdj,
        h = finalH,
        s = finalS,
        b = finalB,
        a = finalA
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
        this.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }.drawBackdrop(
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
                    val tintColor = Color.hsv(p.h, p.s.coerceIn(0f, 1f), p.b.coerceIn(0f, 1f)).copy(alpha = p.a)
                    drawRect(color = tintColor, blendMode = BlendMode.SrcOver)
                }
            }
        )
    }
}

class PlatformDockShape(private val trapezoidHeightDp: Dp = 44.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val trapHeight = with(density) { trapezoidHeightDp.toPx() }
        val path = Path().apply {
            // 頂部梯形部分
            moveTo(size.width * 0.06f, 0f)
            lineTo(size.width * 0.94f, 0f)
            lineTo(size.width, trapHeight.coerceAtMost(size.height))
            
            // 如果有剩餘高度（如 navPadding 或 dockOffset），向下延伸為矩形底座，防止變形
            if (size.height > trapHeight) {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            
            lineTo(0f, trapHeight.coerceAtMost(size.height))
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
    val finalShape = if (dockStyle == DockStyle.PLATFORM) PlatformDockShape(44.dp) else null
    val finalCornerRadius = if (dockStyle == DockStyle.CLASSIC) 0.dp else cornerRadius

    // 3D Platform 風格使用自訂路徑形狀 (PlatformDockShape)，不支援透鏡折射扭曲效果，強制設為 0 避免閃退
    val effectiveRefractionHeight = if (dockStyle == DockStyle.PLATFORM) 0f else refractionHeight
    val effectiveRefractionAmount = if (dockStyle == DockStyle.PLATFORM) 0f else refractionAmount
    val effectiveChromaticAberration = if (dockStyle == DockStyle.PLATFORM) false else chromaticAberration

    this.liquidGlass(
        enabled = isLiquidGlass,
        backdrop = backdrop,
        cornerRadius = finalCornerRadius,
        shape = finalShape,
        blurRadius = blurRadius,
        refractionHeight = effectiveRefractionHeight,
        refractionAmount = effectiveRefractionAmount,
        chromaticAberration = effectiveChromaticAberration,
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

