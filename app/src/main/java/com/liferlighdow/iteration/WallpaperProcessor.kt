package com.liferlighdow.iteration

import android.app.Application
import android.app.WallpaperManager as AndroidWallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
     * 嘗試從系統獲取桌布 (在預設 Launcher 模式下成功率較高)
     */
    fun extractSystemWallpaper(): WallpaperResult? {
        return try {
            val wm = AndroidWallpaperManager.getInstance(context)
            // 優先嘗試獲取目前 Drawable
            var drawable = wm.drawable
            
            // 如果拿到的是 null (Android 13+ 限制)，嘗試 peek
            if (drawable == null) {
                drawable = wm.peekDrawable()
            }
            
            // 如果還是 null，嘗試獲取系統內建預設圖 (最後手段)
            if (drawable == null && android.os.Build.VERSION.SDK_INT < 33) {
                drawable = wm.getBuiltInDrawable()
            }

            if (drawable != null) {
                processBitmap(drawable.toBitmap())
            } else {
                null
            }
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
        val blurScale = 0.5f
        val bw = (screenW * blurScale).toInt().coerceAtLeast(1)
        val bh = (screenH * blurScale).toInt().coerceAtLeast(1)
        
        val small = Bitmap.createScaledBitmap(scaled, bw, bh, true)
        val blurred = Bitmap.createScaledBitmap(small, screenW, screenH, true)
        
        return WallpaperResult(
            raw = scaled.asImageBitmap(),
            blurred = blurred.asImageBitmap()
        )
    }
}
