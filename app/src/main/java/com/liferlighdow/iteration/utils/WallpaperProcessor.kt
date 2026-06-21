package com.liferlighdow.iteration.utils

import android.app.Application
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        val blurred: ImageBitmap
    )

    /**
     * 從系統獲取當前桌布
     */
    fun extractSystemWallpaper(): WallpaperResult? {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.drawable ?: return null
            processBitmap(drawable.toBitmap())
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
    fun processBitmap(rawBitmap: Bitmap): WallpaperResult {
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        // 1. 精確裁剪以符合螢幕比例
        val wallpaperAspectRatio = rawBitmap.width.toFloat() / rawBitmap.height
        val screenAspectRatio = screenW.toFloat() / screenH

        val cropW = if (wallpaperAspectRatio > screenAspectRatio) (rawBitmap.height * screenAspectRatio).toInt() else rawBitmap.width
        val cropH = if (wallpaperAspectRatio > screenAspectRatio) rawBitmap.height else (rawBitmap.width / screenAspectRatio).toInt()

        val cropped = Bitmap.createBitmap(rawBitmap, (rawBitmap.width - cropW) / 2, (rawBitmap.height - cropH) / 2, cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(cropped, screenW, screenH, true)

        // 2. 【Liquid Glass 採樣優化】
        // 優化點：不再將模糊後的圖片拉伸回全解析度。
        // 返回小尺寸位圖，由 UI 層 (Compose Image) 進行硬體加速拉伸，
        // 視覺效果幾乎一致（因為本就是模糊的），但節省了 90% 的內存佔用。
        val blurScale = 0.15f
        val bw = (screenW * blurScale).toInt().coerceAtLeast(1)
        val bh = (screenH * blurScale).toInt().coerceAtLeast(1)

        val blurred = Bitmap.createScaledBitmap(scaled, bw, bh, true)

        return WallpaperResult(
            raw = scaled.asImageBitmap(),
            blurred = blurred.asImageBitmap()
        )
    }
}