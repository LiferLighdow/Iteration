package com.liferlighdow.iteration.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.liquidGlass
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

    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    
    val rawWallpaper by viewModel.rawWallpaper.collectAsState()
    val backdrop = rememberLayerBackdrop()

    // 拖動狀態
    var glassOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
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
                        TextButton(onClick = {
                            viewModel.resetLiquidGlassParams()
                            glassOffset = androidx.compose.ui.geometry.Offset.Zero
                        }) {
                            Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                item {
                    // Preview Area (Workshop)
                    Text(
                        text = stringResource(R.string.liquid_glass_workshop_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background: Wallpaper
                        if (rawWallpaper != null) {
                            Image(
                                bitmap = rawWallpaper!!,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(backdrop)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                        }

                        // Liquid Glass Preview Box (Free Dragging)
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        glassOffset.x.roundToInt(),
                                        glassOffset.y.roundToInt()
                                    )
                                }
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
                                    chromaticAberration = chromaticAberration
                                )
                        )

                        // 指引文字
                        Text(
                            stringResource(R.string.drag_to_preview),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        )
                    }
                }

                item {
                    SettingCategoryHeader(stringResource(R.string.visual_effects))
                }

                item {
                    SettingSliderItem(
                        label = stringResource(R.string.blur_radius_label, (blurRadius * 5).toInt()),
                        value = (blurRadius * 5).coerceIn(0f, 100f),
                        onValueChange = { viewModel.setLiquidGlassBlur(it / 5f) },
                        onIncrement = { viewModel.setLiquidGlassBlur(((blurRadius * 5 + 1f).coerceAtMost(100f)) / 5f) },
                        onDecrement = { viewModel.setLiquidGlassBlur(((blurRadius * 5 - 1f).coerceAtLeast(0f)) / 5f) }
                    )
                }

                item {
                    SettingSliderItem(
                        label = stringResource(R.string.refraction_height_label, refractionHeight.toInt()),
                        value = refractionHeight,
                        onValueChange = { viewModel.setLiquidGlassRefractionHeight(it) },
                        onIncrement = { viewModel.setLiquidGlassRefractionHeight((refractionHeight + 1f).coerceAtMost(100f)) },
                        onDecrement = { viewModel.setLiquidGlassRefractionHeight((refractionHeight - 1f).coerceAtLeast(0f)) }
                    )
                }

                item {
                    SettingSliderItem(
                        label = stringResource(R.string.refraction_amount_label, refractionAmount.toInt()),
                        value = refractionAmount,
                        onValueChange = { viewModel.setLiquidGlassRefractionAmount(it) },
                        onIncrement = { viewModel.setLiquidGlassRefractionAmount((refractionAmount + 1f).coerceAtMost(100f)) },
                        onDecrement = { viewModel.setLiquidGlassRefractionAmount((refractionAmount - 1f).coerceAtLeast(0f)) }
                    )
                }

                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.chromatic_aberration),
                        checked = chromaticAberration,
                        onCheckedChange = { viewModel.setLiquidGlassChromaticAberration(it) }
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            } else {
                item {
                    // 對於 API < 31 的用戶，顯示一個簡單的說明卡片代替工作坊與參數調整
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

            if (isLiquidGlassEnabled) {
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
}
