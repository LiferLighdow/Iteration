package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel

@Composable
fun MusicWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val isWide = (widget.type as? WidgetType.Music)?.isWide ?: false
    if (isWide) {
        WideMusicWidget(displayMode, modifier, backdrop, isMinusOnePage)
    } else {
        StandardMusicWidget(displayMode, modifier, backdrop, isMinusOnePage)
    }
}

@Composable
fun StandardMusicWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mediaInfo by NotificationService.currentMedia.collectAsState()

    val isGlass = displayMode == WidgetDisplayMode.GLASS
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Album Art
            mediaInfo?.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (displayMode == WidgetDisplayMode.GLASS) 20.dp else 40.dp)
                        .graphicsLayer(alpha = 0.3f),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mediaInfo?.title ?: stringResource(R.string.no_music_playing),
                        style = MaterialTheme.typography.titleSmall.withGlassShadow(isGlass),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaInfo?.artist ?: stringResource(R.string.unknown_artist),
                        style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass),
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaInfo?.albumArt != null) {
                        Image(
                            bitmap = mediaInfo!!.albumArt!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = contentColor.copy(alpha = 0.5f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { NotificationService.sendMediaCommand("previous") }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor)
                    }
                    FilledTonalIconButton(
                        onClick = { NotificationService.sendMediaCommand("play_pause") },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (displayMode == WidgetDisplayMode.GLASS) glassFallbackColor(
                                0.3f
                            ) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (mediaInfo?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (displayMode == WidgetDisplayMode.GLASS) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { NotificationService.sendMediaCommand("next") }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WideMusicWidget(
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null

    val mediaInfo by NotificationService.currentMedia.collectAsState()

    val isGlass = displayMode == WidgetDisplayMode.GLASS
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier
            .aspectRatio(2f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Album Art
            mediaInfo?.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (displayMode == WidgetDisplayMode.GLASS) 20.dp else 40.dp)
                        .graphicsLayer(alpha = 0.3f),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art on the left
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaInfo?.albumArt != null) {
                        Image(
                            bitmap = mediaInfo!!.albumArt!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = contentColor.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info and Controls on the right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = mediaInfo?.title ?: stringResource(R.string.no_music_playing),
                            style = MaterialTheme.typography.titleMedium.withGlassShadow(isGlass),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaInfo?.artist ?: stringResource(R.string.unknown_artist),
                            style = MaterialTheme.typography.bodyMedium.withGlassShadow(isGlass),
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { NotificationService.sendMediaCommand("previous") }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { NotificationService.sendMediaCommand("play_pause") },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (displayMode == WidgetDisplayMode.GLASS) glassFallbackColor(
                                    0.3f
                                ) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (mediaInfo?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(32.dp),
                                tint = if (displayMode == WidgetDisplayMode.GLASS) Color.White else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { NotificationService.sendMediaCommand("next") }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
