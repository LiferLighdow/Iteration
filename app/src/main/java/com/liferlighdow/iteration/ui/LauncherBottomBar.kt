package com.liferlighdow.iteration.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.viewmodel.MainViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@Composable
fun LauncherBottomBar(
    visibilityProgress: Float,
    showPill: Boolean,
    // 配置
    isLiquidGlassEnabled: Boolean,
    isLiquidGlassDockEnabled: Boolean,
    backdrop: LayerBackdrop,
    iconSize: Dp,
    iconShape: IconShape,
    dockStyle: DockStyle,
    dockCornerRadius: Float,
    // 模糊與效果參數 (注意型別與子組件一致)
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean,
    // 分頁資訊
    horizontalPadding: Dp = 16.dp,
    desktopPageCount: Int,
    currentPage: Int,
    // App 數據
    dockApps: List<AppModel>,
    isEditMode: Boolean,
    myPackageName: String,
    notificationCounts: Map<String, Int> = emptyMap(),
    // 回調
    onSearchClick: () -> Unit,
    onAppClick: (AppModel, Offset) -> Unit,
    onSettingsClick: () -> Unit,
    onLongClick: (Int) -> Unit,
    onReplaceClick: (Int) -> Unit,
    onDeleteClick: (AppModel) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val activeContextMenuId by viewModel.activeContextMenuId.collectAsState()
    val isAnyMenuVisible = activeContextMenuId != null
    val context = androidx.compose.ui.platform.LocalContext.current

    val menuAlpha by animateFloatAsState(
        targetValue = if (isAnyMenuVisible) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "MenuAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // 根據滑動進度調整位移與透明度
                translationY = (1f - visibilityProgress) * 120.dp.toPx()
                alpha = visibilityProgress
            }
    ) {
        if (visibilityProgress > 0f) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer { alpha = menuAlpha }
                ) {
                    AnimatedContent(
                        targetState = showPill,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                        },
                        label = "PillVsDots"
                    ) { isPill ->
                        if (isPill) {
                            SearchPill(
                                isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassDockEnabled,
                                backdrop = backdrop,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration,
                                onClick = onSearchClick
                            )
                        } else {
                            PageIndicator(
                                pageCount = desktopPageCount,
                                currentPage = currentPage
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    // Dock Background that disappears
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = menuAlpha }
                    ) {
                        // We need a dummy Dock here or move background logic to a separate component
                        // For now, let's just accept that icons handle themselves and we fade decorations
                    }

                    Dock(
                        apps = dockApps,
                        iconSize = iconSize,
                        horizontalPadding = horizontalPadding,
                        isLiquidGlass = isLiquidGlassEnabled && isLiquidGlassDockEnabled,
                        backdrop = backdrop,
                        dockStyle = dockStyle,
                        dockCornerRadius = dockCornerRadius,
                        iconShape = iconShape,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration,
                        isEditMode = isEditMode,
                        notificationCounts = notificationCounts,
                        onAppClick = { app, pos ->
                            if (app.packageName == context.packageName) onSettingsClick() else onAppClick(app, pos)
                        },
                        onLongClick = onLongClick,
                        onReplaceClick = onReplaceClick,
                        onDeleteClick = onDeleteClick
                    )
                }

                if (dockStyle == DockStyle.MODERN || dockStyle == DockStyle.LITE) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
