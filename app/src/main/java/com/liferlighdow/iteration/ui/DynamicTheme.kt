package com.liferlighdow.iteration.ui

import android.annotation.SuppressLint
import android.os.Build
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.*
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { LIGHT, DARK, FOLLOW_SYSTEM }

/**
 * 基礎動態顏色類別，封裝 MaterialColorUtilities 的顏色提取與產生邏輯
 */
object DynamicColorGenerator {
    
    @SuppressLint("RestrictedApi")
    fun extractSeedColorFromBitmap(bitmap: Bitmap): Int? {
        return try {
            // 縮小圖片以提高效能
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
            val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
            scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
            
            // 使用 QuantizerCelebi 提取主要顏色
            // 注意：某些版本的 library 回傳的是 QuantizerResult，需取其 colorToCount
            val quantizerResult = QuantizerCelebi.quantize(pixels, 128)
            
            // 判斷回傳類型並取得顏色分佈表
            val colorToCount = if (quantizerResult is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                quantizerResult as Map<Int, Int>
            } else {
                // 嘗試反射或假設它是 QuantizerResult 物件
                try {
                    val field = quantizerResult.javaClass.getDeclaredField("colorToCount")
                    field.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    field.get(quantizerResult) as Map<Int, Int>
                } catch (e: Exception) {
                    null
                }
            }

            if (colorToCount == null || colorToCount.isEmpty()) return null
            
            // 使用 Score 演算法評分並選出最適合的種子顏色
            val rankedColors = Score.score(colorToCount)
            
            if (rankedColors.isNotEmpty()) rankedColors[0] else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("RestrictedApi")
    fun generateColorSchemeFromSeed(seedColor: Int, isDark: Boolean): ColorScheme {
        val palette = CorePalette.of(seedColor)
        
        return if (isDark) {
            darkColorScheme(
                primary = Color(palette.a1.tone(80)),
                onPrimary = Color(palette.a1.tone(20)),
                primaryContainer = Color(palette.a1.tone(30)),
                onPrimaryContainer = Color(palette.a1.tone(90)),
                secondary = Color(palette.a2.tone(80)),
                onSecondary = Color(palette.a2.tone(20)),
                secondaryContainer = Color(palette.a2.tone(30)),
                onSecondaryContainer = Color(palette.a2.tone(90)),
                tertiary = Color(palette.a3.tone(80)),
                onTertiary = Color(palette.a3.tone(20)),
                tertiaryContainer = Color(palette.a3.tone(30)),
                onTertiaryContainer = Color(palette.a3.tone(90)),
                error = Color(palette.error.tone(80)),
                onError = Color(palette.error.tone(20)),
                errorContainer = Color(palette.error.tone(30)),
                onErrorContainer = Color(palette.error.tone(90)),
                background = Color(palette.n1.tone(10)),
                onBackground = Color(palette.n1.tone(90)),
                surface = Color(palette.n1.tone(10)),
                onSurface = Color(palette.n1.tone(90)),
                surfaceVariant = Color(palette.n2.tone(30)),
                onSurfaceVariant = Color(palette.n2.tone(80)),
                outline = Color(palette.n2.tone(60))
            )
        } else {
            lightColorScheme(
                primary = Color(palette.a1.tone(40)),
                onPrimary = Color(palette.a1.tone(100)),
                primaryContainer = Color(palette.a1.tone(90)),
                onPrimaryContainer = Color(palette.a1.tone(10)),
                secondary = Color(palette.a2.tone(40)),
                onSecondary = Color(palette.a2.tone(100)),
                secondaryContainer = Color(palette.a2.tone(90)),
                onSecondaryContainer = Color(palette.a2.tone(10)),
                tertiary = Color(palette.a3.tone(40)),
                onTertiary = Color(palette.a3.tone(100)),
                tertiaryContainer = Color(palette.a3.tone(90)),
                onTertiaryContainer = Color(palette.a3.tone(10)),
                error = Color(palette.error.tone(40)),
                onError = Color(palette.error.tone(100)),
                errorContainer = Color(palette.error.tone(90)),
                onErrorContainer = Color(palette.error.tone(10)),
                background = Color(palette.n1.tone(99)),
                onBackground = Color(palette.n1.tone(10)),
                surface = Color(palette.n1.tone(99)),
                onSurface = Color(palette.n1.tone(10)),
                surfaceVariant = Color(palette.n2.tone(90)),
                onSurfaceVariant = Color(palette.n2.tone(30)),
                outline = Color(palette.n2.tone(50))
            )
        }
    }
}

// 定義 Iteration 啟動器的經典藍色品牌色 (Light)
private val IterationLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E)
)

// 定義 Iteration 啟動器的經典藍色品牌色 (Dark)
private val IterationDarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF)
)

@Composable
fun IterationTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    isAmoledBlack: Boolean = false,
    isMaterialYouEnabled: Boolean = false,
    seedColor: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    
    // 使用 remember 計算配色，當開關、暗色模式或種子顏色變化時重新生成
    val colorScheme = remember(isMaterialYouEnabled, darkTheme, seedColor) {
        if (isMaterialYouEnabled) {
            if (seedColor != null) {
                // 如果有提取到種子顏色，則不論 Android 版本皆可產生動態色彩
                DynamicColorGenerator.generateColorSchemeFromSeed(seedColor, darkTheme)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 如果沒有種子顏色但 Android 版本足夠，則使用系統內建的動態色彩作為後備
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                // 低版本且無種子顏色時的回退
                if (darkTheme) IterationDarkColors else IterationLightColors
            }
        } else {
            // 如果關閉 Material You，強制使用 Iteration 經典藍色
            if (darkTheme) IterationDarkColors else IterationLightColors
        }
    }

    var finalColorScheme = colorScheme
    if (darkTheme && isAmoledBlack) {
        finalColorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF111111), // 稍微留一點層次感給卡片
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        content = content
    )
}
