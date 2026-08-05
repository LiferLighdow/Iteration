package com.liferlighdow.iteration

import android.app.Application
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
}
