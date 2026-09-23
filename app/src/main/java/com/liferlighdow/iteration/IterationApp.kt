package com.liferlighdow.iteration

import android.app.Application
import android.content.Intent
import com.rosan.dhizuku.api.Dhizuku

class IterationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 Dhizuku
        try {
            Dhizuku.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 當 UI 隱藏 (進入背景) 或系統記憶體吃緊時，主動發送廣播清理圖片快取與桌布 Bitmap
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            val intent = Intent("com.liferlighdow.iteration.ACTION_CLEAR_CACHE_SILENT").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        }
    }
}
