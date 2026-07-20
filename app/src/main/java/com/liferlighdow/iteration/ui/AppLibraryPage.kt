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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.viewmodel.addAppToHome
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.utils.ActionMode

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
    horizontalPadding: Dp = 16.dp,
    iconSize: Dp = 62.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onAppClick: (AppModel) -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isSearchFocused by viewModel.isLibrarySearchFocused.collectAsState()
    val filteredApps by viewModel.filteredLibraryApps.collectAsState()
    val isPrivateSpaceLocked by viewModel.isPrivateSpaceLocked.collectAsState()
    val menuOptions by viewModel.homeMenuOptions.collectAsState()
    
    var isHiddenUnlocked by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val mContext = LocalContext.current

    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

    // 用於重新命名的狀態
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var newLabelText by remember { mutableStateOf("") }
    var appToUnfreeze by remember { mutableStateOf<AppModel?>(null) }

    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty() || (selectedCategory != null && selectedCategory != "All")) {
        if (selectedCategory != null && selectedCategory != "All") viewModel.setSelectedCategory("All")
        else { viewModel.setSearchQuery(""); focusManager.clearFocus(); viewModel.setLibrarySearchFocused(false) }
    }

    // 分類目錄邏輯
    val normalApps = remember(allApps) { allApps.filter { !it.isHidden && !it.isFrozen && !it.isPrivate } }
    val hiddenApps = remember(allApps) { allApps.filter { it.isHidden && !it.isFrozen && !it.isPrivate } }
    val categories = remember(normalApps, userCategories) {
        val grouped = normalApps.groupBy { it.displayCategory }
        val result = mutableListOf<Pair<String, List<AppModel>>>()
        userCategories.forEach { name -> grouped[name]?.let { result.add(name to it) } }
        val handledNames = userCategories.toSet()
        grouped.forEach { (name, apps) -> if (!handledNames.contains(name)) result.add(name to apps) }
        result
    }
    
    val hiddenAppsCount = remember(allApps) { allApps.count { it.isHidden && !it.isFrozen && !it.isPrivate } }
    val showHiddenFolder = hiddenAppsCount > 0 && searchQuery.isBlank() && (selectedCategory == null || selectedCategory == "All")
    
    val hasPrivateProfile = remember(allApps) { allApps.any { it.isPrivate } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
        // 搜尋欄：縮減垂直 padding 以平衡視覺
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                val appsToShow = if ((selectedCategory == "Hidden Apps" && !isHiddenUnlocked) || (selectedCategory == "Private" && isPrivateSpaceLocked)) emptyList() else filteredApps
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    if (selectedCategory == "Private" && isPrivateSpaceLocked) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clickable {
                                    // 找到任意一個私密應用的 userId 來解鎖
                                    val privateUser = allApps.find { it.isPrivate }?.userId
                                    if (privateUser != null) viewModel.unlockPrivateSpace(privateUser)
                                },
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.private_space_locked), color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.private_space_locked_desc), color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(appsToShow, key = { it.uniqueId }) { app ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ListItem(
                                    headlineContent = { Text(app.label, color = Color.White) },
                                    leadingContent = {
                                        val appIcon = viewModel.getIcon(app.uniqueId)
                                        if (appIcon != null) {
                                            val listIconSize = 40.dp
                                            val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(listIconSize * 0.238f)
                                            val colorFilter = remember(app.isFrozen) {
                                                if (app.isFrozen) {
                                                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
                                                } else null
                                            }
                                            Image(
                                                bitmap = appIcon,
                                                contentDescription = null,
                                                colorFilter = colorFilter,
                                                modifier = Modifier
                                                    .size(listIconSize)
                                                    .clip(shape)
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = Color.White.copy(alpha = 0.3f),
                                                        shape = shape
                                                    )
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.combinedClickable(
                                        onClick = { 
                                            if (app.isFrozen) appToUnfreeze = app
                                            else onAppClick(app) 
                                        },
                                        onLongClick = { showMenu = true }
                                    )
                                )
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    val actionMode by viewModel.actionMode.collectAsState()
                                    if (menuOptions.contains("freeze") && (actionMode == ActionMode.SHIZUKU || actionMode == ActionMode.ROOT)) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (app.isFrozen) R.string.unfreeze else R.string.freeze)) },
                                            leadingIcon = { Icon(Icons.Default.AcUnit, null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = { viewModel.toggleFreezeApp(app, mContext); showMenu = false }
                                        )
                                        HorizontalDivider()
                                    }
                                    if (!app.isPrivate) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_add_to_home)) }, leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.addAppToHome(app.uniqueId); showMenu = false })
                                    }
                                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { appToRename = app; newLabelText = app.label; showMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(if (app.isHidden) R.string.unhide else R.string.hide)) }, leadingIcon = { Icon(if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.toggleHiddenApp(app.uniqueId); showMenu = false })
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_app_info)) },
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
                                    if (!app.isSystem) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_uninstall)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                if (app.isPWA) {
                                                    viewModel.deletePWA(app)
                                                    showMenu = false
                                                    return@DropdownMenuItem
                                                }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            isLocked = (name == "Hidden Apps" && !isHiddenUnlocked),
                            onAppClick = { 
                                if (name == "Hidden Apps" && !isHiddenUnlocked) showPasswordDialog = true 
                                else if (it.isFrozen) appToUnfreeze = it
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

    if (appToUnfreeze != null) {
        AlertDialog(
            onDismissRequest = { appToUnfreeze = null },
            title = { Text(stringResource(R.string.unfreeze_dialog_title)) },
            text = { Text(stringResource(R.string.unfreeze_dialog_msg)) },
            confirmButton = {
                Button(onClick = {
                    appToUnfreeze?.let { viewModel.toggleFreezeApp(it, mContext) }
                    appToUnfreeze = null
                }) { Text(stringResource(R.string.unfreeze)) }
            },
            dismissButton = {
                TextButton(onClick = { appToUnfreeze = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
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
    iconSize: Dp = 72.dp,
    onAppClick: (AppModel) -> Unit,
    onMoreClick: () -> Unit,
    onDragStart: (AppModel, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    
    // 根據形狀決定裁切方式：只有圓形才強制裁切（防止脫框），Default 則不裁切（確保圖示角角完整）
    val folderShape = if (libraryShape == IconShape.CIRCLE) CircleShape else null
    val folderPadding = if (libraryShape == IconShape.CIRCLE) (iconSize * 0.27f) else (iconSize * 0.16f)
    val internalIconSize = iconSize // 直接使用傳入的大小，或者是比例換算
    
    val showLock = isLocked || (name == "Private" && apps.isEmpty())

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
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val lockColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (showLock) {
                            Box(
                                modifier = Modifier
                                    .size(internalIconSize)
                                    .background(lockColor, lockShape)
                                    .clickable { onMoreClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(internalIconSize * 0.5f))
                            }
                        }
                        else apps.getOrNull(0)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val lockColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (showLock) {
                            Box(
                                modifier = Modifier
                                    .size(internalIconSize)
                                    .background(lockColor, lockShape)
                                    .clickable { onMoreClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(internalIconSize * 0.5f))
                            }
                        }
                        else apps.getOrNull(1)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val lockColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (showLock) {
                            Box(
                                modifier = Modifier
                                    .size(internalIconSize)
                                    .background(lockColor, lockShape)
                                    .clickable { onMoreClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(internalIconSize * 0.5f))
                            }
                        }
                        else apps.getOrNull(2)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val lockShape = if (libraryShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(15.1.dp)
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val lockColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        if (showLock) {
                            Box(
                                modifier = Modifier
                                    .size(internalIconSize)
                                    .background(lockColor, lockShape)
                                    .clickable { onMoreClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(internalIconSize * 0.5f))
                            }
                        }
                        else if (apps.size > 4) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f), lockShape).clickable { onMoreClick() }, contentAlignment = Alignment.Center) { Text(stringResource(R.string.plus_more, apps.size - 3), color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        else apps.getOrNull(3)?.let { app ->
                            LibraryItemWithMenu(app, name, iconShape, internalIconSize, onAppClick)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (name == "Private" && !isLocked) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = stringResource(R.string.lock_private_space),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable {
                            val privateUser = apps.find { it.isPrivate }?.userId ?: viewModel.allApps.value.find { it.isPrivate }?.userId
                            if (privateUser != null) viewModel.lockPrivateSpace(privateUser)
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryItemWithMenu(
    app: AppModel,
    folderName: String,
    iconShape: IconShape = IconShape.DEFAULT,
    iconSize: Dp = 72.dp,
    onAppClick: (AppModel) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val mContext = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    // 用於重新命名的狀態
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(app.label) }
    
    val menuOptions by viewModel.homeMenuOptions.collectAsState()
    
    val shortcuts = remember(showMenu) {
        if (showMenu && menuOptions.contains("shortcuts") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            viewModel.getAppShortcuts(app.packageName, app.userId)
        } else emptyList()
    }
    
    Box {
        AppItem(
            app = app,
            showLabel = false,
            iconSize = iconSize,
            iconShape = iconShape,
            getIcon = { pkg -> viewModel.getIcon(pkg) },
            modifier = Modifier.combinedClickable(
                onClick = { onAppClick(app) },
                onLongClick = {
                    if (folderName != "Hidden Apps") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                }
            )
        )

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            val actionMode by viewModel.actionMode.collectAsState()
            if (menuOptions.contains("freeze") && (actionMode == ActionMode.SHIZUKU || actionMode == ActionMode.ROOT)) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (app.isFrozen) R.string.unfreeze else R.string.freeze)) },
                    leadingIcon = { Icon(Icons.Default.AcUnit, null) },
                    onClick = { viewModel.toggleFreezeApp(app, mContext); showMenu = false }
                )
                HorizontalDivider()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1 && shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    DropdownMenuItem(
                        text = { 
                            @Suppress("NewApi")
                            Text(shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "") 
                        },
                        leadingIcon = {
                            val icon = viewModel.getShortcutIcon(shortcut)
                            if (icon != null) {
                                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            @Suppress("NewApi")
                            viewModel.launchShortcut(app.packageName, shortcut.id, app.userId)
                            showMenu = false
                        }
                    )
                }
                HorizontalDivider()
            }

            if (!app.isPrivate) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_to_home)) },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    onClick = { viewModel.addAppToHome(app.uniqueId); showMenu = false }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { showRenameDialog = true; showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(if (app.isHidden) R.string.unhide else R.string.hide)) },
                leadingIcon = { Icon(if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) },
                onClick = { viewModel.toggleHiddenApp(app.packageName); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_app_info)) },
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
            if (!app.isSystem) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_uninstall)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        if (app.isPWA) {
                            viewModel.deletePWA(app)
                            showMenu = false
                            return@DropdownMenuItem
                        }
                        Log.d("Iteration", "Uninstalling: ${app.packageName}")
                        Toast.makeText(mContext, mContext.getString(R.string.uninstalling_app, app.label), Toast.LENGTH_SHORT).show()
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
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_app)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.new_label_hint)) },
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
