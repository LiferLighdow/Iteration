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
import androidx.compose.ui.graphics.asAndroidBitmap
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
        maskCache.values.forEach { it.recycle() }
        maskCache.clear()
    }

    fun getOrCreateMask(cornerRadiusPercent: Float, size: Int): Bitmap {
        val key = "${cornerRadiusPercent}_$size"
        return maskCache.getOrPut(key) {
            val mask = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
            val canvas = Canvas(mask)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

            val cornerRadius = size * cornerRadiusPercent
            canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), cornerRadius, cornerRadius, paint)
            mask
        }
    }

    fun processIcon(
        icon: Drawable?,
        isThemed: Boolean,
        themeColors: ColorScheme?,
        style: IconStyle,
        cornerRadiusPercent: Float,
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
        clockTime: Pair<Int, Int>? = null,
        useLegacyUniformSquare: Boolean = false,
        customFgHue: Float = 0f,
        customFgSaturation: Float = 0f,
        customFgBrightness: Float = 1f,
        customUseDominantFgColor: Boolean = false
    ): ImageBitmap {
        if (icon == null) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = threadPaint.get()!!

        // 統一定理：所有非標準著色風格（黑色、白色、透明、M3主題圖示、自訂風格）皆以「標準 (STANDARD)」圖標為底層，轉為黑白後疊加對應色彩與半透明層（系統單色模式除外，直接著色於前後景）
        val isOverlayStyle = !useMonochrome && (style == IconStyle.BLACK || style == IconStyle.WHITE || style == IconStyle.GLASS || style == IconStyle.CUSTOM || ((style == IconStyle.STANDARD || style == IconStyle.THEMED) && isThemed)) && calendarDay == null && clockTime == null

        if (isOverlayStyle) {
            val standardBitmap = processIcon(
                icon = icon,
                isThemed = false,
                themeColors = null,
                style = IconStyle.STANDARD,
                cornerRadiusPercent = cornerRadiusPercent,
                sizePx = sizePx,
                isIconPack = isIconPack,
                customBgColor = customBgColor,
                customFgColor = customFgColor,
                customUseOriginal = customUseOriginal,
                customUseOriginalBg = customUseOriginalBg,
                customUseDominantColor = customUseDominantColor,
                useMonochrome = useMonochrome,
                customHue = customHue,
                customSaturation = customSaturation,
                customBrightness = customBrightness,
                originalIcon = originalIcon,
                userId = userId,
                isPrivate = isPrivate,
                calendarDay = null,
                clockTime = null,
                useLegacyUniformSquare = useLegacyUniformSquare
            )

            // 1. 轉為黑白（Grayscale）作為底層：玻璃風格底層透明度設為 50%，其餘不透明
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            paint.alpha = if (style == IconStyle.GLASS) 128 else 255
            canvas.drawBitmap(standardBitmap.asAndroidBitmap(), 0f, 0f, paint)
            paint.colorFilter = null
            paint.alpha = 255

            // 2. 決定對應的疊加色彩與透明度
            val overlayColor = when {
                style == IconStyle.BLACK -> Color.argb(100, 0, 0, 0) // 半透明黑
                style == IconStyle.WHITE || style == IconStyle.GLASS -> Color.argb(100, 255, 255, 255) // 半透明白
                (style == IconStyle.STANDARD || style == IconStyle.THEMED) && isThemed -> {
                    val tc = themeColors
                    if (tc != null) {
                        val p = tc.primary
                        Color.argb(150, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                    } else {
                        Color.argb(150, 0, 97, 164)
                    }
                }
                style == IconStyle.CUSTOM -> {
                    val c = Color.HSVToColor(floatArrayOf(customHue, customSaturation, customBrightness))
                    Color.argb(150, Color.red(c), Color.green(c), Color.blue(c))
                }
                else -> Color.TRANSPARENT
            }

            paint.color = overlayColor
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
            paint.xfermode = null

            if (isPrivate) {
                drawPrivateBadge(canvas, sizePx)
            } else if (userId > 0) {
                drawWorkBadge(canvas, output, sizePx, userId)
            }

            return output.asImageBitmap()
        } else {
            // --- 原始的分層處理邏輯 ---
            val m3Colors = if (isThemed && themeColors != null) {
                val p = themeColors.primary
                val op = themeColors.onPrimary
                val m3 = Color.argb(255, (p.red * 255).toInt(), (p.green * 255).toInt(), (p.blue * 255).toInt())
                val m3On = Color.argb(255, (op.red * 255).toInt(), (op.green * 255).toInt(), (op.blue * 255).toInt())
                m3 to m3On
            } else null

            // 修復點：當關閉 Monochrome 且為 CUSTOM 時，背景色應來自 HSB
            val customColorFromHsb = if (style == IconStyle.CUSTOM) {
                Color.HSVToColor(floatArrayOf(customHue, customSaturation, customBrightness))
            } else customBgColor

            val customFgColorFromHsb = if (style == IconStyle.CUSTOM) {
                Color.HSVToColor(floatArrayOf(customFgHue, customFgSaturation, customFgBrightness))
            } else customFgColor

            val finalCustomBg = if (style == IconStyle.CUSTOM && customUseDominantColor) {
                val colorSource = originalIcon ?: icon
                colorSource.let { extractDominantColor(it) } ?: customColorFromHsb
            } else customColorFromHsb

            val finalCustomFg = if (style == IconStyle.CUSTOM && customUseDominantFgColor) {
                val colorSource = originalIcon ?: icon
                colorSource.let { extractForegroundDominantColor(it) } ?: customFgColorFromHsb
            } else customFgColorFromHsb

            val bgColor = determineBgColor(style, isThemed, m3Colors?.first, finalCustomBg, customUseOriginalBg, useMonochrome, isIconPack)
            val fgColor = determineFgColor(style, isThemed, m3Colors, finalCustomFg, customUseOriginal, useMonochrome, isIconPack)

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

                if (filter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && useMonochrome) {
                    // 在單色模式下，優先使用單色層
                    if (!(style == IconStyle.CUSTOM && customUseOriginal)) {
                        icon.monochrome?.let { mono ->
                            mono.colorFilter = filter
                            mono.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                            mono.draw(canvas)
                            mono.colorFilter = null
                            drawnMonochrome = true
                        }
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
                // --- Legacy Icon Handling with Optional Uniform Square ---
                val finalBg = if (useLegacyUniformSquare && bgColor == null && !isIconPack) {
                    extractDominantColor(originalIcon ?: icon) ?: Color.LTGRAY
                } else {
                    bgColor
                }

                if (finalBg != null) {
                    paint.color = finalBg
                    paint.xfermode = null
                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
                    
                    // Scale down the icon if we added a square background
                    if (bgColor == null && !isIconPack) {
                        val iconScale = 0.72f // Leave some room for the background
                        val s = (sizePx * iconScale).toInt()
                        val o = (sizePx - s) / 2
                        icon.setBounds(o, o, o + s, o + s)
                    } else if (isIconPack) {
                        val iconScale = 1.15f
                        val s = (sizePx * iconScale).toInt()
                        val o = (sizePx - s) / 2
                        icon.setBounds(o, o, o + s, o + s)
                    } else {
                        icon.setBounds(0, 0, sizePx, sizePx)
                    }
                } else {
                    if (isIconPack) {
                        val iconScale = 1.15f
                        val s = (sizePx * iconScale).toInt()
                        val o = (sizePx - s) / 2
                        icon.setBounds(o, o, o + s, o + s)
                    } else {
                        icon.setBounds(0, 0, sizePx, sizePx)
                    }
                }
                
                if (fgColor != null) icon.setTint(fgColor)
                icon.draw(canvas)
                icon.setTintList(null)
            }
        }

        /*
        val mask = getOrCreateMask(cornerRadiusPercent, sizePx)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null
        */

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
            val seedColor = DynamicColorGenerator.extractSeedColorFromBitmap(bitmap)
            
            val finalColor = seedColor ?: run {
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
            bitmap.recycle()
            finalColor
        } catch (e: Exception) {
            null
        }
    }

    private fun drawCalendarDate(canvas: Canvas, sizePx: Int, day: String, bgColor: Int?, fgColor: Int?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val finalBg = bgColor ?: Color.WHITE
        
        // 判定是否為自定義或主題模式 (只要有指定 bgColor 即為增強/自定義模式，包含白色風格)
        val isEnhancedMode = bgColor != null
        val isDarkBg = ColorUtils.calculateLuminance(finalBg) < 0.5f
        
        val finalFg = if (isEnhancedMode) (if (isDarkBg) Color.WHITE else Color.BLACK) else (fgColor ?: Color.BLACK)
        val headerColor = if (isEnhancedMode) (if (isDarkBg) Color.WHITE else Color.BLACK) else Color.parseColor("#FF0000") // 預設模式恢復紅色
        val weekTextColor = finalBg // 星期文字與背景同色，產生鏤空感
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
        val isDarkBg = ColorUtils.calculateLuminance(finalBg) < 0.5f
        // 修正點：自定義背景時根據背景明暗反轉前景色彩 (黑色背配白字，白色背配黑字)
        val finalFg = if (bgColor != null) (if (isDarkBg) Color.WHITE else Color.BLACK) else (fgColor ?: Color.WHITE)

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

    private fun determineBgColor(style: IconStyle, isThemed: Boolean, m3Color: Int?, customBg: Int, customUseOrigBg: Boolean, useMonochrome: Boolean, isIconPack: Boolean): Int? {
        if (style == IconStyle.CUSTOM) return if (customUseOrigBg) null else customBg
        if ((isThemed || style == IconStyle.THEMED) && m3Color != null) {
            // 對於圖標包，在 STANDARD / THEMED 模式下保留原色
            if (isIconPack && (style == IconStyle.STANDARD || style == IconStyle.THEMED)) return null
            return when (style) {
                IconStyle.STANDARD, IconStyle.THEMED -> m3Color
                IconStyle.BLACK -> ColorUtils.blendARGB(Color.BLACK, m3Color, 0.3f)
                IconStyle.WHITE -> ColorUtils.blendARGB(Color.WHITE, m3Color, 0.5f)
                IconStyle.GLASS -> ColorUtils.blendARGB(Color.argb(100, 255, 255, 255), m3Color, 0.15f)
                IconStyle.CUSTOM -> null
            }
        }
        return when (style) {
            IconStyle.STANDARD -> if (useMonochrome) Color.parseColor("#0061A4") else null
            IconStyle.THEMED -> m3Color ?: if (useMonochrome) Color.parseColor("#0061A4") else null
            IconStyle.BLACK -> Color.BLACK
            IconStyle.WHITE -> Color.WHITE
            IconStyle.GLASS -> Color.argb(120, 255, 255, 255) // 半透明白
            IconStyle.CUSTOM -> customBg
        }
    }

    private fun determineFgColor(style: IconStyle, isThemed: Boolean, m3Colors: Pair<Int, Int>?, customFg: Int, customUseOrig: Boolean, useMonochrome: Boolean, isIconPack: Boolean): Int? {
        if (style == IconStyle.CUSTOM) {
            return if (customUseOrig) null else customFg
        }
        if ((isThemed || style == IconStyle.THEMED) && m3Colors != null) {
            // 對於圖標包，在 STANDARD / THEMED 模式下保留原色
            if (isIconPack && (style == IconStyle.STANDARD || style == IconStyle.THEMED)) return null
            return when (style) {
                IconStyle.STANDARD, IconStyle.THEMED -> m3Colors.second
                IconStyle.BLACK -> ColorUtils.blendARGB(Color.WHITE, m3Colors.first, 0.3f)
                IconStyle.WHITE -> Color.BLACK
                IconStyle.GLASS -> m3Colors.first
                IconStyle.CUSTOM -> null
            }
        }
        return when (style) {
            IconStyle.STANDARD -> if (useMonochrome) Color.WHITE else null
            IconStyle.THEMED -> m3Colors?.second ?: if (useMonochrome) Color.WHITE else null
            IconStyle.BLACK -> Color.WHITE
            IconStyle.WHITE -> Color.BLACK
            IconStyle.GLASS -> Color.WHITE // 不透明白前景
            IconStyle.CUSTOM -> customFg
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

    private fun extractForegroundDominantColor(drawable: Drawable): Int? {
        return try {
            val mutated = drawable.mutate()
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mutated is AdaptiveIconDrawable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mutated.monochrome?.toBitmap(64, 64) ?: mutated.foreground.toBitmap(64, 64)
                } else {
                    mutated.foreground.toBitmap(64, 64)
                }
            } else {
                mutated.toBitmap(64, 64)
            }
            val seedColor = DynamicColorGenerator.extractSeedColorFromBitmap(bitmap)
            val finalColor = seedColor ?: run {
                val pixels = IntArray(16)
                bitmap.getPixels(pixels, 0, 4, bitmap.width / 4, bitmap.height / 4, 4, 4)
                var r = 0; var g = 0; var b = 0
                var count = 0
                pixels.forEach { p ->
                    if (Color.alpha(p) > 128) {
                        r += Color.red(p)
                        g += Color.green(p)
                        b += Color.blue(p)
                        count++
                    }
                }
                if (count > 0) Color.rgb(r / count, g / count, b / count) else null
            }
            bitmap.recycle()
            finalColor
        } catch (e: Exception) {
            null
        }
    }
}
