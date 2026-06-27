package com.liferlighdow.iteration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.liferlighdow.iteration.R

/**
 * 接收並處理來自第三方 App (如 Hermit) 的捷徑安裝請求。
 */
class ShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.e("ShortcutReceiver", "!!!!! Received action: $action")
        
        // 彈出 Toast 讓我們知道 Receiver 真的有被觸發
        Toast.makeText(context, context.getString(R.string.shortcut_action, action), Toast.LENGTH_SHORT).show()

        when (action) {
            "com.android.launcher.action.INSTALL_SHORTCUT" -> {
                Log.e("ShortcutReceiver", "Handling legacy INSTALL_SHORTCUT")
            }
            "android.content.pm.action.CONFIRM_PIN_SHORTCUT" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    try {
                        val pinItemRequest = launcherApps.getPinItemRequest(intent)
                        if (pinItemRequest != null) {
                            Log.e("ShortcutReceiver", "Pin request valid. Type: ${pinItemRequest.requestType}")
                            if (pinItemRequest.isValid) {
                                val success = pinItemRequest.accept()
                                Log.e("ShortcutReceiver", "Accept result: $success")
                            }
                        } else {
                            Log.e("ShortcutReceiver", "PinItemRequest is null")
                        }
                    } catch (e: Exception) {
                        Log.e("ShortcutReceiver", "Error parsing PinItemRequest", e)
                    }
                }
            }
        }
    }
}
