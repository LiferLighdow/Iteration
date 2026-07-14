package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.platform.LocalContext
import com.liferlighdow.iteration.utils.WidgetParser
import kotlinx.coroutines.delay

import com.liferlighdow.iteration.data.WidgetActionType
import com.liferlighdow.iteration.data.WidgetClickAction
import com.liferlighdow.iteration.data.CustomShapeType
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun CustomWidget(
    widget: WidgetModel,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false,
    onLongClick: (() -> Unit)? = null, // 新增：傳遞給子組件
    workshopComponentModifier: ((CustomComponent) -> Modifier)? = null
) {
    val type = widget.type as? WidgetType.Custom ?: return
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    
    // 定時刷新信號，用於更新時間與電量
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while(true) {
            tick = System.currentTimeMillis()
            delay(1000 * 60) // 每分鐘更新一次
        }
    }

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

    val aspectRatio = if (type.size == "4x2") 2f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
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
                .graphicsLayer {
                    scaleX = type.globalScale
                    scaleY = type.globalScale
                    translationX = type.globalX.dp.toPx()
                    translationY = type.globalY.dp.toPx()
                },
            contentAlignment = Alignment.Center
        ) {
            // 使用 key(tick) 強制觸入定時重新渲染
            key(tick) {
                type.components.forEach { comp ->
                    val isVisible = remember(comp.isVisible, comp.visibilityFormula, tick) {
                        WidgetParser.parseVisibility(comp.visibilityFormula, comp.isVisible, context)
                    }
                    
                    if (isVisible) {
                        CustomComponentRenderer(
                            component = comp,
                            modifier = workshopComponentModifier?.invoke(comp) ?: Modifier,
                            onLongClick = onLongClick
                        ) { action ->
                            handleWidgetClick(action, context)
                        }
                    }
                }
            }
        }
    }
}

fun handleWidgetClick(action: WidgetClickAction?, context: Context) {
    if (action == null || action.type == WidgetActionType.NONE) return
    
    when (action.type) {
        WidgetActionType.OPEN_APP -> {
            action.value?.let { pkg ->
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "App not found: $pkg", Toast.LENGTH_SHORT).show()
                }
            }
        }
        WidgetActionType.OPEN_SEARCH -> {
            context.sendBroadcast(Intent("com.liferlighdow.iteration.ACTION_OPEN_SEARCH"))
        }
        WidgetActionType.OPEN_DRAWER -> {
            context.sendBroadcast(Intent("com.liferlighdow.iteration.ACTION_OPEN_DRAWER"))
        }
        WidgetActionType.LAUNCHER_SETTINGS -> {
            val intent = Intent(context, com.liferlighdow.iteration.SettingsActivity::class.java)
            context.startActivity(intent)
        }
        else -> {}
    }
}

@Composable
fun CustomComponentRenderer(
    component: CustomComponent,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null, // 新增：傳遞長按事件給父層
    onClick: (WidgetClickAction?) -> Unit
) {
    val context = LocalContext.current
    val color = remember(component, context) {
        val baseColor = when (component) {
            is CustomComponent.Text -> component.color
            is CustomComponent.Shape -> component.color
            is CustomComponent.Progress -> component.color
            is CustomComponent.Image -> 0
        }
        val formula = when (component) {
            is CustomComponent.Text -> component.colorFormula
            is CustomComponent.Shape -> component.colorFormula
            is CustomComponent.Progress -> component.colorFormula
            is CustomComponent.Image -> null
        }
        WidgetParser.parseColor(formula, baseColor, context)
    }

    Box(
        modifier = Modifier
            .offset(x = component.x.dp, y = component.y.dp)
            .then(modifier)
            .pointerInput(component.id, component.clickAction) {
                detectTapGestures(
                    onTap = { 
                        if (component.clickAction != null && component.clickAction?.type != WidgetActionType.NONE) {
                            onClick(component.clickAction)
                        }
                    },
                    onLongPress = { 
                        // 如果有傳入長按回調，則執行（通常是彈出 Widget 菜單）
                        onLongClick?.invoke() 
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (component) {
            is CustomComponent.Text -> {
                val parsedText = remember(component.content) {
                    WidgetParser.parseText(component.content, context)
                }
                Text(
                    text = parsedText,
                    fontSize = component.fontSize.sp,
                    color = Color(color)
                )
            }
            is CustomComponent.Shape -> {
                val shape = when (component.shapeType) {
                    CustomShapeType.RECTANGLE -> RoundedCornerShape(component.cornerRadius.dp)
                    CustomShapeType.CIRCLE -> androidx.compose.foundation.shape.CircleShape
                    CustomShapeType.TRIANGLE -> TriangleShape
                }
                Box(
                    modifier = Modifier
                        .size(component.width.dp, component.height.dp)
                        .background(Color(color), shape)
                )
            }
            is CustomComponent.Progress -> {
                val progressValue = remember(component.valueFormula) {
                    WidgetParser.parseValue(component.valueFormula, context)
                }
                if (component.isCircular) {
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.size(component.size.dp),
                        color = Color(color),
                        trackColor = Color(component.trackColor),
                        strokeWidth = component.strokeWidth.dp
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.width(component.size.dp),
                        color = Color(color),
                        trackColor = Color(component.trackColor),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            is CustomComponent.Image -> {
                AsyncImage(
                    model = component.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(component.width.dp, component.height.dp)
                        .clip(RoundedCornerShape(component.cornerRadius.dp))
                        .graphicsLayer { alpha = component.opacity },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
