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
        // 縮小圖片以提高效能
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
        val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
        scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        
        // 使用 QuantizerCelebi 提取主要顏色
        val result = QuantizerCelebi.quantize(pixels, 128)
        
        // 使用 Score 演算法評分並選出最適合的種子顏色
        val rankedColors = Score.score(result)
        
        return if (rankedColors.isNotEmpty()) rankedColors[0] else null
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

@Composable
fun IterationTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    isAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    
    // 優先使用 Android 12+ 的官方動態色彩
    var colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            // Android 12 以下的回退邏輯 (手動提取或預設)
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    if (darkTheme && isAmoledBlack) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF111111), // 稍微留一點層次感給卡片
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
