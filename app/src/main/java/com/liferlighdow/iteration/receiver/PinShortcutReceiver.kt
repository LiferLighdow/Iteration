package com.liferlighdow.iteration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build

class PinShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val request = launcherApps.getPinItemRequest(intent)
            if (request != null && request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                val shortcutInfo = request.shortcutInfo
                if (shortcutInfo != null) {
                    val label = (shortcutInfo.shortLabel ?: shortcutInfo.longLabel ?: "Shortcut").toString()
                    
                    // 發送內部廣播給 MainViewModel，並帶上全量資訊
                    val internalIntent = Intent("com.liferlighdow.iteration.ACTION_PIN_SHORTCUT").apply {
                        putExtra("package_name", shortcutInfo.`package`)
                        putExtra("shortcut_id", shortcutInfo.id)
                        putExtra("label", label)
                        // 注意：ShortcutInfo 本身不直接提供 Bitmap，
                        // 但如果是 PinItemRequest，我們可以透過系統 API 拿到圖標
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(internalIntent)
                    
                    // 確認請求 (這步很重要，否則發送端會顯示失敗)
                    request.accept()
                }
            }
        }
    }
}
