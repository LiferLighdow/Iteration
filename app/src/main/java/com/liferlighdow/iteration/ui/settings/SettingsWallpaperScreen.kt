package com.liferlighdow.iteration.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WallpaperPreset
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.ui.dialogs.WallpaperCropDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWallpaperScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val presets by viewModel.wallpaperPresets.collectAsState()
    val currentPresetName by viewModel.currentWallpaperPresetName.collectAsState()
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenAspectRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedPresetForMenu by remember { mutableStateOf<WallpaperPreset?>(null) }
    var originalBitmapForCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showRenameDialog by remember { mutableStateOf<WallpaperPreset?>(null) }
    var showNameConflictWarning by remember { mutableStateOf<String?>(null) }
    
    data class EmojiSelectionData(val color: Int, val emojiText: String)
    var showEmojiModeSelection by remember { mutableStateOf<EmojiSelectionData?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.loadWallpaperPresets()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedUri = uri
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_wallpaper), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "CURRENT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(screenAspectRatio)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No wallpapers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { presets.size })
                
                val cardWidthDp = configuration.screenWidthDp.dp - 128.dp 
                val pagerHeight = cardWidthDp / screenAspectRatio

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pagerHeight),
                    contentPadding = PaddingValues(horizontal = 64.dp),
                    pageSpacing = 20.dp
                ) { page ->
                    val preset = presets[page]
                    WallpaperPresetCard(
                        preset = preset,
                        isSelected = preset.name == currentPresetName,
                        aspectRatio = screenAspectRatio,
                        onApply = { viewModel.applyWallpaperPreset(preset) },
                        onCustomize = { selectedPresetForMenu = preset }
                    )
                }
                
                Row(
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(presets.size) { index ->
                        val color = if (pagerState.currentPage == index) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showTypeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New Wallpaper", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTypeDialog) {
        val actionMode by viewModel.actionMode.collectAsState()
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text(stringResource(R.string.menu_wallpaper)) },
            text = {
                Column {
                    if (actionMode != com.liferlighdow.iteration.utils.ActionMode.ACCESSIBILITY) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.wallpaper_sync_system)) },
                            supportingContent = { Text(stringResource(R.string.wallpaper_sync_system_desc)) },
                            leadingContent = { Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                showTypeDialog = false
                                scope.launch(Dispatchers.IO) {
                                    val wm = android.app.WallpaperManager.getInstance(context)
                                    val drawable = wm.drawable
                                    if (drawable != null) {
                                        val bitmap = drawable.toBitmap()
                                        viewModel.addNewWallpaperPreset(bitmap, "System_${System.currentTimeMillis()}")
                                    }
                                }
                            }
                        )
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.widget_photo)) },
                        supportingContent = { Text(stringResource(R.string.select_wallpaper_type_desc)) },
                        leadingContent = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showTypeDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.style_custom)) },
                        supportingContent = { Text(stringResource(R.string.emoji_hint)) },
                        leadingContent = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showTypeDialog = false
                            showColorPicker = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showColorPicker) {
        var selectedColor by remember { mutableIntStateOf(0xFF2196F3.toInt()) }
        var emojiText by remember { mutableStateOf("") }
        var selectedStyle by remember { mutableStateOf(com.liferlighdow.iteration.data.EmojiPatternStyle.MEDIUM_GRID) }
        val favorites by viewModel.favoriteWallpaperColors.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.custom_style_title)) },
            text = {
                Column {
                    com.liferlighdow.iteration.ui.ColorPickerInternal(
                        initialColor = selectedColor,
                        onColorChanged = { selectedColor = it },
                        emojiText = emojiText,
                        onEmojiChanged = { emojiText = it },
                        favorites = favorites,
                        onAddFavorite = { viewModel.addFavoriteWallpaperColor(it) },
                        onRemoveFavorite = { viewModel.removeFavoriteWallpaperColor(it) }
                    )
                    
                    if (emojiText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Arrangement Style", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(com.liferlighdow.iteration.data.EmojiPatternStyle.entries.size) { index ->
                                val style = com.liferlighdow.iteration.data.EmojiPatternStyle.entries[index]
                                FilterChip(
                                    selected = selectedStyle == style,
                                    onClick = { selectedStyle = style },
                                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setEmojiPatternStyle(selectedStyle) 
                    if (emojiText.isBlank()) {
                        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(selectedColor)
                        }
                        viewModel.addNewWallpaperPreset(bitmap, "Color_${System.currentTimeMillis()}")
                        showColorPicker = false
                    } else {
                        showEmojiModeSelection = EmojiSelectionData(selectedColor, emojiText)
                        showColorPicker = false
                    }
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showEmojiModeSelection != null) {
        val data = showEmojiModeSelection!!
        val emojiPatternStyle by viewModel.emojiPatternStyle.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showEmojiModeSelection = null },
            title = { Text(stringResource(R.string.wallpaper_mode_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_lite)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_lite_desc)) },
                        leadingContent = { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            viewModel.addNewEmojiWallpaperPreset(
                                com.liferlighdow.iteration.data.EmojiRenderingMode.LITE,
                                data.color, data.emojiText, emojiPatternStyle, "EmojiLite_${System.currentTimeMillis()}"
                            )
                            showEmojiModeSelection = null
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_balance)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_balance_desc)) },
                        leadingContent = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            viewModel.addNewEmojiWallpaperPreset(
                                com.liferlighdow.iteration.data.EmojiRenderingMode.BALANCE,
                                data.color, data.emojiText, emojiPatternStyle, "EmojiBal_${System.currentTimeMillis()}"
                            )
                            showEmojiModeSelection = null
                        }
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.wallpaper_mode_full)) },
                        supportingContent = { Text(stringResource(R.string.wallpaper_mode_full_desc)) },
                        leadingContent = { Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            viewModel.addNewEmojiWallpaperPreset(
                                com.liferlighdow.iteration.data.EmojiRenderingMode.FULL,
                                data.color, data.emojiText, emojiPatternStyle, "EmojiFull_${System.currentTimeMillis()}"
                            )
                            showEmojiModeSelection = null
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (pickedUri != null) {
        WallpaperCropDialog(
            uri = pickedUri!!,
            onDismiss = { pickedUri = null },
            onConfirm = { cropped, original ->
                val name = "Theme_${System.currentTimeMillis()}"
                viewModel.addNewWallpaperPreset(cropped, name, original)
                pickedUri = null
            }
        )
    }

    if (originalBitmapForCrop != null && selectedPresetForMenu != null) {
        WallpaperCropDialog(
            bitmap = originalBitmapForCrop,
            onDismiss = { originalBitmapForCrop = null; selectedPresetForMenu = null },
            onConfirm = { cropped, _ ->
                viewModel.updateWallpaperPresetCrop(selectedPresetForMenu!!, cropped)
                originalBitmapForCrop = null
                selectedPresetForMenu = null
            }
        )
    }

    if (selectedPresetForMenu != null && originalBitmapForCrop == null) {
        AlertDialog(
            onDismissRequest = { selectedPresetForMenu = null },
            title = { Text(selectedPresetForMenu!!.name) },
            text = {
                Column {
                    if (selectedPresetForMenu!!.originalPath != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.adjust_position)) },
                            leadingContent = { Icon(Icons.Default.Crop, null) },
                            modifier = Modifier.clickable {
                                val path = selectedPresetForMenu!!.originalPath!!
                                scope.launch(Dispatchers.IO) {
                                    val bitmap = BitmapFactory.decodeFile(path)
                                    withContext(Dispatchers.Main) {
                                        originalBitmapForCrop = bitmap
                                    }
                                }
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.rename)) },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable {
                            showRenameDialog = selectedPresetForMenu
                            selectedPresetForMenu = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deleteWallpaperPreset(selectedPresetForMenu!!)
                            selectedPresetForMenu = null
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (showRenameDialog != null) {
        var newName by remember { mutableStateOf(showRenameDialog!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName != showRenameDialog!!.name && presets.any { it.name == newName }) {
                        showNameConflictWarning = newName
                    } else {
                        viewModel.renameWallpaperPreset(showRenameDialog!!, newName)
                        showRenameDialog = null
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showNameConflictWarning != null) {
        AlertDialog(
            onDismissRequest = { showNameConflictWarning = null },
            title = { Text("名稱衝突") },
            text = { Text("已經存在名為 \"${showNameConflictWarning}\" 的桌布預設。請選擇不同的名稱。") },
            confirmButton = {
                TextButton(onClick = { showNameConflictWarning = null }) { Text(stringResource(R.string.got_it)) }
            }
        )
    }
}

@Composable
fun WallpaperPresetCard(
    preset: WallpaperPreset,
    isSelected: Boolean,
    aspectRatio: Float,
    onApply: () -> Unit,
    onCustomize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onApply() }
    ) {
        if (preset.previewPath != null) {
            AsyncImage(
                model = preset.previewPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clickable { onCustomize() },
            color = Color.White.copy(alpha = 0.25f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Text(
                text = "Customize",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
