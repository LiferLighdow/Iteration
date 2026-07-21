package com.liferlighdow.iteration.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.data.AppModel

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
    onAppClick: (AppModel) -> Unit,
    onSettingsClick: () -> Unit,
    onLongClick: (Int) -> Unit,
    onReplaceClick: (Int) -> Unit,
    onDeleteClick: (AppModel) -> Unit
) {

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
                Box(contentAlignment = Alignment.Center) {
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
                    onAppClick = { app ->
                        if (app.packageName == myPackageName) onSettingsClick() else onAppClick(app)
                    },
                    onLongClick = onLongClick,
                    onReplaceClick = onReplaceClick,
                    onDeleteClick = onDeleteClick
                )

                if (dockStyle == DockStyle.MODERN || dockStyle == DockStyle.LITE) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
