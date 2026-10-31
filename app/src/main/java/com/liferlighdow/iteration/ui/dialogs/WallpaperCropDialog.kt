package com.liferlighdow.iteration.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.liferlighdow.iteration.R
import kotlin.math.roundToInt

private fun decodeSampledBitmapFromUri(context: android.content.Context, uri: Uri, reqWidth: Int = 2048, reqHeight: Int = 2048): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        null
    }
}

@Composable
fun WallpaperCropDialog(
    uri: Uri? = null,
    bitmap: Bitmap? = null,
    onDismiss: () -> Unit,
    onConfirm: (cropped: Bitmap, original: Bitmap) -> Unit
) {
    val mContext = LocalContext.current
    val originalBitmap = remember(uri, bitmap) {
        if (bitmap != null) return@remember bitmap
        uri?.let { decodeSampledBitmapFromUri(mContext, it) }
    }

    if (originalBitmap == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { containerSize = it.size.toSize() }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale *= zoom
                                offset += pan
                            }
                        }
                ) {
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // Crop Overlay (Screen Aspect Ratio)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val dm = mContext.resources.displayMetrics
                    val screenRatio = dm.widthPixels.toFloat() / dm.heightPixels.toFloat()
                    
                    val overlayWidth = containerSize.width * 0.8f
                    val overlayHeight = overlayWidth / screenRatio

                    // Draw Overlay and Border
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Darken background outside crop area
                        val path = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addRoundRect(
                                RoundRect(
                                    left = center.x - overlayWidth / 2f,
                                    top = center.y - overlayHeight / 2f,
                                    right = center.x + overlayWidth / 2f,
                                    bottom = center.y + overlayHeight / 2f,
                                    cornerRadius = CornerRadius(24.dp.toPx())
                                )
                            )
                        }
                        drawPath(path, color = Color.Black.copy(alpha = 0.7f))

                        // 2. Draw clear white border for crop area
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(center.x - overlayWidth / 2f, center.y - overlayHeight / 2f),
                            size = Size(overlayWidth, overlayHeight),
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    
                    Text(
                        text = stringResource(R.string.adjust_position),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(onClick = {
                        val cropped = cropWallpaperBitmap(originalBitmap, scale, offset, containerSize, mContext)
                        onConfirm(cropped, originalBitmap)
                    }) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

fun cropWallpaperBitmap(original: Bitmap, scale: Float, offset: Offset, containerSize: Size, context: android.content.Context): Bitmap {
    val dm = context.resources.displayMetrics
    val targetWidth = dm.widthPixels
    val targetHeight = dm.heightPixels
    val screenRatio = targetWidth.toFloat() / targetHeight

    val overlayWidth = containerSize.width * 0.8f
    val overlayHeight = overlayWidth / screenRatio

    val bitmapWidth = original.width.toFloat()
    val bitmapHeight = original.height.toFloat()

    val scaleX = containerSize.width / bitmapWidth
    val scaleY = containerSize.height / bitmapHeight
    val baseScale = Math.min(scaleX, scaleY)

    val totalScale = Math.max(baseScale * scale, 0.01f)

    val centerX = containerSize.width / 2f
    val centerY = containerSize.height / 2f

    val bitmapLeftInContainer = centerX - (bitmapWidth * totalScale) / 2f + offset.x
    val bitmapTopInContainer = centerY - (bitmapHeight * totalScale) / 2f + offset.y

    val cropLeftInContainer = centerX - overlayWidth / 2f
    val cropTopInContainer = centerY - overlayHeight / 2f

    val xOffsetInBitmap = ((cropLeftInContainer - bitmapLeftInContainer) / totalScale).coerceIn(0f, Math.max(0f, bitmapWidth - 1f))
    val yOffsetInBitmap = ((cropTopInContainer - bitmapTopInContainer) / totalScale).coerceIn(0f, Math.max(0f, bitmapHeight - 1f))
    val widthInBitmap = (overlayWidth / totalScale).coerceAtMost(bitmapWidth - xOffsetInBitmap)
    val heightInBitmap = (overlayHeight / totalScale).coerceAtMost(bitmapHeight - yOffsetInBitmap)

    return try {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        val srcLeft = xOffsetInBitmap.roundToInt().coerceIn(0, original.width - 1)
        val srcTop = yOffsetInBitmap.roundToInt().coerceIn(0, original.height - 1)
        val srcRight = (xOffsetInBitmap + widthInBitmap).roundToInt().coerceIn(srcLeft + 1, original.width)
        val srcBottom = (yOffsetInBitmap + heightInBitmap).roundToInt().coerceIn(srcTop + 1, original.height)

        val srcRect = android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom)
        val dstRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)

        canvas.drawBitmap(original, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        result
    } catch (e: Exception) {
        e.printStackTrace()
        original
    } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        original
    }
}
