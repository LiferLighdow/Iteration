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

class IconProcessor(private val context: Context) {
    // 使用 ThreadLocal 確保執行緒安全的物件重用，相容 minSdk 23
    private val threadPaint = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }
    private val threadPath = object : ThreadLocal<Path>() {
        override fun initialValue(): Path = Path()
    }
    private val threadMatrixArray = object : ThreadLocal<FloatArray>() {
        override fun initialValue(): FloatArray = FloatArray(20)
    }

    fun processIcon(
        icon: Drawable?,
        isThemed: Boolean,
        themeColors: ColorScheme?,
        style: IconStyle,
        shape: IconShape,
        sizePx: Int,
        isIconPack: Boolean = false
    ): ImageBitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        
        // 重用 Path 物件並根據目前尺寸更新
        val path = threadPath.get()!!.apply {
            reset()
            if (shape == IconShape.CIRCLE) {
                addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Path.Direction.CW)
            } else {
                val cornerRadius = sizePx * 0.238f
                addRoundRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
            }
        }
        canvas.clipPath(path)
        
        if (icon != null) {
            val supportsMonochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && icon is AdaptiveIconDrawable) {
                icon.monochrome != null
            } else false

            val effectiveIsThemed = if (isThemed && !supportsMonochrome) false else isThemed
            val effectiveStyle = if (isThemed && !supportsMonochrome) IconStyle.STANDARD else style

            val scale = 1.4f
            val scaledSize = (sizePx * scale).toInt()
            val offset = (sizePx - scaledSize) / 2

            val m3Colors = if (effectiveIsThemed && themeColors != null) {
                val p = themeColors.primary
                val op = themeColors.onPrimary
                val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
                m3 to m3On
            } else null

            val m3Color = m3Colors?.first
            val m3OnColor = m3Colors?.second

            // 1. 決定背景顏色 (使用 ColorUtils.blendARGB 優化混合)
            val bgColor = if (effectiveIsThemed && m3Color != null) {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> m3Color
                    IconStyle.BLACK -> ColorUtils.blendARGB(Color.BLACK, m3Color, 0.3f)
                    IconStyle.WHITE -> ColorUtils.blendARGB(Color.WHITE, m3Color, 0.5f)
                    IconStyle.GLASS -> ColorUtils.blendARGB(Color.argb(100, 255, 255, 255), m3Color, 0.15f)
                }
            } else {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> {
                        val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable
                        if (!isAdaptive && !isIconPack) Color.WHITE else null
                    }
                    IconStyle.BLACK -> Color.BLACK
                    IconStyle.WHITE -> Color.WHITE
                    IconStyle.GLASS -> Color.argb(120, 255, 255, 255)
                }
            }

            // 2. 決定前景顏色
            val fgColor = if (effectiveIsThemed && m3Colors != null) {
                when (effectiveStyle) {
                    IconStyle.STANDARD -> m3OnColor
                    IconStyle.BLACK -> ColorUtils.blendARGB(Color.WHITE, m3Color!!, 0.3f)
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
                if (bgColor != null) {
                    val bgPaint = threadPaint.get()!!.apply {
                        color = bgColor
                        colorFilter = null
                    }
                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
                } else {
                    icon.background?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                    icon.background?.draw(canvas)
                }

                val filter = if (fgColor != null) {
                    val matrixArray = threadMatrixArray.get()!!
                    matrixArray[0] = 0f; matrixArray[1] = 0f; matrixArray[2] = 0f; matrixArray[3] = 0f; matrixArray[4] = Color.red(fgColor).toFloat()
                    matrixArray[5] = 0f; matrixArray[6] = 0f; matrixArray[7] = 0f; matrixArray[8] = 0f; matrixArray[9] = Color.green(fgColor).toFloat()
                    matrixArray[10] = 0f; matrixArray[11] = 0f; matrixArray[12] = 0f; matrixArray[13] = 0f; matrixArray[14] = Color.blue(fgColor).toFloat()
                    matrixArray[15] = 0f; matrixArray[16] = 0f; matrixArray[17] = 0f; matrixArray[18] = 1f; matrixArray[19] = 0f
                    ColorMatrixColorFilter(matrixArray)
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
                        if (effectiveIsThemed || effectiveStyle != IconStyle.STANDARD) {
                            it.colorFilter = filter
                        }
                        it.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                        it.draw(canvas)
                        it.colorFilter = null
                    }
                }
            } else {
                if (bgColor != null) {
                    val bgPaint = threadPaint.get()!!.apply {
                        color = bgColor
                        colorFilter = null
                    }
                    canvas.drawRoundRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), sizePx * 0.2f, sizePx * 0.2f, bgPaint)
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
            }
        }
        return b.asImageBitmap()
    }
}
