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
                },
                actions = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.customization),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_change_icon)) },
                    supportingContent = { Text(stringResource(R.string.settings_change_icon_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToChangeIcon() }
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.change_icon_shape)) },
                    supportingContent = { Text(if (currentShape == IconShape.CIRCLE) stringResource(R.string.shape_circle) else stringResource(R.string.shape_default)) },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.shape_default)) },
                                    onClick = { viewModel.setIconShape(IconShape.DEFAULT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.shape_circle)) },
                                    onClick = { viewModel.setIconShape(IconShape.CIRCLE); expanded = false }
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { expanded = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.icon_pack),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                // 優化點：將 PackageManager 的查詢移至 IO 線程，避免阻塞 UI
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
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { showIconPackPicker = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.themed_icons_m3_title)) },
                    supportingContent = { Text(stringResource(R.string.themed_icons_m3_desc)) },
                    trailingContent = {
                        Switch(
                            enabled = currentIconPack.isEmpty(),
                            checked = isThemedIconsEnabled,
                            onCheckedChange = { viewModel.setThemedIconsEnabled(it) }
                        )
                    },
                    modifier = Modifier.clickable(enabled = currentIconPack.isEmpty()) { 
                        viewModel.setThemedIconsEnabled(!isThemedIconsEnabled) 
                    }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.iteration_style),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showStyleInfoDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = stringResource(R.string.compatibility_info),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                var showExclusionPicker by remember { mutableStateOf(false) }
                val excludedApps by viewModel.excludedThemedPackages.collectAsState()
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.custom_exclusions)) },
                    supportingContent = { Text(stringResource(R.string.custom_exclusions_desc, excludedApps.size)) },
                    trailingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
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
            }

            items(styles) { (style, label) ->
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

            if (currentIconPack.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.icon_pack_disabled_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.maintenance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_icon_cache_title)) },
                    supportingContent = { Text(stringResource(R.string.clear_icon_cache_desc)) },
                    trailingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { showClearCacheDialog = true }
                )
            }
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
fun IconPackPickerDialog(onDismiss: () -> Unit, onPackSelected: (String) -> Unit) {
    val viewModel: MainViewModel = viewModel()
    // 優化點：異步獲取圖標包列表
    val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            viewModel.getInstalledIconPacks()
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
                Text(stringResource(R.string.select_icon_pack), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.default_style_desc)) },
                            modifier = Modifier.clickable { onPackSelected("") }
                        )
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
    val iconShape by viewModel.iconShape.collectAsState()
    
    // 預覽用的虛擬 AppModel
    val context = LocalContext.current
    val previewIcon = remember { context.packageManager.getApplicationIcon(context.packageName) }
    
    // 優化點：將耗時的圖標處理移至後台線程，避免阻塞 UI 滑動
    val previewBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, 
        bgColor, fgColor, useOriginal, useOriginalBg, iconShape
    ) {
        value = withContext(Dispatchers.Default) {
            val processor = IconProcessor(context)
            val density = context.resources.displayMetrics.density
            processor.processIcon(
                icon = previewIcon,
                isThemed = false,
                themeColors = null,
                style = IconStyle.CUSTOM,
                shape = iconShape,
                sizePx = (64 * density).toInt(),
                customBgColor = bgColor,
                customFgColor = fgColor,
                customUseOriginal = useOriginal,
                customUseOriginalBg = useOriginalBg
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
                        ColorPicker(
                            initialColor = bgColor,
                            onColorChanged = { viewModel.setCustomIconBgColor(it) }
                        )
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
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(allApps, searchQuery) {
        val base = allApps.filter { !it.isFrozen && !it.isPrivate }
        if (searchQuery.isEmpty()) base
        else base.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    
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
                },
                actions = {}
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
                                        .clip(shape)
                                        .border(
                                            width = 0.5.dp,
                                            color = Color.Black.copy(alpha = 0.1f),
                                            shape = shape
                                        )
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
                                    launcher.launch("image/*")
                                }) {
                                    Icon(Icons.Default.Image, contentDescription = stringResource(R.string.change_icon), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }
        }

        if (pickedImageUri != null && selectedApp != null) {
            IconCropperDialog(
                uri = pickedImageUri!!,
                onDismiss = { pickedImageUri = null },
                onConfirm = { croppedBitmap ->
                    viewModel.setCustomIcon(selectedApp!!.packageName, croppedBitmap)
                    pickedImageUri = null
                    selectedApp = null
                }
            )
        }
    }
}
