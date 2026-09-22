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
        // 當系統記憶體吃緊時，主動發送廣播清理啟動器的圖片快取
        // TRIM_MEMORY_BACKGROUND: App 進入後台且記憶體開始吃緊
        // TRIM_MEMORY_MODERATE / TRIM_MEMORY_COMPLETE: 記憶體極度吃緊，即將觸發 LMK
        // 使用 ComponentCallbacks2 的常數以避免直接引用過時常數
        if (level >= TRIM_MEMORY_BACKGROUND || level == TRIM_MEMORY_RUNNING_CRITICAL) {
            val intent = Intent("com.liferlighdow.iteration.ACTION_CLEAR_CACHE_SILENT").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        }
    }
}
