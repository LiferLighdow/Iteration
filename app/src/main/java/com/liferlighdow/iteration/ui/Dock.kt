package com.liferlighdow.iteration.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import androidx.compose.material.icons.filled.AcUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    apps: List<AppModel>,
    iconSize: Dp,
    horizontalPadding: Dp = 12.dp,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop,
    dockStyle: DockStyle = DockStyle.MODERN,
    dockCornerRadius: Float = 42f,
    iconShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    isEditMode: Boolean = false,
    notificationCounts: Map<String, Int> = emptyMap(),
    onAppClick: (AppModel) -> Unit,
    onLongClick: (Int) -> Unit,
    onReplaceClick: (Int) -> Unit,
    onDeleteClick: ((AppModel) -> Unit)? = null
) {
    val viewModel: MainViewModel = viewModel()
    val activeContextMenuId by viewModel.activeContextMenuId.collectAsState()
    val isAnyMenuVisible = activeContextMenuId != null
    val menuAlpha by animateFloatAsState(
        targetValue = if (isAnyMenuVisible) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "MenuAlpha"
    )

    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // 容器總高度
    val totalHeight = when (dockStyle) {
        DockStyle.CLASSIC -> 94.dp + navPadding
        DockStyle.PLATFORM -> 90.dp + navPadding
        else -> 100.dp + navPadding // Modern 恢復 100dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. 背景層
        when (dockStyle) {
            DockStyle.MODERN -> {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = navPadding)
                        .fillMaxSize() // 讓 Modern 背景填滿容器（減去 padding）
                        .graphicsLayer { alpha = menuAlpha }
                        .liquidGlassDock(
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            dockStyle = dockStyle,
                            cornerRadius = dockCornerRadius.dp,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration
                        )
                )
            }
            DockStyle.CLASSIC -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = menuAlpha }
                        .liquidGlassDock(
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            dockStyle = dockStyle,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration
                        )
                )
            }
            DockStyle.PLATFORM -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp + navPadding)
                        .graphicsLayer { alpha = menuAlpha }
                        .liquidGlassDock(
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            dockStyle = dockStyle,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration
                        )
                )
            }
            DockStyle.LITE -> {
                // LITE style has no background decorations
            }
        }

        // 2. 內容層 (App 圖示)
        val dockContentHeight = when (dockStyle) {
            DockStyle.CLASSIC -> 94.dp
            DockStyle.PLATFORM -> 90.dp
            else -> 100.dp
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navPadding)
                .height(dockContentHeight)
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = when(dockStyle) {
                DockStyle.PLATFORM -> Alignment.Bottom
                else -> Alignment.CenterVertically
            }
        ) {
            val contentBottomPadding = if (dockStyle == DockStyle.PLATFORM) 20.dp else 0.dp

            apps.forEachIndexed { index, app ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = contentBottomPadding),
                    contentAlignment = if (dockStyle == DockStyle.PLATFORM) Alignment.BottomCenter else Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = -2.5f, targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
                    )
                    
                    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
                    var showContextMenu by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    AppItem(
                        app = app,
                        isLiquidGlass = isLiquidGlass,
                        backdrop = backdrop,
                        iconShape = iconShape,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        isEditMode = isEditMode,
                        showReflection = dockStyle == DockStyle.PLATFORM,
                        onDeleteClick = { onDeleteClick?.invoke(app) },
                        getIcon = { pkg -> viewModel.getIcon(pkg) },
                        notificationCountProvider = { notificationCounts[app.packageName] ?: 0 },
                        modifier = Modifier.graphicsLayer {
                            if (isEditMode) rotationZ = rotation
                        }.pointerInput(app.uniqueId) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.setPressedItemId(app.uniqueId)
                                    try { awaitRelease() } finally { viewModel.setPressedItemId(null) }
                                },
                                onTap = {
                                    if (app.isFolder || app.packageName.isNotEmpty()) onAppClick(app) else {
                                        if (!isDesktopLocked) onLongClick(index)
                                    }
                                },
                                onLongPress = {
                                    if (app.isFolder || app.packageName.isNotEmpty()) {
                                        if (!isEditMode && !isDesktopLocked) {
                                            viewModel.setActiveContextMenuId(app.uniqueId)
                                            showContextMenu = true
                                        }
                                    } else {
                                        if (!isDesktopLocked) onLongClick(index)
                                    }
                                }
                            )
                        },
                        showLabel = false,
                        iconSize = iconSize
                    )

                    val dismissMenu = {
                        showContextMenu = false
                        viewModel.clearFocusState()
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = dismissMenu,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        val actionMode by viewModel.actionMode.collectAsState()
                        val menuOptions by viewModel.homeMenuOptions.collectAsState()

                        // 1. 凍結/解凍
                        if (menuOptions.contains("freeze") && (actionMode == ActionMode.SHIZUKU || actionMode == ActionMode.ROOT) && !app.isFolder) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (app.isFrozen) R.string.unfreeze else R.string.freeze)) },
                                leadingIcon = { Icon(Icons.Default.AcUnit, null) },
                                onClick = { viewModel.toggleFreezeApp(app, context); dismissMenu() }
                            )
                        }

                        // 2. 替換/更改 (Replace App / Change to App)
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.isFolder) R.string.change_to_app else R.string.replace_app)) },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                            onClick = { onReplaceClick(index); dismissMenu() }
                        )

                        // 3. 轉為資料夾 (Change to Folder) - 僅在非資料夾且有 App 時出現
                        if (!app.isFolder && app.packageName.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_to_folder)) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                                onClick = { viewModel.convertDockAppToFolder(index); dismissMenu() }
                            )
                        }

                        // 4. 移除 (Delete from Home)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_home)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { 
                                viewModel.removeAppFromDock(index)
                                dismissMenu() 
                            }
                        )

                        // 5. 卸載 (Uninstall) - 僅非系統 App 且非資料夾
                        if (!app.isFolder && !app.isSystem && app.packageName.isNotEmpty()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_uninstall)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showNativeUninstallDialog(context, app.label) {
                                        if (app.isPWA) {
                                            viewModel.deletePWA(app)
                                        } else {
                                            try {
                                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                                    data = Uri.fromParts("package", app.packageName, null)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("Iteration", "Uninstall failed", e)
                                            }
                                        }
                                    }
                                    dismissMenu()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchPill(
    isLiquidGlass: Boolean,
    backdrop: Backdrop?,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(26.dp)
            .liquidGlass(
                enabled = isLiquidGlass,
                backdrop = backdrop,
                cornerRadius = 13.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.search_hint_short),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
        repeat(pageCount) { index ->
            val color = if (currentPage == index) Color.White else Color.White.copy(alpha = 0.5f)
            Box(modifier = Modifier.padding(4.dp).size(8.dp).background(color, RoundedCornerShape(4.dp)))
        }
    }
}
