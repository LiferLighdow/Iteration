package com.liferlighdow.iteration.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.utils.IconPackInfo
import com.liferlighdow.iteration.utils.IconProcessor
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import com.liferlighdow.iteration.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconThemeScreen(onBack: () -> Unit, onNavigateToChangeIcon: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val isThemedIconsEnabled by viewModel.isThemedIconsEnabled.collectAsState()
    val currentStyle by viewModel.iconStyle.collectAsState()
    val currentShape by viewModel.iconShape.collectAsState()
    val currentIconPack by viewModel.iconPackPackage.collectAsState()
    val isDynamicCalendarEnabled by viewModel.isDynamicCalendarEnabled.collectAsState()
    val isDynamicClockEnabled by viewModel.isDynamicClockEnabled.collectAsState()
    
    var showIconPackPicker by remember { mutableStateOf(false) }
    var showStyleInfoDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val styles = listOf(
        IconStyle.STANDARD to stringResource(R.string.style_standard),
        IconStyle.BLACK to stringResource(R.string.style_black),
        IconStyle.WHITE to stringResource(R.string.style_white),
        IconStyle.GLASS to stringResource(R.string.style_glass),
        IconStyle.CUSTOM to stringResource(R.string.style_custom)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.icon_theme_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- Customization ---
            item {
                SettingsSection(title = stringResource(R.string.customization)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_change_icon)) },
                        supportingContent = { Text(stringResource(R.string.settings_change_icon_desc)) },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onNavigateToChangeIcon() }
                    )
                    
                    var expandedShape by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.change_icon_shape)) },
                        supportingContent = { Text(if (currentShape == IconShape.CIRCLE) stringResource(R.string.shape_circle) else stringResource(R.string.shape_default)) },
                        leadingContent = { Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { expandedShape = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = expandedShape, onDismissRequest = { expandedShape = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.shape_default)) },
                                        onClick = { viewModel.setIconShape(IconShape.DEFAULT); expandedShape = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.shape_circle)) },
                                        onClick = { viewModel.setIconShape(IconShape.CIRCLE); expandedShape = false }
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { expandedShape = true }
                    )
                }
            }

            // --- Icon Pack ---
            item {
                SettingsSection(title = stringResource(R.string.icon_pack)) {
                    val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
                        value = withContext(Dispatchers.IO) {
                            viewModel.getInstalledIconPacks()
                        }
                    }

                    val currentPackName = remember(currentIconPack, iconPacks) {
                        if (currentIconPack.isEmpty()) context.getString(R.string.shape_default)
                        else iconPacks.find { it.packageName == currentIconPack }?.label ?: context.getString(R.string.unknown)
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.current_pack)) },
                        supportingContent = { Text(currentPackName) },
                        leadingContent = { Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showIconPackPicker = true }
                    )

                    SettingSwitchItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.themed_icons_m3_title),
                        supportingText = stringResource(R.string.themed_icons_m3_desc),
                        checked = isThemedIconsEnabled,
                        onCheckedChange = { if (currentIconPack.isEmpty()) viewModel.setThemedIconsEnabled(it) }
                    )
                    
                    if (currentIconPack.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.icon_pack_disabled_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // --- Iteration Style ---
            item {
                SettingsSection(title = stringResource(R.string.iteration_style)) {
                    val excludedApps by viewModel.excludedThemedPackages.collectAsState()
                    var showExclusionPicker by remember { mutableStateOf(false) }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.iteration_style)) },
                        supportingContent = { Text("自定義圖示產生的視覺效果") },
                        leadingContent = { Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            IconButton(onClick = { showStyleInfoDialog = true }) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.custom_exclusions)) },
                        supportingContent = { Text(stringResource(R.string.custom_exclusions_desc, excludedApps.size)) },
                        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showExclusionPicker = true }
                    )
                    
                    if (showExclusionPicker) {
                        val allApps by viewModel.allApps.collectAsState()
                        MultiAppExclusionPickerDialog(
                            allApps = allApps,
                            excludedPackages = excludedApps,
                            viewModel = viewModel,
                            onDismiss = { showExclusionPicker = false },
                            onToggle = { viewModel.toggleExcludedThemedApp(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    styles.forEach { (style, label) ->
                        var showCustomPicker by remember { mutableStateOf(false) }
                        
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (style == IconStyle.CUSTOM && currentStyle == IconStyle.CUSTOM && currentIconPack.isEmpty()) {
                                        IconButton(onClick = { showCustomPicker = true }) {
                                            Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    RadioButton(
                                        enabled = currentIconPack.isEmpty(),
                                        selected = currentStyle == style && currentIconPack.isEmpty(),
                                        onClick = { viewModel.setIconStyle(style) }
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(enabled = currentIconPack.isEmpty()) { 
                                viewModel.setIconStyle(style) 
                                if (style == IconStyle.CUSTOM) showCustomPicker = true
                            }
                        )

                        if (showCustomPicker) {
                            CustomIconStylePickerDialog(
                                viewModel = viewModel,
                                onDismiss = { showCustomPicker = false }
                            )
                        }
                    }
                }
            }

            // --- Dynamic Icons ---
            item {
                SettingsSection(title = "動態圖示") {
                    SettingSwitchItem(
                        icon = Icons.Default.CalendarToday,
                        title = stringResource(R.string.dynamic_calendar),
                        supportingText = stringResource(R.string.dynamic_calendar_desc),
                        checked = isDynamicCalendarEnabled,
                        onCheckedChange = { viewModel.setDynamicCalendarEnabled(it) }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.dynamic_clock),
                        supportingText = stringResource(R.string.dynamic_clock_desc),
                        checked = isDynamicClockEnabled,
                        onCheckedChange = { viewModel.setDynamicClockEnabled(it) }
                    )
                }
            }

            // --- Maintenance ---
            item {
                SettingsSection(title = stringResource(R.string.maintenance)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.clear_icon_cache_title)) },
                        supportingContent = { Text(stringResource(R.string.clear_icon_cache_desc)) },
                        leadingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showClearCacheDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_icon_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_icon_cache_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearIconCache()
                        showClearCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showStyleInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStyleInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(stringResource(R.string.style_compatibility_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sdk = android.os.Build.VERSION.SDK_INT
                    Text(stringResource(R.string.android_version_precision), style = MaterialTheme.typography.bodyMedium)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    CompatibilityRow(stringResource(R.string.android_13_plus), stringResource(R.string.precision_perfect), stringResource(R.string.precision_perfect_desc), sdk >= 33)
                    CompatibilityRow(stringResource(R.string.android_12), stringResource(R.string.precision_high), stringResource(R.string.precision_high_desc), sdk == 31 || sdk == 32)
                    CompatibilityRow(stringResource(R.string.android_8_11), stringResource(R.string.precision_good), stringResource(R.string.precision_good_desc), sdk in 26..30)
                    CompatibilityRow(stringResource(R.string.android_6_7), stringResource(R.string.precision_basic), stringResource(R.string.precision_basic_desc), sdk in 23..25)
                    
                    Text(stringResource(R.string.iteration_presets_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleInfoDialog = false }) { Text(stringResource(R.string.got_it)) }
            }
        )
    }

    if (showIconPackPicker) {
        IconPackPickerDialog(
            onDismiss = { showIconPackPicker = false },
            onPackSelected = { pkg ->
                viewModel.setIconPack(pkg)
                showIconPackPicker = false
            }
        )
    }
}

@Composable
fun IconPackPickerDialog(
    onDismiss: () -> Unit, 
    onPackSelected: (String) -> Unit,
    onlyLines: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    // 優化點：異步獲取圖標包列表，並根據需求過濾「線條類」圖標包
    val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
        val all = withContext(Dispatchers.IO) {
            viewModel.getInstalledIconPacks()
        }
        value = if (onlyLines) {
            val keywords = listOf("line", "outline", "wire", "arcticon", "lawnicon", "snow", "mono", "glyph", "stencil", "white", "black", "thin", "border")
            all.filter { pack ->
                val lowerPkg = pack.packageName.lowercase()
                val lowerLabel = pack.label.lowercase()
                keywords.any { lowerPkg.contains(it) || lowerLabel.contains(it) }
            }
        } else {
            all
        }
    }
    val currentShape by viewModel.iconShape.collectAsState()
    val shape = if (currentShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (onlyLines) stringResource(R.string.custom_icon_pack_select) else stringResource(R.string.select_icon_pack),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (onlyLines) {
                    Text(
                        text = stringResource(R.string.icon_pack_lines_only_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text(if (onlyLines) stringResource(R.string.default_symbol) else stringResource(R.string.default_style_desc)) },
                            modifier = Modifier.clickable { onPackSelected("") }
                        )
                    }
                    if (onlyLines && iconPacks.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_compatible_icon_packs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    items(iconPacks) { pack ->
                        ListItem(
                            headlineContent = { Text(pack.label) },
                            leadingContent = {
                                Image(
                                    bitmap = pack.icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(shape)
                                )
                            },
                            modifier = Modifier.clickable { onPackSelected(pack.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconCropperDialog(uri: Uri, onDismiss: () -> Unit, onConfirm: (Bitmap) -> Unit) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inMutable = true }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                originalBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adjust_icon_position)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (originalBitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.crop_instruction), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }
                                .clip(RoundedCornerShape(44.dp))
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                        offset += pan
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.1f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.failed_to_load_image))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && originalBitmap != null,
                onClick = {
                    originalBitmap?.let { bitmap ->
                        val outputSize = 512
                        val cropped = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(cropped)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            isFilterBitmap = true
                            isDither = true
                        }
                        val matrix = Matrix()
                        val baseScale = containerWidthPx / Math.max(bitmap.width.toFloat(), bitmap.height.toFloat())
                        val totalScale = scale * baseScale * (outputSize / containerWidthPx)
                        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                        matrix.postScale(totalScale, totalScale)
                        val userOffsetX = offset.x * (outputSize / containerWidthPx)
                        val userOffsetY = offset.y * (outputSize / containerWidthPx)
                        matrix.postTranslate(outputSize / 2f + userOffsetX, outputSize / 2f + userOffsetY)
                        canvas.drawBitmap(bitmap, matrix, paint)
                        onConfirm(cropped)
                    }
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconStylePickerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val bgColor by viewModel.customIconBgColor.collectAsState()
    val fgColor by viewModel.customIconFgColor.collectAsState()
    val useOriginal by viewModel.customIconUseOriginal.collectAsState()
    val useOriginalBg by viewModel.customIconUseOriginalBg.collectAsState()
    val useDominantColor by viewModel.customIconUseDominantColor.collectAsState()
    val customIconPack by viewModel.customIconPackPackage.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    
    // 預覽用的虛擬 AppModel
    val context = LocalContext.current
    val previewIcon = remember { context.packageManager.getApplicationIcon(context.packageName) }
    
    // 優化點：將耗時的圖標處理移至後台線程，避免阻塞 UI 滑動
    val previewBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, 
        bgColor, fgColor, useOriginal, useOriginalBg, useDominantColor, iconShape, customIconPack
    ) {
        value = withContext(Dispatchers.Default) {
            val processor = IconProcessor(context)
            val density = context.resources.displayMetrics.density
            
            val sourceIcon = if (customIconPack.isNotEmpty()) {
                viewModel.iconPackManager.loadIconPack(customIconPack)
                viewModel.iconPackManager.getIcon(context.packageName, "") ?: previewIcon
            } else previewIcon

            processor.processIcon(
                icon = sourceIcon,
                isThemed = false,
                themeColors = null,
                style = IconStyle.CUSTOM,
                shape = iconShape,
                sizePx = (64 * density).toInt(),
                isIconPack = customIconPack.isNotEmpty() && sourceIcon != previewIcon,
                customBgColor = bgColor,
                customFgColor = fgColor,
                customUseOriginal = useOriginal,
                customUseOriginalBg = useOriginalBg,
                customUseDominantColor = useDominantColor,
                originalIcon = previewIcon
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.custom_style_title), modifier = Modifier.weight(1f))
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f))
                    )
                } else {
                    Box(modifier = Modifier.size(48.dp).background(Color.Gray.copy(alpha = 0.1f), if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)))
                }
            }
        },
        text = {
            LazyColumn {
                item {
                    Text(stringResource(R.string.background_settings), style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.use_original_bg), modifier = Modifier.weight(1f))
                        Switch(checked = useOriginalBg, onCheckedChange = { viewModel.setCustomIconUseOriginalBg(it) })
                    }
                    if (!useOriginalBg) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.use_dominant_bg_color), modifier = Modifier.weight(1f))
                            Switch(checked = useDominantColor, onCheckedChange = { viewModel.setCustomIconUseDominantColor(it) })
                        }
                        if (!useDominantColor) {
                            ColorPicker(
                                initialColor = bgColor,
                                onColorChanged = { viewModel.setCustomIconBgColor(it) }
                            )
                        } else {
                            Text(stringResource(R.string.dominant_bg_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        Text(stringResource(R.string.bg_disabled_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(stringResource(R.string.foreground_settings), style = MaterialTheme.typography.titleSmall, color = if (useOriginal) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.use_original_icon), modifier = Modifier.weight(1f))
                        Switch(checked = useOriginal, onCheckedChange = { viewModel.setCustomIconUseOriginal(it) })
                    }
                    if (!useOriginal) {
                        var showCustomIconPackPicker by remember { mutableStateOf(false) }
                        val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
                            value = withContext(Dispatchers.IO) {
                                viewModel.getInstalledIconPacks()
                            }
                        }
                        val customPackLabel = remember(customIconPack, iconPacks) {
                            if (customIconPack.isEmpty()) context.getString(R.string.default_symbol)
                            else iconPacks.find { it.packageName == customIconPack }?.label ?: customIconPack
                        }

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.icon_pack)) },
                            supportingContent = { Text(customPackLabel) },
                            leadingContent = { Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showCustomIconPackPicker = true }
                        )

                        if (showCustomIconPackPicker) {
                            IconPackPickerDialog(
                                onlyLines = true,
                                onDismiss = { showCustomIconPackPicker = false },
                                onPackSelected = { pkg ->
                                    viewModel.setCustomIconPackPackage(pkg)
                                    showCustomIconPackPicker = false
                                }
                            )
                        }

                        ColorPicker(
                            initialColor = fgColor,
                            onColorChanged = { viewModel.setCustomIconFgColor(it) }
                        )
                    } else {
                        Text(stringResource(R.string.fg_disabled_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}

@Composable
fun CompatibilityRow(version: String, level: String, desc: String, isCurrentDevice: Boolean = false) {
    val perfect = stringResource(R.string.precision_perfect)
    val high = stringResource(R.string.precision_high)
    val good = stringResource(R.string.precision_good)

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(version, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(90.dp))
            Surface(
                color = when(level) {
                    perfect, "Perfect" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    high, "High" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                    good, "Good" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    else -> Color.Gray.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = level, 
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when(level) {
                        perfect, "Perfect" -> Color(0xFF2E7D32)
                        high, "High" -> Color(0xFF1565C0)
                        good, "Good" -> Color(0xFFE65100)
                        else -> Color.Black
                    }
                )
            }
            
            if (isCurrentDevice) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        stringResource(R.string.your_device),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    
    // 與 IconProcessor 邏輯保持一致的 UI 形狀
    val shape = remember(iconShape) {
        if (iconShape == IconShape.CIRCLE) CircleShape 
        else RoundedCornerShape(10.dp) // 40dp * 0.238 ≈ 10dp
    }
    
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(allApps, searchQuery) {
        val base = allApps.filter { !it.isFrozen && !it.isPrivate }
        if (searchQuery.isEmpty()) base
        else base.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showBuiltinPicker by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_change_icon)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.uniqueId }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        leadingContent = {
                            val appIcon = viewModel.getIcon(app.uniqueId)
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(shape) // 列表中的 App 圖示
                                )
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.resetCustomIcon(app.packageName) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    selectedApp = app
                                    showSourceDialog = true
                                }) {
                                    Icon(Icons.Default.Image, contentDescription = stringResource(R.string.change_icon), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }
        }

        // --- 1. 選擇圖示來源對話框 ---
        if (showSourceDialog && selectedApp != null) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("選擇圖示來源") },
                text = { Text("您想要使用內建的設計圖示，還是從藝廊選取自己的圖片？") },
                confirmButton = {
                    Button(onClick = {
                        showSourceDialog = false
                        launcher.launch("image/*")
                    }) {
                        Text("藝廊選取")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSourceDialog = false
                        showBuiltinPicker = true
                    }) {
                        Text("內建圖示")
                    }
                }
            )
        }

        // --- 2. 內建圖示挑選器 ---
        if (showBuiltinPicker && selectedApp != null) {
            val builtinIcons = listOf(
                "ic_builtin_phone" to "電話",
                "ic_builtin_messages" to "訊息",
                "ic_builtin_browser" to "瀏覽器",
                "ic_builtin_contacts" to "聯絡人",
                "ic_builtin_camera" to "相機",
                "ic_builtin_settings" to "設定"
            )

            AlertDialog(
                onDismissRequest = { showBuiltinPicker = false },
                title = { Text("挑選內建圖示") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(builtinIcons) { (resName, label) ->
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            if (resId != 0) {
                                ListItem(
                                    headlineContent = { Text(label) },
                                    leadingContent = {
                                        // 關鍵修改：挑選清單中的圖示也要裁切且放大 (1.15f)
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = shape,
                                            color = Color.Transparent
                                        ) {
                                            Image(
                                                painter = androidx.compose.ui.res.painterResource(resId),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(scaleX = 1.15f, scaleY = 1.15f),
                                                contentScale = ContentScale.FillBounds
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
                                        drawable?.let {
                                            // 關鍵修改：模擬 IconProcessor 的放大裁切邏輯
                                            val size = 512
                                            val iconScale = 1.15f
                                            val scaledSize = (size * iconScale).toInt()
                                            val offset = (size - scaledSize) / 2f
                                            
                                            // 1. 取得放大後的原始圖
                                            val rawBitmap = it.toBitmap(scaledSize, scaledSize)
                                            val maskedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(maskedBitmap)
                                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                                            
                                            // 2. 將放大後的圖置中繪製 (這會造成邊緣溢出，正是我們要的)
                                            canvas.drawBitmap(rawBitmap, offset, offset, paint)
                                            
                                            // 3. 套用遮罩進行裁切
                                            val mask = viewModel.iconProcessor.getOrCreateMask(iconShape, size)
                                            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                                            canvas.drawBitmap(mask, 0f, 0f, paint)
                                            
                                            viewModel.setCustomIcon(selectedApp!!.packageName, maskedBitmap)
                                        }
                                        showBuiltinPicker = false
                                        selectedApp = null
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBuiltinPicker = false }) { Text("取消") }
                }
            )
        }

        if (pickedImageUri != null && selectedApp != null) {
            IconCropperDialog(
                uri = pickedImageUri!!,
                onDismiss = { pickedImageUri = null },
                onConfirm = { croppedBitmap ->
                    // 關鍵修改：將從藝廊選取的圖片也進行形狀裁切
                    val size = croppedBitmap.width
                    val maskedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(maskedBitmap)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    
                    canvas.drawBitmap(croppedBitmap, 0f, 0f, paint)
                    
                    val mask = viewModel.iconProcessor.getOrCreateMask(iconShape, size)
                    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                    canvas.drawBitmap(mask, 0f, 0f, paint)

                    viewModel.setCustomIcon(selectedApp!!.packageName, maskedBitmap)
                    pickedImageUri = null
                    selectedApp = null
                }
            )
        }
    }
}
