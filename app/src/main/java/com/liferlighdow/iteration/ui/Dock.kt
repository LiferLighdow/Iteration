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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    apps: List<AppModel>,
    iconSize: Dp,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop,
    dockStyle: DockStyle = DockStyle.MODERN,
    iconShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    isEditMode: Boolean = false,
    onAppClick: (String) -> Unit,
    onLongClick: (Int) -> Unit,
    onDeleteClick: ((AppModel) -> Unit)? = null
) {
    // 獲取導覽列（小白條）的高度
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dockStyle == DockStyle.MODERN)
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp + navPadding) 
                else 
                    Modifier // Classic 模式下背景直接貼底
            )
            .height(if (dockStyle == DockStyle.CLASSIC) 94.dp + navPadding else 100.dp)
            // 關鍵：將所有背景、模糊、折射邏輯統一交給 liquidGlassDock
            .liquidGlassDock(
                isLiquidGlass = isLiquidGlass,
                backdrop = backdrop,
                dockStyle = dockStyle,
                cornerRadius = 42.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ),
        contentAlignment = Alignment.Center
    ) {
        // 內容層：不受任何模糊影響
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (dockStyle == DockStyle.CLASSIC) Modifier.padding(bottom = navPadding) else Modifier)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEachIndexed { index, app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = -2.5f, targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
                    )
                    
                    val viewModel: MainViewModel = viewModel()
                    val mContext = LocalContext.current
                    var showContextMenu by remember { mutableStateOf(false) }
                    val shortcuts = remember(showContextMenu) { if (showContextMenu && app.packageName.isNotEmpty()) viewModel.getShortcuts(app.packageName) else emptyList() }

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
                        onDeleteClick = { onDeleteClick?.invoke(app) },
                        getIcon = { pkg -> viewModel.getIcon(pkg) },
                        modifier = Modifier.graphicsLayer {
                            if (isEditMode) rotationZ = rotation
                        }.combinedClickable(
                            onClick = {
                                if (app.packageName.isNotEmpty()) onAppClick(app.packageName) else onLongClick(
                                    index
                                )
                            },
                            onLongClick = {
                                if (app.packageName.isNotEmpty() && !isEditMode) showContextMenu =
                                    true
                                else onLongClick(index)
                            }
                        ),
                        showLabel = false,
                        iconSize = iconSize
                    )

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
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
                                        showContextMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text("Replace App") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { onLongClick(index); showContextMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_uninstall)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
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
                                showContextMenu = false
                            }
                        )
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
                text = stringResource(R.string.search_hint_short), // "Search"
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
