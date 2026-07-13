package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.data.CustomComponent
import androidx.compose.ui.unit.sp
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.viewmodel.MainViewModel

@Composable
fun CustomWidget(
    widget: WidgetModel,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val type = widget.type as? WidgetType.Custom ?: return
    val viewModel: MainViewModel = viewModel()
    
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val displayMode = widget.displayMode
    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            type.components.forEach { comp ->
                if (comp.isVisible) {
                    CustomComponentRenderer(comp)
                }
            }
        }
    }
}

@Composable
fun CustomComponentRenderer(component: CustomComponent) {
    Box(
        modifier = Modifier
            .offset(x = component.x.dp, y = component.y.dp)
    ) {
        when (component) {
            is CustomComponent.Text -> {
                Text(
                    text = component.content,
                    fontSize = component.fontSize.sp,
                    color = Color(component.color)
                )
            }
            is CustomComponent.Shape -> {
                Box(
                    modifier = Modifier
                        .size(component.width.dp, component.height.dp)
                        .background(Color(component.color), RoundedCornerShape(component.cornerRadius.dp))
                )
            }
        }
    }
}

