package com.liferlighdow.iteration

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

class IconProcessor(private val context: Context) {
    
    fun processIcon(
        icon: Drawable?,
        isThemed: Boolean,
        themeColors: ColorScheme?,
        sizePx: Int
    ): ImageBitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        
        if (icon != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                val scale = 1.4f
                val scaledSize = (sizePx * scale).toInt()
                val offset = (sizePx - scaledSize) / 2
                
                if (isThemed && themeColors != null) {
                    // 1. 繪製背景 (使用 Primary)
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val bgCol = themeColors.primary
                    bgPaint.color = Color.argb(
                        (bgCol.alpha * 255).toInt(),
                        (bgCol.red * 255).toInt(),
                        (bgCol.green * 255).toInt(),
                        (bgCol.blue * 255).toInt()
                    )
                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

                    // 2. 染色並繪製前景 (使用 OnPrimary)
                    val fgCol = themeColors.onPrimary
                    val iconColor = Color.argb(
                        (fgCol.alpha * 255).toInt(),
                        (fgCol.red * 255).toInt(),
                        (fgCol.green * 255).toInt(),
                        (fgCol.blue * 255).toInt()
                    )
                    
                    val cm = ColorMatrix(floatArrayOf(
                        0f, 0f, 0f, 0f, Color.red(iconColor).toFloat(),
                        0f, 0f, 0f, 0f, Color.green(iconColor).toFloat(),
                        0f, 0f, 0f, 0f, Color.blue(iconColor).toFloat(),
                        0f, 0f, 0f, 1f, 0f
                    ))
                    
                    val filter = ColorMatrixColorFilter(cm)

                    // 優先使用 Android 13+ 的 Monochrome (單色) 圖層
                    var drawn = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        icon.monochrome?.let { mono ->
                            mono.colorFilter = filter
                            mono.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                            mono.draw(canvas)
                            mono.colorFilter = null
                            drawn = true
                        }
                    }

                    // 如果沒有單色圖層，則回退到前景圖層染色
                    if (!drawn) {
                        icon.foreground?.let {
                            it.colorFilter = filter
                            it.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                            it.draw(canvas)
                            it.colorFilter = null
                        }
                    }
                } else {
                    icon.background?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    icon.background?.draw(canvas)
                    icon.foreground?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    icon.foreground?.draw(canvas)
                }
            } else {
                if (isThemed && themeColors != null) {
                    val fgCol = themeColors.primary
                    val iconColor = Color.argb((fgCol.alpha * 255).toInt(), (fgCol.red * 255).toInt(), (fgCol.green * 255).toInt(), (fgCol.blue * 255).toInt())
                    icon.setTint(iconColor)
                }
                icon.setBounds(0, 0, sizePx, sizePx)
                icon.draw(canvas)
            }
        }
        return b.asImageBitmap()
    }
}
