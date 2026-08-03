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
import androidx.core.graphics.drawable.toBitmap
import com.liferlighdow.iteration.ui.DynamicColorGenerator
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
        customUseDominantColor: Boolean = false,
        useMonochrome: Boolean = true,
        customHue: Float = 210f,
        customSaturation: Float = 0.5f,
        customBrightness: Float = 0.8f,
        originalIcon: Drawable? = null,
        userId: Long = 0,
        isPrivate: Boolean = false,
        calendarDay: String? = null,
        clockTime: Pair<Int, Int>? = null
    ): ImageBitmap {
        if (icon == null) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = threadPaint.get()!!

        // --- 全域著色模式 (Unified Mode / iOS Style) ---
        // 判斷條件：關閉 Monochrome 時，只要不是「標準原色」狀態 (非 STANDARD 或已開啟 M3)，就進入全域著色
        val useUnifiedMode = !useMonochrome && (style != IconStyle.STANDARD || isThemed)

        if (useUnifiedMode && calendarDay == null && clockTime == null) {
            val tintColor = when {
                style == IconStyle.CUSTOM -> {
                    Color.HSVToColor(floatArrayOf(customHue, customSaturation, customBrightness))
                }
                style == IconStyle.STANDARD && isThemed && themeColors != null -> {
                    val p = themeColors.primary
                    Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                }
                style == IconStyle.BLACK -> Color.DKGRAY
                style == IconStyle.WHITE -> Color.WHITE
                style == IconStyle.GLASS -> Color.LTGRAY
                else -> Color.HSVToColor(floatArrayOf(customHue, customSaturation, customBrightness))
            }

            val tempBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(tempBitmap)
            
            // 處理圖標繪製與縮放
            if (isIconPack) {
                val iconScale = 1.15f
                val s = (sizePx * iconScale).toInt()
                val o = (sizePx - s) / 2
                icon.setBounds(o, o, o + s, o + s)
            } else {
                icon.setBounds(0, 0, sizePx, sizePx)
            }
            icon.draw(tempCanvas)

            paint.colorFilter = createUnifiedTintFilter(tintColor)
            canvas.drawBitmap(tempBitmap, 0f, 0f, paint)
            paint.colorFilter = null
            tempBitmap.recycle()
        } else {
            // --- 原始的分層處理邏輯 ---
            val m3Colors = if (isThemed && themeColors != null) {
                val p = themeColors.primary
                val op = themeColors.onPrimary
                val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
                m3 to m3On
            } else null

            val finalCustomBg = if (style == IconStyle.CUSTOM && customUseDominantColor) {
                val colorSource = originalIcon ?: icon
                colorSource?.let { extractDominantColor(it) } ?: customBgColor
            } else customBgColor

            val bgColor = determineBgColor(style, isThemed, m3Colors?.first, finalCustomBg, customUseOriginalBg)
            val fgColor = determineFgColor(style, isThemed, m3Colors, customFgColor, customUseOriginal)

            if (calendarDay != null) {
                drawCalendarDate(canvas, sizePx, calendarDay, bgColor, fgColor)
            } else if (clockTime != null) {
                drawClockIcon(canvas, sizePx, clockTime.first, clockTime.second, bgColor, fgColor)
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

                if (filter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && useMonochrome && !(style == IconStyle.CUSTOM && customUseOriginal)) {
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

    private fun extractDominantColor(drawable: Drawable): Int? {
        return try {
            val mutated = drawable.mutate()
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mutated is AdaptiveIconDrawable) {
                mutated.background.toBitmap(64, 64)
            } else {
                mutated.toBitmap(64, 64)
            }
            DynamicColorGenerator.extractSeedColorFromBitmap(bitmap) ?: run {
                val pixels = IntArray(16)
                bitmap.getPixels(pixels, 0, 4, bitmap.width / 4, bitmap.height / 4, 4, 4)
                var r = 0; var g = 0; var b = 0
                pixels.forEach { p ->
                    if (Color.alpha(p) > 128) {
                        r += Color.red(p)
                        g += Color.green(p)
                        b += Color.blue(p)
                    }
                }
                if (r + g + b > 0) Color.rgb(r/16, g/16, b/16) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun drawCalendarDate(canvas: Canvas, sizePx: Int, day: String, bgColor: Int?, fgColor: Int?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val finalBg = bgColor ?: Color.WHITE
        val finalFg = fgColor ?: Color.BLACK
        val headerColor = if (fgColor != null) fgColor else Color.parseColor("#FF0000")
        val weekTextColor = finalBg
        paint.color = finalBg
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        val headerHeight = sizePx * 0.28f
        paint.color = headerColor
        canvas.drawRect(0f, 0f, sizePx.toFloat(), headerHeight, paint)
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE", Locale.ENGLISH)
        val weekDay = sdf.format(calendar.time).uppercase()
        paint.color = weekTextColor
        paint.textSize = headerHeight * 0.55f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        val weekX = sizePx / 2f
        val weekFontMetrics = paint.fontMetrics
        val weekY = (headerHeight - weekFontMetrics.ascent - weekFontMetrics.descent) / 2f
        canvas.drawText(weekDay, weekX, weekY, paint)
        paint.color = finalFg
        paint.textSize = sizePx * 0.42f
        paint.isFakeBoldText = true
        val dayX = sizePx / 2f
        val dayFontMetrics = paint.fontMetrics
        val remainingHeight = sizePx - headerHeight
        val dayY = headerHeight + (remainingHeight - dayFontMetrics.ascent - dayFontMetrics.descent) / 2f
        canvas.drawText(day, dayX, dayY, paint)
    }

    private fun drawClockIcon(canvas: Canvas, sizePx: Int, hour: Int, minute: Int, bgColor: Int?, fgColor: Int?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val finalBg = bgColor ?: Color.parseColor("#003153")
        val finalFg = fgColor ?: Color.WHITE
        paint.color = finalBg
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        paint.color = finalFg
        val radius = sizePx * 0.42f
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.color = finalBg
        paint.strokeWidth = sizePx * 0.015f
        for (i in 0 until 12) {
            val angle = i * 30.0
            val startR = radius * 0.82f
            val endR = radius * 0.92f
            val startX = centerX + startR * Math.sin(Math.toRadians(angle)).toFloat()
            val startY = centerY - startR * Math.cos(Math.toRadians(angle)).toFloat()
            val endX = centerX + endR * Math.sin(Math.toRadians(angle)).toFloat()
            val endY = centerY - endR * Math.cos(Math.toRadians(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = if (bgColor != null) finalBg else Color.parseColor("#FF0000")
        paint.strokeWidth = sizePx * 0.04f
        val hourAngle = (hour % 12 + minute / 60f) * 30.0
        val hourLen = radius * 0.5f
        canvas.drawLine(centerX, centerY, 
            centerX + hourLen * Math.sin(Math.toRadians(hourAngle)).toFloat(),
            centerY - hourLen * Math.cos(Math.toRadians(hourAngle)).toFloat(), paint)
        paint.color = finalBg
        paint.strokeWidth = sizePx * 0.025f
        val minAngle = minute * 6.0
        val minLen = radius * 0.75f
        canvas.drawLine(centerX, centerY,
            centerX + minLen * Math.sin(Math.toRadians(minAngle)).toFloat(),
            centerY - minLen * Math.cos(Math.toRadians(minAngle)).toFloat(), paint)
        paint.color = finalBg
        canvas.drawCircle(centerX, centerY, sizePx * 0.03f, paint)
    }

    private fun drawPrivateBadge(canvas: Canvas, sizePx: Int) {
        val badgeSize = (sizePx * 0.35f).toInt()
        val margin = (sizePx * 0.05f).toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#CC000000")
        val centerX = sizePx - badgeSize/2f - margin
        val centerY = sizePx - badgeSize/2f - margin
        canvas.drawCircle(centerX, centerY, badgeSize/2f, paint)
        paint.color = Color.WHITE
        val lockWidth = badgeSize * 0.45f
        val lockHeight = badgeSize * 0.35f
        val top = centerY - lockHeight * 0.1f
        val bodyRect = RectF(centerX - lockWidth/2, top, centerX + lockWidth/2, top + lockHeight)
        canvas.drawRoundRect(bodyRect, 4f, 4f, paint)
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

    private fun createUnifiedTintFilter(tintColor: Int): ColorFilter {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val contrast = 1.2f
        val brightness = 0.1f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness * 255,
            0f, contrast, 0f, 0f, brightness * 255,
            0f, 0f, contrast, 0f, brightness * 255,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(contrastMatrix)
        val r = Color.red(tintColor) / 255f
        val g = Color.green(tintColor) / 255f
        val b = Color.blue(tintColor) / 255f
        val tintMatrix = ColorMatrix(floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(tintMatrix)
        return ColorMatrixColorFilter(matrix)
    }
}
