package com.liferlighdow.iteration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 接收舊版捷徑安裝請求。
 * 雖然現代 Android 使用 ShortcutManager，但宣告此接收器能讓許多第三方 App (如 Hermit)
 * 認定此桌面具備接收捷徑的能力。
 */
class ShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.android.launcher.action.INSTALL_SHORTCUT") {
            Log.d("ShortcutReceiver", "Received legacy shortcut install request")
            // 現代 Android 系統會透過我們在 MainViewModel 設置的 LauncherApps.Callback 自動處理更新
        }
    }
}
