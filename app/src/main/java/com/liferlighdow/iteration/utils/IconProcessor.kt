package com.liferlighdow.iteration.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class IconProcessor(private val context: Context) {
    private val threadPaint = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    private val threadMatrixArray = object : ThreadLocal<FloatArray>() {
        override fun initialValue(): FloatArray = FloatArray(20)
    }

    private val maskCache = ConcurrentHashMap<String, Bitmap>()

    fun clearCache() {
        maskCache.clear()
    }

    fun getOrCreateMask(shape: IconShape, size: Int): Bitmap {
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
        customUseOriginalBg: Boolean = false,
        userId: Long = 0,
        isPrivate: Boolean = false,
        calendarDay: String? = null
    ): ImageBitmap {
        if (icon == null) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = threadPaint.get()!!

        val m3Colors = if (isThemed && themeColors != null) {
            val p = themeColors.primary
            val op = themeColors.onPrimary
            val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
            val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
            m3 to m3On
        } else null

        val bgColor = determineBgColor(style, isThemed, m3Colors?.first, customBgColor, customUseOriginalBg)
        val fgColor = determineFgColor(style, isThemed, m3Colors, customFgColor, customUseOriginal)

        if (calendarDay != null) {
            // 繪製自定義動態日曆設計
            drawCalendarDate(canvas, sizePx, calendarDay)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            val scale = 1.45f
            val scaledSize = (sizePx * scale).toInt()
            val offset = (sizePx - scaledSize) / 2

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

            val filter = createColorFilter(fgColor)
            var drawnMonochrome = false

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

        val mask = getOrCreateMask(shape, sizePx)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        if (isPrivate) {
            drawPrivateBadge(canvas, sizePx)
        } else if (userId > 0) {
            drawWorkBadge(canvas, output, sizePx, userId)
        }

        return output.asImageBitmap()
    }

    private fun drawCalendarDate(canvas: Canvas, sizePx: Int, day: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 1. 繪製純白背景 (填滿整個區域，之後會被 Mask 裁切成圓角或圓形)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        
        // 2. 繪製紅色頂部區塊 (佔約 28%)
        val headerHeight = sizePx * 0.28f
        paint.color = Color.parseColor("#E53935") // 經典日曆紅
        canvas.drawRect(0f, 0f, sizePx.toFloat(), headerHeight, paint)
        
        // 3. 繪製星期 (英文縮寫，如 MON, TUE)
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE", Locale.ENGLISH)
        val weekDay = sdf.format(calendar.time).uppercase()
        
        paint.color = Color.WHITE
        paint.textSize = headerHeight * 0.55f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        
        val weekX = sizePx / 2f
        val weekFontMetrics = paint.fontMetrics
        // 垂直居中在紅色區塊內
        val weekY = (headerHeight - weekFontMetrics.ascent - weekFontMetrics.descent) / 2f
        canvas.drawText(weekDay, weekX, weekY, paint)
        
        // 4. 繪製日期數字 (黑色)
        paint.color = Color.BLACK
        paint.textSize = sizePx * 0.42f
        paint.isFakeBoldText = true
        
        val dayX = sizePx / 2f
        val dayFontMetrics = paint.fontMetrics
        val remainingHeight = sizePx - headerHeight
        // 垂直居中在剩餘的白色區塊內
        val dayY = headerHeight + (remainingHeight - dayFontMetrics.ascent - dayFontMetrics.descent) / 2f
        canvas.drawText(day, dayX, dayY, paint)
    }

    private fun drawPrivateBadge(canvas: Canvas, sizePx: Int) {
        val badgeSize = (sizePx * 0.35f).toInt()
        val margin = (sizePx * 0.05f).toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 繪製背景圓圈
        paint.color = Color.parseColor("#CC000000")
        val centerX = sizePx - badgeSize/2f - margin
        val centerY = sizePx - badgeSize/2f - margin
        canvas.drawCircle(centerX, centerY, badgeSize/2f, paint)
        
        // 手動繪製鎖頭圖標 (Lock Icon)
        paint.color = Color.WHITE
        val lockWidth = badgeSize * 0.45f
        val lockHeight = badgeSize * 0.35f
        val top = centerY - lockHeight * 0.1f
        
        // 1. 鎖身 (Rect)
        val bodyRect = RectF(centerX - lockWidth/2, top, centerX + lockWidth/2, top + lockHeight)
        canvas.drawRoundRect(bodyRect, 4f, 4f, paint)
        
        // 2. 鎖勾 (Arc)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = badgeSize * 0.08f
        val hookRadius = lockWidth * 0.35f
        val hookRect = RectF(centerX - hookRadius, bodyRect.top - hookRadius * 1.2f, centerX + hookRadius, bodyRect.top + hookRadius * 0.8f)
        canvas.drawArc(hookRect, 180f, 180f, false, paint)
    }

    private fun drawWorkBadge(canvas: Canvas, bitmap: Bitmap, sizePx: Int, userId: Long) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return
        val userHandle = userManager.getUserForSerialNumber(userId) ?: return
        try {
            val drawable = BitmapDrawable(context.resources, bitmap)
            val badgedDrawable = context.packageManager.getUserBadgedIcon(drawable, userHandle)
            if (badgedDrawable != drawable) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                badgedDrawable.setBounds(0, 0, sizePx, sizePx)
                badgedDrawable.draw(canvas)
            }
        } catch (e: Exception) {}
    }

    private fun determineBgColor(style: IconStyle, isThemed: Boolean, m3Color: Int?, customBg: Int, customUseOrigBg: Boolean): Int? {
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
        for (i in 0..19) matrixArray[i] = 0f
        matrixArray[0] = 0f; matrixArray[4] = r
        matrixArray[6] = 0f; matrixArray[9] = g
        matrixArray[12] = 0f; matrixArray[14] = b
        matrixArray[18] = 1f
        return ColorMatrixColorFilter(matrixArray)
    }
}
