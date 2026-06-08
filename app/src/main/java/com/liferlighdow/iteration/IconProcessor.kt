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
        style: IconStyle,
        sizePx: Int
    ): ImageBitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        
        if (icon != null) {
            // 檢查是否支援 Material You (Monochrome 模式)
            val supportsMonochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && icon is AdaptiveIconDrawable) {
                icon.monochrome != null
            } else false

            // 如果不支援 Monochrome 且開啟了 Themed 模式，則強制降級為 Standard + 非 Themed
            val effectiveIsThemed = if (isThemed && !supportsMonochrome) false else isThemed
            val effectiveStyle = if (isThemed && !supportsMonochrome) IconStyle.STANDARD else style

            val scale = 1.4f
            val scaledSize = (sizePx * scale).toInt()
            val offset = (sizePx - scaledSize) / 2

            // 提前提取 M3 顏色資訊
            val m3Colors = if (effectiveIsThemed && themeColors != null) {
                val p = themeColors.primary
                val op = themeColors.onPrimary
                val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
                m3 to m3On
            } else null

            val m3Color = m3Colors?.first
            val m3OnColor = m3Colors?.second

            // 1. 決定背景顏色
            val bgColor = if (effectiveIsThemed && m3Color != null) {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> m3Color
                    IconStyle.BLACK -> mixColors(Color.BLACK, m3Color, 0.3f) 
                    IconStyle.WHITE -> mixColors(Color.WHITE, m3Color, 0.5f)
                    IconStyle.GLASS -> mixColors(Color.argb(100, 255, 255, 255), m3Color, 0.15f)
                }
            } else {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> null 
                    IconStyle.BLACK -> Color.BLACK
                    IconStyle.WHITE -> Color.WHITE
                    IconStyle.GLASS -> Color.argb(120, 255, 255, 255)
                }
            }

            // 2. 決定前景顏色 (ColorFilter)
            val fgColor = if (effectiveIsThemed && m3Colors != null) {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> m3OnColor
                    IconStyle.BLACK -> mixColors(Color.WHITE, m3Color!!, 0.3f)
                    IconStyle.WHITE -> Color.BLACK
                    IconStyle.GLASS -> m3Color
                }
            } else {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> null
                    IconStyle.BLACK -> Color.WHITE
                    IconStyle.WHITE -> Color.BLACK
                    IconStyle.GLASS -> Color.WHITE
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                // 繪製背景
                if (bgColor != null) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    bgPaint.color = bgColor
                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
                } else {
                    icon.background?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    icon.background?.draw(canvas)
                }

                // 繪製前景
                val filter = if (fgColor != null) {
                    ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                        0f, 0f, 0f, 0f, Color.red(fgColor).toFloat(),
                        0f, 0f, 0f, 0f, Color.green(fgColor).toFloat(),
                        0f, 0f, 0f, 0f, Color.blue(fgColor).toFloat(),
                        0f, 0f, 0f, 1f, 0f
                    )))
                } else null

                var drawn = false
                if (filter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    icon.monochrome?.let { mono ->
                        mono.colorFilter = filter
                        mono.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                        mono.draw(canvas)
                        mono.colorFilter = null
                        drawn = true
                    }
                }

                if (!drawn) {
                    icon.foreground?.let {
                        // 只有當 effectiveIsThemed 為 true 或是非 Standard 樣式時才套用 filter
                        // 這裡加上一個保險，確保如果不支援 Monochrome 就不會被強制變色
                        if (effectiveIsThemed || effectiveStyle != IconStyle.STANDARD) {
                            it.colorFilter = filter
                        }
                        it.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                        it.draw(canvas)
                        it.colorFilter = null
                    }
                }
            } else {
                // 非 Adaptive Icon 的處理
                if (bgColor != null) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    bgPaint.color = bgColor
                    canvas.drawRoundRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), sizePx * 0.2f, sizePx * 0.2f, bgPaint)
                }
                
                if (fgColor != null) icon.setTint(fgColor)
                icon.setBounds(0, 0, sizePx, sizePx)
                icon.draw(canvas)
            }
        }
        return b.asImageBitmap()
    }

    private fun mixColors(base: Int, tint: Int, amount: Float): Int {
        val r = (Color.red(base) * (1 - amount) + Color.red(tint) * amount).toInt()
        val g = (Color.green(base) * (1 - amount) + Color.green(tint) * amount).toInt()
        val b = (Color.blue(base) * (1 - amount) + Color.blue(tint) * amount).toInt()
        val a = Color.alpha(base)
        return Color.argb(a, r, g, b)
    }
}
