package com.liferlighdow.iteration.viewmodel

import android.app.WallpaperManager
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream

fun MainViewModel.updateBlurredWallpaper() {
    val currentSignal = _wallpaperUpdateSignal.value
    // 如果桌布沒換，且已經有緩存，就不重複執行昂貴的模糊運算
    if (currentSignal == lastBlurredSignal && _blurredWallpaper.value != null) return

    viewModelScope.launch(Dispatchers.IO) {
        performWallpaperUpdate()
    }
}

internal fun MainViewModel.performWallpaperUpdate() {
    lastBlurredSignal = _wallpaperUpdateSignal.value
    // 1. 優先從本地儲存載入 (使用者自選)
    var result = wallpaperProcessor.loadWallpaperFromFile(wallpaperFile)

    // 2. 如果沒有自選，則嘗試從系統獲取 (降級方案)
    if (result == null) {
        result = wallpaperProcessor.extractSystemWallpaper()
    }

    if (result != null) {
        _rawWallpaper.value = result.raw
        _blurredWallpaper.value = result.blurred
        _isLightWallpaper.value = result.isLightWallpaper
    }
}

fun MainViewModel.setCustomWallpaper(bitmap: Bitmap, syncToSystem: Boolean = true) {
    _isApplyingWallpaper.value = true
    viewModelScope.launch(Dispatchers.IO) {
        try {
            if (syncToSystem) {
                val wm = WallpaperManager.getInstance(getApplication())
                wm.setBitmap(bitmap)
            }
            saveWallpaperToLocal(bitmap)
            performWallpaperUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.setBalanceWallpaper(fullBitmap: Bitmap, liteBitmap: Bitmap) {
    _isApplyingWallpaper.value = true
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val wm = WallpaperManager.getInstance(getApplication())
            // 1. 鎖定畫面套用 Full (包含 Emoji)
            wm.setBitmap(fullBitmap, null, true, WallpaperManager.FLAG_LOCK)
            // 2. 主畫面套用 Lite (1x1 純色)
            wm.setBitmap(liteBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            
            // 3. 啟動器內部快取使用 Lite (這樣背景才不會有重複的 Emoji)
            saveWallpaperToLocal(liteBitmap)
            performWallpaperUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.saveWallpaperToLocal(bitmap: Bitmap) {
    try {
        FileOutputStream(wallpaperFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        // 發出更新訊號
        _wallpaperUpdateSignal.value = System.currentTimeMillis()
    } catch (e: Exception) { e.printStackTrace() }
}
