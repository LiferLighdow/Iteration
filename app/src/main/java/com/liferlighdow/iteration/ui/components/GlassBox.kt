package com.liferlighdow.iteration.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.viewmodel.MainViewModel

/**
 * 通用的玻璃容器，自動處理來自 MainViewModel 的玻璃擬態參數。
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    cornerRadius: Dp = 24.dp,
    fallbackAlpha: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = enabled && isLiquidGlassEnabled && backdrop != null

    Surface(
        modifier = modifier.then(
            if (useLiquid) {
                Modifier.liquidGlass(
                    enabled = true,
                    backdrop = backdrop,
                    cornerRadius = cornerRadius,
                    blurRadius = blurRadius,
                    refractionHeight = refractionHeight,
                    refractionAmount = refractionAmount,
                    chromaticAberration = chromaticAberration
                )
            } else {
                Modifier
            }
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (useLiquid) Color.Transparent else glassFallbackColor(fallbackAlpha)
    ) {
        Box(content = content)
    }
}
