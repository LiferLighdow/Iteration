package com.liferlighdow.iteration.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
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
    onAppClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    getIcon: @Composable (String) -> ImageBitmap? = { null }
) {
    val viewModel: MainViewModel = viewModel()
    val iconSignal by viewModel.iconUpdateSignal.collectAsState()
    val notificationCounts by NotificationService.notifications.collectAsState()
    
    val appIcon = remember(app.uniqueId, iconSignal) {
        if (!app.isFolder) viewModel.getIcon(app.uniqueId) else null
    }

    // 計算通知數量：若是資料夾，則加總內部所有 App 的數量
    val count = if (app.isFolder) {
        app.folderItems.sumOf { notificationCounts[it.packageName] ?: 0 }
    } else {
        notificationCounts[app.packageName] ?: 0
    }

    val currentShape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(iconSize * 0.238f)

    Column(
        modifier = modifier
            .padding(vertical = if (showLabel) 4.dp else 0.dp)
            .then(
                if (onAppClick != null) {
                    Modifier.clickable { onAppClick() }
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (app.packageName.isEmpty() && !app.isFolder) {
                // Dock 空位顯示添加圖示
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(currentShape)
                        .background(glassFallbackColor(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
            } else if (app.isFolder) {
                Box(
                    modifier = Modifier
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
            } else if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
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
            }

            // 編輯模式下的叉叉按鈕
            if (isEditMode && (app.packageName.isNotEmpty() || app.isFolder)) {
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
                        contentDescription = "Delete",
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
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
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
fun FolderPreviewIcon(
    app: AppModel?,
    size: Dp,
    iconShape: IconShape = IconShape.DEFAULT,
    getIcon: @Composable (String) -> ImageBitmap?
) {
    val currentShape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(size * 0.238f)
    val appIcon = app?.let { if (!it.isFolder) getIcon(it.uniqueId) else null }
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(currentShape)
        )
    } else {
        Spacer(modifier = Modifier.size(size))
    }
}
