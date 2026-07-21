package com.liferlighdow.iteration.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.viewmodel.*
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.R

import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

@Composable
fun AppItem(
    app: AppModel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: Dp = 62.dp,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
    iconShape: IconShape = IconShape.DEFAULT,
    libraryShape: IconShape = IconShape.DEFAULT,
    blurRadius: Float = 0f,
    refractionHeight: Float = 24f,
    refractionAmount: Float = 48f,
    chromaticAberration: Boolean = true,
    isEditMode: Boolean = false,
    showReflection: Boolean = false,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onAppClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    notificationCountProvider: (() -> Int)? = null,
    getIcon: @Composable (String) -> ImageBitmap? = { null }
) {
    val viewModel: MainViewModel = viewModel()
    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
    val iconSignal by viewModel.iconUpdateSignal.collectAsState()

    // 針對資料夾內部的圖標，我們也需要監聽 iconSignal 確保重組
    val safeGetIcon: @Composable (String) -> ImageBitmap? = { id ->
        remember(id, iconSignal) { viewModel.getIcon(id) } ?: getIcon(id)
    }
    
    val appIcon = remember(app.uniqueId, iconSignal) {
        if (!app.isFolder) {
            viewModel.getIcon(app.uniqueId)
        } else null
    }
    
    val displayIcon = appIcon ?: if (!app.isFolder) getIcon(app.uniqueId) else null
    
    // 效能優化：不再集體收集 NotificationService.notifications
    // 改由外部 Provider 傳入具體數字，避免集體重組
    val count = notificationCountProvider?.invoke() ?: 0

    val currentShape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(iconSize * 0.238f)

    val colorFilter = remember(app.isFrozen) {
        if (app.isFrozen) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else null
    }

    val removingItemIds by viewModel.removingItemIds.collectAsState()
    val isRemoving = remember(removingItemIds, app.uniqueId) { removingItemIds.contains(app.uniqueId) }

    // 解決資料夾進場太早的問題：增加一個微小的準備狀態
    var isFolderReady by remember(app.uniqueId) { mutableStateOf(false) }
    if (app.isFolder) {
        LaunchedEffect(app.uniqueId) {
            delay(300) // 增加到 300ms 以更好地對齊啟動時的延遲
            isFolderReady = true
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isRemoving) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "RemovingScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isRemoving) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "RemovingAlpha"
    )

    Column(
        modifier = modifier
            .padding(vertical = if (showLabel) 4.dp else 0.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
                if (isRemoving) {
                    rotationZ = (1f - animatedScale) * 30f // 增加旋轉動感
                    // Android 12+ 模糊效果 (質感提升)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            (1f - animatedScale) * 30f,
                            (1f - animatedScale) * 30f,
                            android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
            }
            .then(
                if (onAppClick != null) {
                    Modifier.clickable { onAppClick() }
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Reflection
            if (showReflection) {
                if (!app.isFolder && displayIcon != null) {
                    Image(
                        bitmap = displayIcon,
                        contentDescription = null,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .offset(y = iconSize * 0.7f)
                            .size(iconSize)
                            .graphicsLayer {
                                rotationX = 180f
                                alpha = 0.3f
                            }
                            .clip(currentShape)
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                )
                            },
                        contentScale = ContentScale.FillBounds
                    )
                } else if (app.isFolder && isFolderReady) {
                    FolderIconContent(
                        app = app,
                        iconSize = iconSize,
                        isLiquidGlass = isLiquidGlass,
                        backdrop = backdrop,
                        iconShape = iconShape,
                        libraryShape = libraryShape,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        getIcon = safeGetIcon,
                        modifier = Modifier
                            .offset(y = iconSize * 0.7f)
                            .graphicsLayer {
                                rotationX = 180f
                                alpha = 0.3f
                            }
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                )
                            }
                    )
                }
            }

            Box(contentAlignment = Alignment.TopEnd) {
                // 使用 Crossfade 處理圖示切換，解決載入一半或殘影問題
                // 優化：將資料夾納入動畫目標，確保其出現時也有淡入效果
                val animationTarget = when {
                    app.isFolder -> if (isFolderReady) "folder" else null
                    app.packageName.isEmpty() -> "empty"
                    else -> displayIcon
                }
                
                Crossfade(
                    targetState = animationTarget,
                    animationSpec = tween(250, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                    label = "IconFade"
                ) { targetState ->
                    if (app.packageName.isEmpty() && !app.isFolder) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(currentShape)
                                .background(glassFallbackColor(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        }
                    } else if (targetState == "folder") {
                        FolderIconContent(
                            app = app,
                            iconSize = iconSize,
                            isLiquidGlass = isLiquidGlass,
                            backdrop = backdrop,
                            iconShape = iconShape,
                            libraryShape = libraryShape,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration,
                            getIcon = safeGetIcon
                        )
                    } else if (targetState is ImageBitmap) {
                        Image(
                            bitmap = targetState,
                            contentDescription = null,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(currentShape)
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = currentShape
                                ),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        // 佔位符，防止圖示區域塌陷
                        Box(modifier = Modifier.size(iconSize))
                    }
                }

                // 編輯模式下的叉叉按鈕
                if (isEditMode && !isDesktopLocked && (app.packageName.isNotEmpty() || app.isFolder || app.isWidget)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .size(24.dp)
                            .background(Color.Gray.copy(alpha = 0.9f), CircleShape)
                            .clickable { onDeleteClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.delete),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (count > 9) 9.sp else 11.sp
                        )
                    }
                }
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = labelFontSize,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun FolderIconContent(
    app: AppModel,
    iconSize: Dp,
    isLiquidGlass: Boolean,
    backdrop: Backdrop?,
    iconShape: IconShape,
    libraryShape: IconShape,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    getIcon: @Composable (String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .liquidGlass(
                enabled = isLiquidGlass,
                backdrop = backdrop,
                cornerRadius = if (libraryShape == IconShape.CIRCLE) iconSize / 2f else iconSize * 0.238f,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                FolderPreviewIcon(app.folderItems.getOrNull(0), iconSize / 2.5f, iconShape, getIcon)
                FolderPreviewIcon(app.folderItems.getOrNull(1), iconSize / 2.5f, iconShape, getIcon)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                FolderPreviewIcon(app.folderItems.getOrNull(2), iconSize / 2.5f, iconShape, getIcon)
                FolderPreviewIcon(app.folderItems.getOrNull(3), iconSize / 2.5f, iconShape, getIcon)
            }
        }
    }
}

@Composable
fun FolderPreviewIcon(
    app: AppModel?,
    size: Dp,
    iconShape: IconShape = IconShape.DEFAULT,
    getIcon: @Composable (String) -> ImageBitmap?
) {
    val viewModel: MainViewModel = viewModel()
    val iconSignal by viewModel.iconUpdateSignal.collectAsState()
    
    val currentShape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(size * 0.238f)
    val appIcon = app?.let { if (!it.isFolder) {
        remember(it.uniqueId, iconSignal) { viewModel.getIcon(it.uniqueId) } ?: getIcon(it.uniqueId)
    } else null }
    val colorFilter = remember(app?.isFrozen) {
        if (app?.isFrozen == true) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else null
    }
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            colorFilter = colorFilter,
            modifier = Modifier
                .size(size)
                .clip(currentShape)
        )
    } else {
        Spacer(modifier = Modifier.size(size))
    }
}
