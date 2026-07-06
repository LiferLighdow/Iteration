package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.viewmodel.MainViewModel

@Composable
fun NoteWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val isWide = (widget.type as? WidgetType.Note)?.isWide ?: false
    val text = (widget.type as? WidgetType.Note)?.text ?: ""

    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(if (isWide) 2f else 1f)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = text.ifEmpty { "Tap to edit note..." },
                style = if (isWide) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (text.isEmpty()) contentColor.copy(alpha = 0.5f) else contentColor,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
