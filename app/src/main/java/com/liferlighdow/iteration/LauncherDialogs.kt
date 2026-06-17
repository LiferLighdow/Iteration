package com.liferlighdow.iteration

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun LauncherOverlays(
    viewModel: MainViewModel,
    // 狀態控制
    showDesktopMenu: Boolean,
    onDismissDesktopMenu: () -> Unit,
    showCreateFolderDialog: Boolean,
    onDismissCreateFolder: () -> Unit,
    showDeleteFolderConfirm: Boolean,
    onDismissDeleteFolder: () -> Unit,
    showWidgetPicker: Boolean,
    onDismissWidgetPicker: () -> Unit,
    showDockPicker: Int?,
    onDismissDockPicker: () -> Unit,
    appToEdit: AppModel?,
    onDismissAppEdit: () -> Unit,
    folderToOpenId: String?,
    onDismissFolder: () -> Unit,
    // 數據與環境
    currentPage: Int,
    pages: List<List<AppModel>>,
    allAppsFlat: List<AppModel>,
    isDefaultLauncher: Boolean,
    isEditMode: Boolean,
    iconShape: IconShape,
    backdrop: LayerBackdrop,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    isLiquidGlassEnabled: Boolean,
    isLiquidGlassHomeFolderEnabled: Boolean,
    // 回調動作
    onAddWidgetClick: (Int) -> Unit,
    onWallpaperClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val mContext = LocalContext.current
    val openFolder = remember(folderToOpenId, pages) {
        pages.flatten().find { it.uniqueId == folderToOpenId }
    }

    // 1. 桌面長按選單
    LauncherMenu(
        isVisible = showDesktopMenu,
        onDismiss = onDismissDesktopMenu,
        viewModel = viewModel,
        currentPageIdx = currentPage,
        isCurrentPageEmpty = pages.getOrNull(currentPage)?.isEmpty() ?: false,
        isMultiplePages = pages.size > 1,
        isDefaultLauncher = isDefaultLauncher,
        onAddWidgetClick = { onAddWidgetClick(currentPage) },
        onCreateFolderClick = { 
            onDismissDesktopMenu()
            onDismissCreateFolder() 
        },
        onWallpaperClick = onWallpaperClick,
        onSetDefaultClick = {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            mContext.startActivity(intent)
        },
        onSettingsClick = onSettingsClick
    )

    // 2. 建立資料夾對話框
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismissCreateFolder,
            title = { Text(stringResource(R.string.folder_create_title)) },
            text = { 
                OutlinedTextField(
                    value = folderName, 
                    onValueChange = { folderName = it }, 
                    label = { Text(stringResource(R.string.folder_name_hint)) }, 
                    singleLine = true
                ) 
            },
            confirmButton = { 
                Button(onClick = { 
                    viewModel.createFolder(currentPage, folderName)
                    onDismissCreateFolder() 
                }) { Text(stringResource(R.string.create)) } 
            },
            dismissButton = { 
                TextButton(onClick = onDismissCreateFolder) { Text(stringResource(R.string.cancel)) } 
            }
        )
    }

    // 3. 刪除資料夾確認
    if (showDeleteFolderConfirm && openFolder != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteFolder,
            title = { Text(stringResource(R.string.folder_delete_confirm_title)) },
            text = { Text(stringResource(R.string.folder_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(openFolder.uniqueId, keepIcons = true)
                    onDismissFolder()
                    onDismissDeleteFolder()
                }) { Text(stringResource(R.string.folder_delete_keep)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(openFolder.uniqueId, keepIcons = false)
                    onDismissFolder()
                    onDismissDeleteFolder()
                }) { Text(stringResource(R.string.folder_delete_discard)) }
            }
        )
    }

    // 4. Dock App 選擇器
    if (showDockPicker != null) {
        AppPickerDialog(
            allApps = allAppsFlat.filter { !it.isHidden },
            iconShape = iconShape,
            onDismiss = onDismissDockPicker,
            onAppSelected = { pkg: String -> 
                viewModel.updateDockApp(showDockPicker, pkg)
                onDismissDockPicker()
            }
        )
    }

    // 5. Widget 選擇器
    if (showWidgetPicker) {
        WidgetPickerDialog(
            onDismiss = onDismissWidgetPicker,
            onWidgetSelected = {
                viewModel.addWidget(it, currentPage)
                onDismissWidgetPicker()
            }
        )
    }

    // 6. 快速編輯 App
    if (appToEdit != null) {
        QuickEditDialog(
            app = appToEdit,
            viewModel = viewModel,
            iconShape = iconShape,
            onDismiss = onDismissAppEdit
        )
    }

    // 7. 資料夾展開層
    FolderOverlay(
        isVisible = openFolder != null,
        folder = openFolder,
        allAppsFlat = allAppsFlat,
        isEditMode = isEditMode,
        viewModel = viewModel,
        backdrop = backdrop,
        iconShape = iconShape,
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        isLiquidGlassEnabled = isLiquidGlassEnabled,
        isLiquidGlassHomeFolderEnabled = isLiquidGlassHomeFolderEnabled,
        onAppClick = onAppClick,
        onDismiss = onDismissFolder,
        onDeleteFolderClick = { onDismissDeleteFolder() },
        onEditApp = { onDismissAppEdit() } // 或者傳遞回調
    )
}

@Composable
fun AppPickerDialog(
    allApps: List<AppModel>,
    iconShape: IconShape = IconShape.DEFAULT,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_app),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(8.dp)
                )
                
                var query by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val filtered = remember(query, allApps) {
                    allApps.filter { it.label.contains(query, ignoreCase = true) }
                        .sortedBy { it.label.lowercase() }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                app.processedIcon?.let {
                                    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
                                    Image(bitmap = it, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape))
                                }
                            },
                            modifier = Modifier.clickable { onAppSelected(app.packageName) }
                        )
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun QuickEditDialog(
    app: AppModel,
    viewModel: MainViewModel,
    iconShape: IconShape,
    onDismiss: () -> Unit
) {
    var newLabel by remember { mutableStateOf(app.label) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text(stringResource(R.string.folder_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                val favoritePackages by viewModel.favoritePackages.collectAsState()
                val isFavorite = favoritePackages.contains(app.packageName)
                
                ListItem(
                    headlineContent = { Text(stringResource(if (isFavorite) R.string.menu_remove_favorite else R.string.menu_add_favorite)) },
                    leadingContent = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline, null) },
                    modifier = Modifier.clickable { 
                        viewModel.toggleFavoriteApp(app.packageName)
                    }
                )

                ListItem(
                    headlineContent = { Text(if (app.isHidden) "Unhide" else "Hide") },
                    leadingContent = { Icon(if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) },
                    modifier = Modifier.clickable { 
                        viewModel.toggleHiddenApp(app.packageName)
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setCustomLabel(app.packageName, newLabel)
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun MultiAppPickerDialog(
    allApps: List<AppModel>,
    iconShape: IconShape = IconShape.DEFAULT,
    initialSelectedPackages: List<String>,
    onDismiss: () -> Unit,
    onAppsSelected: (List<String>) -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelectedPackages) } }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Manage Apps", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(8.dp))
                
                var query by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val filtered = remember(query, allApps) {
                    allApps.filter { it.label.contains(query, ignoreCase = true) }
                        .sortedBy { it.label.lowercase() }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                app.processedIcon?.let {
                                    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
                                    Image(bitmap = it, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape))
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = selected.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        if (checked) selected.add(app.packageName)
                                        else selected.remove(app.packageName)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (selected.contains(app.packageName)) selected.remove(app.packageName)
                                else selected.add(app.packageName)
                            }
                        )
                    }
                }
                
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onAppsSelected(selected.toList()) }) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}
