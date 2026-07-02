package com.liferlighdow.iteration.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.viewmodel.removeAppFromFolder
import com.liferlighdow.iteration.viewmodel.updateFolderApps
import com.liferlighdow.iteration.viewmodel.updateFolderName

@Composable
fun FolderOverlay(
    isVisible: Boolean,
    folder: AppModel?,
    allAppsFlat: List<AppModel>,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    backdrop: Backdrop?,
    iconShape: IconShape,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    isLiquidGlassEnabled: Boolean,
    isLiquidGlassHomeFolderEnabled: Boolean,
    onAppClick: (AppModel) -> Unit,
    onDismiss: () -> Unit,
    onDeleteFolderClick: () -> Unit,
    onEditApp: (AppModel) -> Unit
) {
    // 關鍵：記住最後一個非空的資料夾資料，確保在退出動畫期間不會因為 folder 變為 null 而閃退
    val lastNonNullFolder = remember { mutableStateOf<AppModel?>(null) }
    LaunchedEffect(folder) {
        if (folder != null) lastNonNullFolder.value = folder
    }

    AnimatedVisibility(
        visible = isVisible && folder != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val currentFolder = lastNonNullFolder.value ?: return@AnimatedVisibility
        val mContext = LocalContext.current
        var isEditingName by remember { mutableStateOf(false) }
        var tempName by remember(currentFolder.label) { mutableStateOf(currentFolder.label) }
        var showMoreMenu by remember { mutableStateOf(false) }
        var showAppPicker by remember { mutableStateOf(false) }

        // 使用自定義全螢幕 Overlay 取代 Dialog，以實現 iOS 感的縮放與模糊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    // Android 12 以下：如果沒有全域模糊，這裡手動加一點遮罩
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier
                    } else {
                        Modifier.background(Color.Black.copy(alpha = 0.2f))
                    }
                )
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // 內層容器動畫
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.2f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = scaleOut(targetScale = 0.2f) + fadeOut()
                    )
                    .clickable(enabled = false) { } // 阻止點擊穿透
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        viewModel.updateFolderName(currentFolder.uniqueId, tempName)
                                        isEditingName = false
                                    }) {
                                        Icon(Icons.Default.Check, null, tint = Color.White)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = currentFolder.label,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                modifier = Modifier.clickable { isEditingName = true },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = { isEditingName = true; showMoreMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_manage_apps)) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                                        onClick = { showAppPicker = true; showMoreMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.folder_delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { onDeleteFolderClick(); showMoreMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val itemsPerPage = 9
                    val folderPages = remember(currentFolder.folderItems) { currentFolder.folderItems.chunked(itemsPerPage) }
                    val folderPagerState = rememberPagerState { folderPages.size }

                    Box(
                        modifier = Modifier
                            .width(340.dp)
                            .height(375.dp)
                            .liquidGlass(
                                enabled = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                backdrop = backdrop,
                                cornerRadius = 32.dp,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        HorizontalPager(state = folderPagerState, modifier = Modifier.fillMaxSize()) { pageIdx ->
                            val pageItems = folderPages[pageIdx]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                pageItems.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        rowItems.forEach { app ->
                                            val lastPos = remember { object { var pos = Offset.Zero } }
                                            var showItemMenu by remember { mutableStateOf(false) }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .onGloballyPositioned { lastPos.pos = it.positionInRoot() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                                                val rotation by infiniteTransition.animateFloat(
                                                    initialValue = -2.5f, targetValue = 2.5f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(120, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "jiggle"
                                                )
                                                AppItem(
                                                    app = app,
                                                    onAppClick = null, // 設為 null，避免內部 clickable 攔截手勢
                                                    iconSize = 64.dp,
                                                    isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassHomeFolderEnabled,
                                                    backdrop = backdrop,
                                                    iconShape = iconShape,
                                                    blurRadius = blurRadius,
                                                    refractionHeight = refractionHeight,
                                                    refractionAmount = refractionAmount,
                                                    chromaticAberration = chromaticAberration,
                                                    isEditMode = isEditMode,
                                                    getIcon = { pkg -> viewModel.getIcon(pkg) },
                                                    onDeleteClick = {
                                                        viewModel.removeAppFromFolder(currentFolder.uniqueId, app.uniqueId)
                                                    },
                                                    modifier = Modifier
                                                        .graphicsLayer {
                                                            if (isEditMode) rotationZ = rotation
                                                        }
                                                        .pointerInput(app.uniqueId) {
                                                            detectTapGestures(
                                                                onLongPress = {
                                                                    if (!isEditMode) showItemMenu =
                                                                        true
                                                                },
                                                                onTap = {
                                                                    onAppClick(app)
                                                                    onDismiss()
                                                                }
                                                            )
                                                        }
                                                )
                                                val menuOptions by viewModel.homeMenuOptions.collectAsState()

                                                DropdownMenu(
                                                    expanded = showItemMenu,
                                                    onDismissRequest = { showItemMenu = false }
                                                ) {
                                                    if (menuOptions.contains("delete_home")) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.menu_delete_home)) },
                                                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                            onClick = {
                                                                viewModel.removeAppFromFolder(currentFolder.uniqueId, app.uniqueId)
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                    if (menuOptions.contains("edit")) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.menu_edit)) },
                                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                            onClick = {
                                                                onEditApp(app)
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                    if (menuOptions.contains("uninstall")) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.menu_uninstall)) },
                                                            leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
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
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                    if (menuOptions.contains("hide")) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.menu_hide)) },
                                                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                                                            onClick = {
                                                                viewModel.toggleHiddenApp(app.packageName)
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                    if (menuOptions.contains("app_info")) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.menu_app_info)) },
                                                            leadingIcon = { Icon(Icons.Default.Info, null) },
                                                            onClick = {
                                                                try {
                                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                        data = Uri.fromParts("package", app.packageName, null)
                                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    }
                                                                    mContext.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Log.e("Iteration", "Open App Info failed", e)
                                                                }
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                    if (menuOptions.contains("favorite")) {
                                                        val favoritePackages by viewModel.favoritePackages.collectAsState()
                                                        val isFavorite = favoritePackages.contains(app.packageName)
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(if (isFavorite) R.string.menu_remove_favorite else R.string.menu_add_favorite)) },
                                                            leadingIcon = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline, null) },
                                                            onClick = {
                                                                viewModel.toggleFavoriteApp(app.packageName)
                                                                showItemMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                    if (folderPages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PageIndicator(
                            pageCount = folderPages.size,
                            currentPage = folderPagerState.currentPage
                        )
                    }
                }
            }

            if (showAppPicker) {
                MultiAppPickerDialog(
                    allApps = allAppsFlat,
                    iconShape = iconShape,
                    viewModel = viewModel,
                    initialSelectedIds = currentFolder.folderItems.map { it.uniqueId },
                    onDismiss = { showAppPicker = false },
                    onAppsSelected = {
                        viewModel.updateFolderApps(currentFolder.uniqueId, it)
                        showAppPicker = false
                    }
                )
            }
        }
    }
}

