package com.liferlighdow.iteration.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.viewmodel.addAppToHome
import com.liferlighdow.iteration.data.AppModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLibraryPage(
    allApps: List<AppModel>,
    isLiquidGlass: Boolean = false,
    isSearchLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
    iconShape: IconShape = IconShape.DEFAULT,
    libraryShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    onAppClick: (String) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isSearchFocused by viewModel.isLibrarySearchFocused.collectAsState()
    val filteredApps by viewModel.filteredLibraryApps.collectAsState()
    
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val mContext = LocalContext.current

    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

    // 用於重新命名的狀態
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var newLabelText by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty() || (selectedCategory != null && selectedCategory != "All")) {
        if (selectedCategory != null && selectedCategory != "All") viewModel.setSelectedCategory("All")
        else { viewModel.setSearchQuery(""); focusManager.clearFocus(); viewModel.setLibrarySearchFocused(false) }
    }

    // 分類目錄邏輯：僅用於顯示「資料夾」按鈕，不涉及過濾
    val normalApps = remember(allApps) { allApps.filter { !it.isHidden } }
    val hiddenApps = remember(allApps) { allApps.filter { it.isHidden } }
    val categories = remember(normalApps, userCategories) {
        val grouped = normalApps.groupBy { it.displayCategory }
        val result = mutableListOf<Pair<String, List<AppModel>>>()
        userCategories.forEach { name -> grouped[name]?.let { result.add(name to it) } }
        val handledNames = userCategories.toSet()
        grouped.forEach { (name, apps) -> if (!handledNames.contains(name)) result.add(name to apps) }
        result
    }
    
    val hiddenAppsCount = remember(allApps) { allApps.count { it.isHidden } }
    val showHiddenFolder = hiddenAppsCount > 0 && searchQuery.isBlank() && (selectedCategory == null || selectedCategory == "All")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 搜尋欄：即使在分類中也顯示，但標題不同
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectedCategory != null && selectedCategory != "All") {
                IconButton(onClick = { viewModel.setSelectedCategory("All") }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { viewModel.setLibrarySearchFocused(it.isFocused) }
                    .liquidGlass(
                        enabled = isSearchLiquidGlass,
                        backdrop = backdrop,
                        cornerRadius = 28.dp,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration
                    ),
                placeholder = {
                    Text(
                        if (selectedCategory != null && selectedCategory != "All") "Search in $selectedCategory" else stringResource(
                            R.string.library_hint),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        }
                        if (isHiddenUnlocked && (selectedCategory == null || selectedCategory == "All")) {
                            IconButton(onClick = { isHiddenUnlocked = false }) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock_hidden_apps), tint = Color.White)
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            if (isSearchFocused || searchQuery.isNotEmpty()) {
                TextButton(onClick = { 
                    viewModel.setSearchQuery("")
                    focusManager.clearFocus()
                    viewModel.setLibrarySearchFocused(false)
                }) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        }

        val isLibraryView = isSearchFocused || searchQuery.isNotEmpty() || (selectedCategory != null && selectedCategory != "All")

        AnimatedContent(
            targetState = isLibraryView,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(fadeOut())
                } else {
                    fadeIn().togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "LibraryTransition"
        ) { targetIsLibraryView ->
            if (targetIsLibraryView) {
                val appsToShow = if (selectedCategory == "Hidden Apps" && !isHiddenUnlocked) emptyList() else filteredApps
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.setSearchQuery("")
                            focusManager.clearFocus()
                            viewModel.setLibrarySearchFocused(false)
                        }
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(appsToShow, key = { it.uniqueId }) { app ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ListItem(
                                    headlineContent = { Text(app.label, color = Color.White) },
                                    leadingContent = {
                                        val appIcon = viewModel.getIcon(app.packageName)
                                        if (appIcon != null) {
                                            val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(40.dp * 0.238f)
                                            Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAppClick(app.packageName) },
                                        onLongClick = { showMenu = true }
                                    )
                                )
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_add_to_home)) }, leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.addAppToHome(app.packageName); showMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { appToRename = app; newLabelText = app.label; showMenu = false })
                                    DropdownMenuItem(text = { Text(if (app.isHidden) "Unhide" else "Hide") }, leadingIcon = { Icon(if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.toggleHiddenApp(app.packageName); showMenu = false })
                                    DropdownMenuItem(
                                        text = { Text("App Info") },
                                        leadingIcon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${app.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            mContext.startActivity(intent)
                                            showMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_uninstall)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                                    data = Uri.fromParts("package", app.packageName, null)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                mContext.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("Iteration", "Uninstall failed", e)
                                            }
                                            showMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(start = 64.dp).align(Alignment.BottomCenter), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            } else {
                val haptic = LocalHapticFeedback.current
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    val folderList = mutableListOf<Pair<String, List<AppModel>>>()
                    if (suggestedApps.isNotEmpty()) folderList.add("Suggestions" to suggestedApps.take(4))
                    folderList.addAll(categories)
                    if (showHiddenFolder) folderList.add("Hidden Apps" to hiddenApps)
                    
                    // 優化點：加入穩定的 key，提升列表重組效能
                    items(folderList, key = { it.first }) { (name, apps) ->
                        AppLibraryFolder(
                            name = name,
                            apps = apps,
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            iconShape = iconShape,
                            libraryShape = libraryShape,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration,
                            isLocked = name == "Hidden Apps" && !isHiddenUnlocked,
                            onAppClick = { 
                                if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true 
                                else onAppClick(it) 
                            },
                            onMoreClick = { 
                                if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true 
                                else viewModel.setSelectedCategory(name) 
                            },
                            onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showPasswordDialog = false }, title = { Text(stringResource(
            R.string.hidden_apps_title)) }, text = { OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text(stringResource(
            R.string.password_hint)) }, singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff; IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, contentDescription = null) } }) }, confirmButton = { Button(onClick = { if (passwordInput == viewModel.getPassword()) { isHiddenUnlocked = true; showPasswordDialog = false } }) { Text(stringResource(
            R.string.unlock)) } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLibraryFolder(
    name: String,
    apps: List<AppModel>,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
    iconShape: IconShape = IconShape.DEFAULT,
    libraryShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    onAppClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    
    // 根據形狀決定裁切方式：只有圓形才強制裁切（防止脫框），Default 則不裁切（確保 72dp 圖示角角完整）
    val folderShape = if (libraryShape == IconShape.CIRCLE) CircleShape else null
    val folderPadding = if (libraryShape == IconShape.CIRCLE) 20.dp else 12.dp
    val internalIconSize = 72.dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .then(if (folderShape != null) Modifier.clip(folderShape) else Modifier)
        ) {
            // 背景層
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidGlass(
                        enabled = isLiquidGlass,
                        backdrop = backdrop,
                        cornerRadius = if (libraryShape == IconShape.CIRCLE) 80.dp else 24.dp,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration
                    )
            )
            
            // 內容層
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(folderPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val lockColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (isLocked) Box(modifier = Modifier.size(internalIconSize).background(lockColor, lockShape).clickable { onMoreClick() })
                        else apps.getOrNull(0)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val lockColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (isLocked) Box(modifier = Modifier.size(internalIconSize).background(lockColor, lockShape).clickable { onMoreClick() })
                        else apps.getOrNull(1)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val lockColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (isLocked) Box(modifier = Modifier.size(internalIconSize).background(lockColor, lockShape).clickable { onMoreClick() })
                        else apps.getOrNull(2)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val lockColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (isLocked) Box(modifier = Modifier.size(internalIconSize).background(lockColor, lockShape).clickable { onMoreClick() })
                        else if (apps.size > 4) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f), lockShape).clickable { onMoreClick() }, contentAlignment = Alignment.Center) { Text("+${apps.size - 3}", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        else apps.getOrNull(3)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                }
            }
        }
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryItemWithMenu(
    app: AppModel,
    folderName: String,
    iconShape: IconShape = IconShape.DEFAULT,
    iconSize: Dp = 72.dp,
    onAppClick: (String) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val mContext = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    // 用於重新命名的狀態（這裡稍微簡化，實際可能需要傳回 AppLibraryPage 處理更優雅，但我們先做基本功能）
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(app.label) }
    
    val shortcuts = remember(showMenu) { if (showMenu) viewModel.getShortcuts(app.packageName) else emptyList() }

    Box {
        AppItem(
            app = app,
            showLabel = false,
            iconSize = iconSize,
            iconShape = iconShape,
            getIcon = { pkg -> viewModel.getIcon(pkg) },
            modifier = Modifier.combinedClickable(
                onClick = { onAppClick(app.packageName) },
                onLongClick = {
                    if (folderName != "Hidden Apps") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                }
            )
        )

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    DropdownMenuItem(
                        text = { Text(shortcut.label) },
                        leadingIcon = {
                            shortcut.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            viewModel.launchShortcut(app.packageName, shortcut.id)
                            showMenu = false
                        }
                    )
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_add_to_home)) },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                onClick = { viewModel.addAppToHome(app.packageName); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { showRenameDialog = true; showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Hide") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { viewModel.toggleHiddenApp(app.packageName); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("App Info") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    mContext.startActivity(intent)
                    showMenu = false
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_uninstall)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = {
                    Log.d("Iteration", "Uninstalling: ${app.packageName}")
                    Toast.makeText(mContext, "Uninstalling ${app.label}...", Toast.LENGTH_SHORT).show()
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        mContext.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("Iteration", "Uninstall failed", e)
                    }
                    showMenu = false
                }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename App") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCustomLabel(app.packageName, renameText)
                    showRenameDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
