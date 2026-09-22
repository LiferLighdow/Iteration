package com.liferlighdow.iteration.service

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class NightlyCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // 清理圖示快取目錄
            val processedIconCacheDir = File(applicationContext.cacheDir, "processed_icons")
            if (processedIconCacheDir.exists()) {
                processedIconCacheDir.listFiles()?.forEach { it.delete() }
            }

            // 發送廣播通知 MainViewModel 清理內存快取並重整
            // 我們定義一個專門的廣播 ACTION_CLEAR_CACHE_SILENT
            val intent = Intent("com.liferlighdow.iteration.ACTION_CLEAR_CACHE_SILENT").apply {
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(intent)
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
