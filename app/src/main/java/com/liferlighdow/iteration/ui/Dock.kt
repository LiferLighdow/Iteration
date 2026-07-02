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
    dockCornerRadius: Float = 42f,
    iconShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    isEditMode: Boolean = false,
    onAppClick: (AppModel) -> Unit,
    onLongClick: (Int) -> Unit,
    onDeleteClick: ((AppModel) -> Unit)? = null
) {
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
        }

        // 2. 內容層 (App 圖示)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = when(dockStyle) {
                    DockStyle.CLASSIC -> 10.dp + navPadding
                    else -> 20.dp + navPadding
                })
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            apps.forEachIndexed { index, app ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = -2.5f, targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "jiggle"
                    )
                    
                    val viewModel: MainViewModel = viewModel()
                    var showContextMenu by remember { mutableStateOf(false) }

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
                        modifier = Modifier.graphicsLayer {
                            if (isEditMode) rotationZ = rotation
                        }.combinedClickable(
                            onClick = {
                                if (app.packageName.isNotEmpty()) onAppClick(app) else onLongClick(
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.replace_app)) },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { onLongClick(index); showContextMenu = false }
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
