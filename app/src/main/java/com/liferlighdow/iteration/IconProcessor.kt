package com.liferlighdow.iteration

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils

import java.util.concurrent.ConcurrentHashMap

class IconProcessor(private val context: Context) {
    // 預先分配常用的繪圖工具，避免在循環中創建
    private val threadPaint = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    private val threadMatrix = object : ThreadLocal<ColorMatrix>() {
        override fun initialValue(): ColorMatrix = ColorMatrix()
    }
    private val threadMatrixArray = object : ThreadLocal<FloatArray>() {
        override fun initialValue(): FloatArray = FloatArray(20)
    }

    // 緩存 Mask（遮罩），使用 ConcurrentHashMap 確保線程安全
    private val maskCache = ConcurrentHashMap<String, Bitmap>()

    private fun getOrCreateMask(shape: IconShape, size: Int): Bitmap {
        val key = "${shape.name}_$size"
        return maskCache.getOrPut(key) {
            val mask = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
            val canvas = Canvas(mask)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            
            if (shape == IconShape.CIRCLE) {
                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            } else {
                val cornerRadius = size * 0.238f
                canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), cornerRadius, cornerRadius, paint)
            }
            mask
        }
    }

    fun processIcon(
        icon: Drawable?,
        isThemed: Boolean,
        themeColors: ColorScheme?,
        style: IconStyle,
        shape: IconShape,
        sizePx: Int,
        isIconPack: Boolean = false,
        customBgColor: Int = 0,
        customFgColor: Int = 0,
        customUseOriginal: Boolean = false,
        customUseOriginalBg: Boolean = false
    ): ImageBitmap {
        // 1. 基礎檢查
        if (icon == null) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        }

        // 2. 準備畫布與底圖
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = threadPaint.get()!!

        // 3. 處理顏色邏輯 (這部分保持原本的高級染色邏輯，但優化效能)
        val m3Colors = if (isThemed && themeColors != null) {
            val p = themeColors.primary
            val op = themeColors.onPrimary
            val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
            val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
            m3 to m3On
        } else null

        val bgColor = determineBgColor(style, isThemed, m3Colors?.first,
            customBgColor, customUseOriginalBg)
        val fgColor = determineFgColor(style, isThemed, m3Colors, customFgColor, customUseOriginal)

        // 4. 繪製
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            val scale = 1.45f
            val scaledSize = (sizePx * scale).toInt()
            val offset = (sizePx - scaledSize) / 2

            // 繪製背景
            if (bgColor != null) {
                paint.color = bgColor
                paint.xfermode = null
                canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
            } else {
                icon.background?.let {
                    it.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    it.draw(canvas)
                }
            }

            // 繪製前景（處理染色）
            val filter = createColorFilter(fgColor)
            var drawnMonochrome = false

            // 嘗試單色層 (Android 13+)
            if (filter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !(style == IconStyle.CUSTOM && customUseOriginal)) {
                icon.monochrome?.let { mono ->
                    mono.colorFilter = filter
                    mono.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    mono.draw(canvas)
                    mono.colorFilter = null
                    drawnMonochrome = true
                }
            }

            if (!drawnMonochrome) {
                icon.foreground?.let { fg ->
                    if (filter != null) fg.colorFilter = filter
                    fg.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    fg.draw(canvas)
                    fg.colorFilter = null
                }
            }
        } else {
            // 傳統圖標繪製
            if (bgColor != null) {
                paint.color = bgColor
                paint.xfermode = null
                canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
            }
            
            if (fgColor != null) icon.setTint(fgColor)
            
            if (isIconPack) {
                val iconScale = 1.15f
                val s = (sizePx * iconScale).toInt()
                val o = (sizePx - s) / 2
                icon.setBounds(o, o, o + s, o + s)
            } else {
                icon.setBounds(0, 0, sizePx, sizePx)
            }
            icon.draw(canvas)
            icon.setTintList(null)
        }

        // 5. 關鍵優化：使用 PorterDuff 遮罩裁切路徑，取代 clipPath
        // 這種方式邊緣最平滑且效能最好
        val mask = getOrCreateMask(shape, sizePx)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        return output.asImageBitmap()
    }

    private fun determineBgColor(style: IconStyle, isThemed: Boolean, m3Color: Int?,
                                 customBg: Int, customUseOrigBg: Boolean): Int? {
        if (style == IconStyle.CUSTOM) return if (customUseOrigBg) null else customBg
        if (isThemed && m3Color != null) {
            return when (style) {
                IconStyle.STANDARD -> m3Color
                IconStyle.BLACK -> ColorUtils.blendARGB(Color.BLACK, m3Color, 0.3f)
                IconStyle.WHITE -> ColorUtils.blendARGB(Color.WHITE, m3Color, 0.5f)
                IconStyle.GLASS -> ColorUtils.blendARGB(Color.argb(100, 255, 255, 255), m3Color, 0.15f)
                else -> null
            }
        }
        return when (style) {
            IconStyle.STANDARD -> null
            IconStyle.BLACK -> Color.BLACK
            IconStyle.WHITE -> Color.WHITE
            IconStyle.GLASS -> Color.argb(120, 255, 255, 255)
            else -> null
        }
    }

    private fun determineFgColor(style: IconStyle, isThemed: Boolean, m3Colors: Pair<Int, Int>?, customFg: Int, customUseOrig: Boolean): Int? {
        if (style == IconStyle.CUSTOM) return if (customUseOrig) null else customFg
        if (isThemed && m3Colors != null) {
            return when (style) {
                IconStyle.STANDARD -> m3Colors.second
                IconStyle.BLACK -> ColorUtils.blendARGB(Color.WHITE, m3Colors.first, 0.3f)
                IconStyle.WHITE -> Color.BLACK
                IconStyle.GLASS -> m3Colors.first
                else -> null
            }
        }
        return when (style) {
            IconStyle.BLACK -> Color.WHITE
            IconStyle.WHITE -> Color.BLACK
            IconStyle.GLASS -> Color.WHITE
            else -> null
        }
    }

    private fun createColorFilter(fgColor: Int?): ColorFilter? {
        if (fgColor == null) return null
        val matrixArray = threadMatrixArray.get()!!
        val r = Color.red(fgColor).toFloat()
        val g = Color.green(fgColor).toFloat()
        val b = Color.blue(fgColor).toFloat()
        
        // 清空矩陣
        for (i in 0..19) matrixArray[i] = 0f
        
        // 快速染色矩陣
        matrixArray[0] = 0f; matrixArray[4] = r
        matrixArray[6] = 0f; matrixArray[9] = g
        matrixArray[12] = 0f; matrixArray[14] = b
        matrixArray[18] = 1f
        
        return ColorMatrixColorFilter(matrixArray)
    }
}
