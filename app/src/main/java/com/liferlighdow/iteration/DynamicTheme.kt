package com.liferlighdow.iteration

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.utilities.*

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var seedColor by remember { mutableStateOf<Int?>(null) }

    // 取得桌面背景並提取顏色
    LaunchedEffect(Unit) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            // 嘗試取得當前桌布
            val drawable = wallpaperManager.drawable
            if (drawable != null) {
                seedColor = DynamicColorGenerator.extractSeedColorFromBitmap(drawable.toBitmap())
            }
        } catch (e: SecurityException) {
            // 權限不足時的處理
        } catch (e: Exception) {
            // 其他異常
        }
    }

    val colorScheme = when {
        seedColor != null -> DynamicColorGenerator.generateColorSchemeFromSeed(seedColor!!, darkTheme)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
