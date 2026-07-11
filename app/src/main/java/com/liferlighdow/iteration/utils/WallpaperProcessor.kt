package com.liferlighdow.iteration.utils

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File

/**
 * 專門負責桌布獲取、裁剪與模糊處理的處理器
 */
class WallpaperProcessor(private val context: Application) {

    data class WallpaperResult(
        val raw: ImageBitmap,
        val blurred: ImageBitmap,
        val isLightWallpaper: Boolean = false
    )

    /**
     * 從系統獲取當前桌布
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun extractSystemWallpaper(): WallpaperResult? {
        return try {
            val wm = WallpaperManager.getInstance(context)
            var isLight = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    isLight = (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }
            }
            val drawable = wm.drawable ?: return null
            processBitmap(drawable.toBitmap(), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isLight else null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 從指定檔案路徑載入並處理桌布
     */
    fun loadWallpaperFromFile(file: File): WallpaperResult? {
        return try {
            if (!file.exists()) return null
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            processBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 核心處理邏輯：裁剪、縮放、二次採樣模糊
     */
    fun processBitmap(rawBitmap: Bitmap, systemSuggestedLight: Boolean? = null): WallpaperResult {
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        // 1. 優化：如果是 1x1 的純色圖片，直接返回
        if (rawBitmap.width == 1 && rawBitmap.height == 1) {
            val isLight = systemSuggestedLight ?: (android.graphics.Color.luminance(rawBitmap.getPixel(0, 0)) > 0.5f)
            return WallpaperResult(
                raw = rawBitmap.asImageBitmap(),
                blurred = rawBitmap.asImageBitmap(),
                isLightWallpaper = isLight
            )
        }

        // 2. 精確裁剪以符合螢幕比例
        val wallpaperAspectRatio = rawBitmap.width.toFloat() / rawBitmap.height
        val screenAspectRatio = screenW.toFloat() / screenH

        val cropW = if (wallpaperAspectRatio > screenAspectRatio) (rawBitmap.height * screenAspectRatio).toInt() else rawBitmap.width
        val cropH = if (wallpaperAspectRatio > screenAspectRatio) rawBitmap.height else (rawBitmap.width / screenAspectRatio).toInt()

        val cropped = Bitmap.createBitmap(rawBitmap, (rawBitmap.width - cropW) / 2, (rawBitmap.height - cropH) / 2, cropW, cropH)
        // 如果產生了新的裁切圖，且不是原圖，則原圖可以回收（因為在我們的使用場景中原圖是剛 decode 出來的）
        if (cropped != rawBitmap) {
            rawBitmap.recycle()
        }

        val scaled = Bitmap.createScaledBitmap(cropped, screenW, screenH, true)
        // 如果縮放後產生了新圖，回收裁切圖
        if (scaled != cropped) {
            cropped.recycle()
        }

        // 計算頂部區域亮度 (Status Bar 所在位置)
        val isLight = systemSuggestedLight ?: calculateIsLight(scaled)

        // 2. 【Liquid Glass 採樣優化】
        val blurScale = 0.15f
        val bw = (screenW * blurScale).toInt().coerceAtLeast(1)
        val bh = (screenH * blurScale).toInt().coerceAtLeast(1)

        val blurred = Bitmap.createScaledBitmap(scaled, bw, bh, true)
        // 注意：scaled 和 blurred 會轉換為 ImageBitmap 並回傳給 ViewModel 顯示，
        // 所以這兩個絕對不能在這裡 recycle。

        return WallpaperResult(
            raw = scaled.asImageBitmap(),
            blurred = blurred.asImageBitmap(),
            isLightWallpaper = isLight
        )
    }

    private fun calculateIsLight(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = (bitmap.height * 0.05f).toInt().coerceAtLeast(20) // 只取頂部 5%
        if (width <= 0 || height <= 0) return false
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var totalLuminance = 0.0
        for (pixel in pixels) {
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            // 使用 W3C 亮度公式
            totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b)
        }
        
        val avgLuminance = totalLuminance / (width * height)
        return avgLuminance > 170 // 經驗值，大於 170 認為是淺色背景
    }
}
