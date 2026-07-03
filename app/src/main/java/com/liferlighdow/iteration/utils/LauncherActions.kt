package com.liferlighdow.iteration.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService

fun performGestureAction(
    action: GestureAction,
    pkg: String,
    context: Context,
    actionMode: ActionMode = ActionMode.ACCESSIBILITY,
    onSettingsClick: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onOpenDesktopMenu: () -> Unit
) {
    when (action) {
        GestureAction.LOCK_SCREEN -> {
            when (actionMode) {
                ActionMode.ACCESSIBILITY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val service = IterationAccessibilityService.instance
                        if (service != null) {
                            service.lockScreen()
                        } else {
                            Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                ActionMode.ROOT -> {
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 26"))
                    } catch (e: Exception) {
                        Log.e("Iteration", "Root lock failed", e)
                        Toast.makeText(context, "Root failed", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionMode.SHIZUKU -> {
                    // 這裡通常需要 Shizuku 庫。先以 adb shell 為例，
                    // 如果用戶通過 Shizuku 啟動了某些 shell 環境，
                    // 或開發者後續集成 Shizuku 庫。目前先顯示提示。
                    Toast.makeText(context, "Shizuku mode pending integration", Toast.LENGTH_SHORT).show()
                }
            }
        }
        GestureAction.LAUNCHER_SETTINGS -> onSettingsClick()
        GestureAction.OPEN_SYSTEM_SETTINGS -> {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.failed_to_open_settings), Toast.LENGTH_SHORT).show()
            }
        }
        GestureAction.LAUNCH_APP -> {
            if (pkg.isNotEmpty()) {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }
        }
        GestureAction.OPEN_GLOBAL_SEARCH -> onOpenGlobalSearch()
        GestureAction.OPEN_DESKTOP_MENU -> onOpenDesktopMenu()
        GestureAction.OPEN_NOTIFICATIONS -> {
            when (actionMode) {
                ActionMode.ACCESSIBILITY -> {
                    val service = IterationAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                    } else {
                        Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
                    }
                }
                ActionMode.ROOT -> {
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd statusbar expand-notifications"))
                    } catch (e: Exception) {
                        Log.e("Iteration", "Root notifications failed", e)
                    }
                }
                ActionMode.SHIZUKU -> {
                    Toast.makeText(context, "Shizuku mode pending integration", Toast.LENGTH_SHORT).show()
                }
            }
        }
        GestureAction.NONE -> {}
    }
}
