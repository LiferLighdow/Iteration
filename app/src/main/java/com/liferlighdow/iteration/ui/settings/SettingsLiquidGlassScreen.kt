package com.liferlighdow.iteration.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.*
import com.liferlighdow.iteration.data.*
import com.liferlighdow.iteration.viewmodel.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidGlassDockEnabled by viewModel.isLiquidGlassDockEnabled.collectAsState()
    val isLiquidGlassHomeFolderEnabled by viewModel.isLiquidGlassHomeFolderEnabled.collectAsState()
    val isLiquidGlassAppLibraryFolderEnabled by viewModel.isLiquidGlassAppLibraryFolderEnabled.collectAsState()
    val isLiquidGlassGlobalSearchEnabled by viewModel.isLiquidGlassGlobalSearchEnabled.collectAsState()
    val isLiquidGlassAppLibrarySearchEnabled by viewModel.isLiquidGlassAppLibrarySearchEnabled.collectAsState()
    val isLiquidGlassWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()
    val isLiquidGlassMinusOneWidgetEnabled by viewModel.isLiquidGlassMinusOneWidgetEnabled.collectAsState()
    val isLiquidGlassMinusOneSearchEnabled by viewModel.isLiquidGlassMinusOneSearchEnabled.collectAsState()
    val isLiquidGlassMinusOneButtonEnabled by viewModel.isLiquidGlassMinusOneButtonEnabled.collectAsState()

    var showGlobalEditor by remember { mutableStateOf(false) }
    var componentToEdit by remember { mutableStateOf<Pair<LiquidGlassComponent, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liquid_glass_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        TextButton(onClick = { viewModel.resetLiquidGlassParams() }) {
                            Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (android.os.Build.VERSION.SDK_INT < 31) {
                item {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.liquid_glass_workshop_unavailable),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.compat_warning_msg, android.os.Build.VERSION.SDK_INT),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                SettingSwitchItem(
                    title = stringResource(R.string.enable_liquid_glass_title),
                    supportingText = stringResource(R.string.enable_liquid_glass_desc),
                    checked = isLiquidGlassEnabled,
                    onCheckedChange = { viewModel.setLiquidGlassEnabled(it) }
                )
            }

            if (isLiquidGlassEnabled && android.os.Build.VERSION.SDK_INT >= 31) {
                item {
                    SettingCategoryHeader(stringResource(R.string.visual_effects))
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.visual_effects)) },
                        supportingContent = { Text(stringResource(R.string.visual_effects_desc)) },
                        trailingContent = {
                            Button(onClick = { showGlobalEditor = true }) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.edit))
                            }
                        },
                        modifier = Modifier.clickable { showGlobalEditor = true }
                    )
                }

                item {
                    SettingCategoryHeader(stringResource(R.string.component_configs_title))
                }
                
                val components = listOf(
                    LiquidGlassComponent.DOCK to R.string.component_dock,
                    LiquidGlassComponent.FOLDER to R.string.component_folder,
                    LiquidGlassComponent.SEARCH to R.string.component_search,
                    LiquidGlassComponent.WIDGET to R.string.component_widget,
                    LiquidGlassComponent.BUTTON to R.string.component_button,
                    LiquidGlassComponent.APP_ICON to R.string.component_icon
                )
                
                components.forEach { (comp, stringRes) ->
                    item {
                        val label = stringResource(stringRes)
                        ComponentConfigItem(label, comp, viewModel) {
                            componentToEdit = comp to label
                        }
                    }
                }

                item {
                    SettingCategoryHeader(stringResource(R.string.apply_to))
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_dock),
                        supportingText = stringResource(R.string.glass_dock_desc),
                        checked = isLiquidGlassDockEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassDockEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_folders_home),
                        supportingText = stringResource(R.string.glass_folders_home_desc),
                        checked = isLiquidGlassHomeFolderEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassHomeFolderEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_search_global),
                        supportingText = stringResource(R.string.glass_search_global_desc),
                        checked = isLiquidGlassGlobalSearchEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassGlobalSearchEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_widgets),
                        supportingText = stringResource(R.string.glass_widgets_desc),
                        checked = isLiquidGlassWidgetsEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassWidgetsEnabled(it) }
                    )
                }

                item {
                    SettingCategoryHeader(stringResource(R.string.minus_one_title))
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_minus_one_widget),
                        supportingText = stringResource(R.string.glass_minus_one_widget_desc),
                        checked = isLiquidGlassMinusOneWidgetEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassMinusOneWidgetEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_minus_one_search),
                        supportingText = stringResource(R.string.glass_minus_one_search_desc),
                        checked = isLiquidGlassMinusOneSearchEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassMinusOneSearchEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_minus_one_button),
                        supportingText = stringResource(R.string.glass_minus_one_button_desc),
                        checked = isLiquidGlassMinusOneButtonEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassMinusOneButtonEnabled(it) }
                    )
                }

                item {
                    SettingCategoryHeader(stringResource(R.string.app_library))
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_folders_library),
                        supportingText = stringResource(R.string.glass_folders_library_desc),
                        checked = isLiquidGlassAppLibraryFolderEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassAppLibraryFolderEnabled(it) }
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.glass_search_library),
                        supportingText = stringResource(R.string.glass_search_library_desc),
                        checked = isLiquidGlassAppLibrarySearchEnabled,
                        onCheckedChange = { viewModel.setLiquidGlassAppLibrarySearchEnabled(it) }
                    )
                }
            }
        }
    }

    if (showGlobalEditor) {
        LiquidGlassEditorDialog(
            title = stringResource(R.string.visual_effects),
            onDismiss = { showGlobalEditor = false },
            content = {
                GlobalEffectEditorContent(viewModel)
            }
        )
    }

    componentToEdit?.let { (comp, label) ->
        LiquidGlassEditorDialog(
            title = label,
            onDismiss = { componentToEdit = null },
            content = {
                ComponentEffectEditorContent(comp, viewModel)
            }
        )
    }
}

@Composable
fun ComponentConfigItem(
    label: String,
    component: LiquidGlassComponent,
    viewModel: MainViewModel,
    onEdit: () -> Unit
) {
    val configs by viewModel.liquidGlassComponentConfigs.collectAsState()
    val config = configs[component.key] ?: LiquidGlassColorConfig()
    
    // 判斷是否「真的有改過參數」
    val isCustomized = config.enabled || config.blur != null || config.refractionHeight != null || config.refractionAmount != null

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                if (isCustomized) stringResource(R.string.customized) else stringResource(R.string.follow_global),
                color = if (isCustomized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null)
            }
        },
        modifier = Modifier.clickable { onEdit() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassEditorDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.done))
                        }
                    }
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun GlobalEffectEditorContent(viewModel: MainViewModel) {
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    val colorAdjustmentEnabled by viewModel.liquidGlassColorAdjustmentEnabled.collectAsState()
    val hue by viewModel.liquidGlassHue.collectAsState()
    val saturation by viewModel.liquidGlassSaturation.collectAsState()
    val brightness by viewModel.liquidGlassBrightness.collectAsState()
    val alpha by viewModel.liquidGlassAlpha.collectAsState()

    EffectEditorBase(
        viewModel = viewModel,
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        colorAdjustmentEnabled = colorAdjustmentEnabled,
        hue = hue,
        saturation = saturation,
        brightness = brightness,
        alpha = alpha,
        onBlurChange = { viewModel.setLiquidGlassBlur(it) },
        onRefractionHeightChange = { viewModel.setLiquidGlassRefractionHeight(it) },
        onRefractionAmountChange = { viewModel.setLiquidGlassRefractionAmount(it) },
        onChromaticAberrationChange = { viewModel.setLiquidGlassChromaticAberration(it) },
        onAdjChange = { viewModel.setLiquidGlassColorAdjustmentEnabled(it) },
        onHueChange = { viewModel.setLiquidGlassHue(it) },
        onSatChange = { viewModel.setLiquidGlassSaturation(it) },
        onBriChange = { viewModel.setLiquidGlassBrightness(it) },
        onAlphaChange = { viewModel.setLiquidGlassAlpha(it) }
    )
}

@Composable
fun ComponentEffectEditorContent(component: LiquidGlassComponent, viewModel: MainViewModel) {
    val configs by viewModel.liquidGlassComponentConfigs.collectAsState()
    val config = configs[component.key] ?: LiquidGlassColorConfig()

    val gBlur by viewModel.liquidGlassBlur.collectAsState()
    val gReH by viewModel.liquidGlassRefractionHeight.collectAsState()
    val gReA by viewModel.liquidGlassRefractionAmount.collectAsState()
    val gChA by viewModel.liquidGlassChromaticAberration.collectAsState()

    EffectEditorBase(
        viewModel = viewModel,
        blurRadius = config.blur ?: gBlur,
        refractionHeight = config.refractionHeight ?: gReH,
        refractionAmount = config.refractionAmount ?: gReA,
        chromaticAberration = config.chromaticAberration ?: gChA,
        colorAdjustmentEnabled = config.enabled,
        hue = config.hue,
        saturation = config.saturation,
        brightness = config.brightness,
        alpha = config.alpha,
        component = component,
        onBlurChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(blur = it)) },
        onRefractionHeightChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(refractionHeight = it)) },
        onRefractionAmountChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(refractionAmount = it)) },
        onChromaticAberrationChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(chromaticAberration = it)) },
        onAdjChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(enabled = it)) },
        onHueChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(hue = it)) },
        onSatChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(saturation = it)) },
        onBriChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(brightness = it)) },
        onAlphaChange = { viewModel.setLiquidGlassComponentConfig(component, config.copy(alpha = it)) }
    )
}

@Composable
fun EffectEditorBase(
    viewModel: MainViewModel,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    colorAdjustmentEnabled: Boolean,
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float,
    component: LiquidGlassComponent? = null,
    onBlurChange: (Float) -> Unit,
    onRefractionHeightChange: (Float) -> Unit,
    onRefractionAmountChange: (Float) -> Unit,
    onChromaticAberrationChange: (Boolean) -> Unit,
    onAdjChange: (Boolean) -> Unit,
    onHueChange: (Float) -> Unit,
    onSatChange: (Float) -> Unit,
    onBriChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    val rawWallpaper by viewModel.rawWallpaper.collectAsState()
    val backdrop = rememberLayerBackdrop()
    var glassOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (rawWallpaper != null) {
                    Image(
                        bitmap = rawWallpaper!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                }

                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(glassOffset.x.roundToInt(), glassOffset.y.roundToInt()) }
                        .size(150.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                glassOffset += dragAmount
                            }
                        }
                        .liquidGlass(
                            enabled = true,
                            backdrop = backdrop,
                            cornerRadius = 32.dp,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration,
                            colorAdjustmentEnabled = colorAdjustmentEnabled,
                            hue = hue,
                            saturation = saturation,
                            brightness = brightness,
                            alpha = alpha,
                            component = component
                        )
                )
                Text(
                    stringResource(R.string.drag_to_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                )
            }
        }

        item { SettingCategoryHeader(stringResource(R.string.visual_effects)) }
        item {
            SettingSliderItem(
                label = stringResource(R.string.blur_radius_label, (blurRadius * 5).toInt()),
                value = (blurRadius * 5).coerceIn(0f, 100f),
                onValueChange = { onBlurChange(it / 5f) },
                onIncrement = { onBlurChange(((blurRadius * 5 + 1f).coerceAtMost(100f)) / 5f) },
                onDecrement = { onBlurChange(((blurRadius * 5 - 1f).coerceAtLeast(0f)) / 5f) }
            )
        }
        item {
            SettingSliderItem(
                label = stringResource(R.string.refraction_height_label, refractionHeight.toInt()),
                value = refractionHeight,
                onValueChange = onRefractionHeightChange,
                onIncrement = { onRefractionHeightChange((refractionHeight + 1f).coerceAtMost(100f)) },
                onDecrement = { onRefractionHeightChange((refractionHeight - 1f).coerceAtLeast(0f)) }
            )
        }
        item {
            SettingSliderItem(
                label = stringResource(R.string.refraction_amount_label, refractionAmount.toInt()),
                value = refractionAmount,
                onValueChange = onRefractionAmountChange,
                onIncrement = { onRefractionAmountChange((refractionAmount + 1f).coerceAtMost(100f)) },
                onDecrement = { onRefractionAmountChange((refractionAmount - 1f).coerceAtLeast(0f)) }
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.chromatic_aberration),
                checked = chromaticAberration,
                onCheckedChange = onChromaticAberrationChange
            )
        }

        item { SettingCategoryHeader(stringResource(R.string.liquid_glass_color_adjustment)) }
        
        item {
            SettingSwitchItem(
                title = if (component != null) stringResource(R.string.independent_adjustment) else stringResource(R.string.liquid_glass_color_adjustment),
                checked = colorAdjustmentEnabled,
                onCheckedChange = onAdjChange
            )
        }

        if (colorAdjustmentEnabled) {
            item {
                SettingSliderItem(
                    label = stringResource(R.string.hue, hue.toInt()),
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = onHueChange,
                    onIncrement = { onHueChange((hue + 5f).coerceAtMost(360f)) },
                    onDecrement = { onHueChange((hue - 5f).coerceAtLeast(0f)) }
                )
            }
            item {
                SettingSliderItem(
                    label = stringResource(R.string.saturation, (saturation * 100).toInt()),
                    value = saturation * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onSatChange(it / 100f) },
                    onIncrement = { onSatChange(((saturation * 100f + 5f).coerceAtMost(100f)) / 100f) },
                    onDecrement = { onSatChange(((saturation * 100f - 5f).coerceAtLeast(0f)) / 100f) }
                )
            }
            item {
                SettingSliderItem(
                    label = stringResource(R.string.brightness, (brightness * 100).toInt()),
                    value = brightness * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onBriChange(it / 100f) },
                    onIncrement = { onBriChange(((brightness * 100f + 5f).coerceAtMost(100f)) / 100f) },
                    onDecrement = { onBriChange(((brightness * 100f - 5f).coerceAtLeast(0f)) / 100f) }
                )
            }
            item {
                SettingSliderItem(
                    label = stringResource(R.string.alpha, (alpha * 100).toInt()),
                    value = alpha * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onAlphaChange(it / 100f) },
                    onIncrement = { onAlphaChange(((alpha * 100f + 5f).coerceAtMost(100f)) / 100f) },
                    onDecrement = { onAlphaChange(((alpha * 100f - 5f).coerceAtLeast(0f)) / 100f) }
                )
            }
        }
    }
}
